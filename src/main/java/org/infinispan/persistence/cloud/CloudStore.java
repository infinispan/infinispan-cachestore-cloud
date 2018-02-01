package org.infinispan.persistence.cloud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.infinispan.commons.util.Util;
import org.infinispan.executors.ExecutorAllCompletionService;
import org.infinispan.filter.KeyFilter;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.InternalMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.metadata.impl.InternalMetadataImpl;
import org.infinispan.persistence.TaskContextImpl;
import org.infinispan.persistence.cloud.configuration.CloudStoreConfiguration;
import org.infinispan.persistence.cloud.logging.Log;
import org.infinispan.persistence.keymappers.MarshallingTwoWayKey2StringMapper;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.util.logging.LogFactory;
import org.infinispan.util.stream.Streams;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.StorageType;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteSource;
import com.google.common.net.MediaType;

/**
 * The CloudStore implementation that utilizes <a
 * href="http://code.google.com/p/jclouds">JClouds</a> to communicate with cloud storage providers
 * such as <a href="http://aws.amazon.com/s3/">Amazon's S3<a>, <a
 * href="http://www.rackspacecloud.com/cloud_hosting_products/files">Rackspace's Cloudfiles</a>, or
 * any other such provider supported by JClouds.
 * <p/>
 *
 * @author Manik Surtani
 * @author Adrian Cole
 * @author Damiano Albani
 * @author Vojtech Juranek
 * @since 7.2
 */
public class CloudStore<K, V> implements AdvancedLoadWriteStore<K, V> {
   private static final Log log = LogFactory.getLog(CloudStore.class, Log.class);

   protected static final String LIFESPAN = "metadata_lifespan";
   protected static final String MAX_IDLE = "metadata_max_idle";
   protected static final String EXPIRE_TIME = "expire_time";
   protected static final int BATCH_SIZE = 1000;

   private CloudStoreConfiguration configuration;
   private InitializationContext initializationContext;

   private MarshallingTwoWayKey2StringMapper key2StringMapper;

   private BlobStoreContext blobStoreContext;
   private BlobStore blobStore;
   private String containerName;

   public CloudStoreConfiguration getConfiguration() {
      return configuration;
   }

   @Override
   public void init(InitializationContext initializationContext) {
      configuration = initializationContext.getConfiguration();
      this.initializationContext = initializationContext;
   }

   @Override
   public void start() {
      key2StringMapper = Util.getInstance(configuration.key2StringMapper(), initializationContext.getCache()
            .getAdvancedCache().getClassLoader());
      key2StringMapper.setMarshaller(initializationContext.getMarshaller());

      ContextBuilder contextBuilder = ContextBuilder.newBuilder(configuration.provider()).credentials(configuration.identity(), configuration.credential());
      if(configuration.overrides() != null)
         contextBuilder.overrides(configuration.overrides());
      if(configuration.endpoint() != null && !configuration.endpoint().isEmpty())
         contextBuilder.endpoint(configuration.endpoint());

      blobStoreContext = contextBuilder.buildView(BlobStoreContext.class);

      blobStore = blobStoreContext.getBlobStore();
      String cacheName = initializationContext.getCache().getName();
      if (configuration.normalizeCacheNames()) { 
         cacheName = cacheName.replaceAll("[^a-zA-Z0-9-]", "-"); // s3 allows [a-zA-Z0-9-.], but azure forbids periods in bucket name 
         cacheName = cacheName.toLowerCase(); // s3 bucket names can contain only lower case chars 
      }
      containerName = String.format("%s-%s", configuration.container(), cacheName);
      
      if (!blobStore.containerExists(containerName)) {
         Location location = null;
         if (configuration.location() != null ) {
            location = new LocationBuilder()
               .scope(LocationScope.REGION)
               .id(configuration.location())
               .description(String.format("Infinispan cache store for %s", containerName))
               .build();
         }
         blobStore.createContainerInLocation(location, containerName);

         //make sure container is created
         if(!blobStore.containerExists(containerName)) {
            try {
               log.waitingForContainer();
               TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
               throw new PersistenceException(String.format("Aborted when creating blob container %s", containerName));
            }
            if(!blobStore.containerExists(containerName)) {
               throw new PersistenceException(String.format("Unable to create blob container %s", containerName));
            }
         }
      }
   }

   @Override
   public void stop() {
      if (blobStoreContext != null) {
         blobStoreContext.close();
      }
   }

   private String encodeKey(Object key) {
      return key2StringMapper.getStringMapping(key);
   }

   private Object decodeKey(String key) {
      return key2StringMapper.getKeyMapping(key);
   }

   private byte[] marshall(MarshalledEntry<? extends K, ? extends V> entry) throws IOException, InterruptedException {
      return initializationContext.getMarshaller().objectToByteBuffer(entry.getValue());
   }

   private Object unmarshall(byte[] bytearray) throws IOException, ClassNotFoundException {
      return initializationContext.getMarshaller().objectFromByteBuffer(bytearray);
   }

   @Override
   public void write(MarshalledEntry<? extends K, ? extends V> entry) {
      String objectName = encodeKey(entry.getKey());
      try {
         byte[] entryBytes =  configuration.compress() ? compress(marshall(entry)) : marshall(entry);
         ByteSource payload = ByteSource.wrap(entryBytes);
         Date expiresDate = null;

         InternalMetadata metadata = entry.getMetadata();
         if (metadata != null && metadata.expiryTime() > -1) {
            expiresDate = new Date(metadata.expiryTime());
         }
         Map<String, String> ispnMetadata = new HashMap<String, String>();
         ispnMetadata.put(LIFESPAN, metadata == null ? "-1" : String.valueOf(metadata.lifespan()));
         ispnMetadata.put(MAX_IDLE, metadata == null ? "-1" : String.valueOf(metadata.maxIdle()));
         ispnMetadata.put(EXPIRE_TIME, metadata == null ? "-1" : String.valueOf(metadata.expiryTime()));

         Blob blob = blobStore.blobBuilder(objectName)
                  .payload(payload)
                  .contentLength(payload.size())
                  .contentType(MediaType.OCTET_STREAM)
                  .expires(expiresDate)
                  .userMetadata(ispnMetadata)
                  .build();

         blobStore.putBlob(containerName, blob);
      } catch (Exception e) {
         throw new PersistenceException(e);
      }
   }

   @Override
   public void clear() {
      blobStore.clearContainer(containerName);
   }

   @Override
   public boolean delete(Object key) {
      String objectName = encodeKey(key);
      if (blobStore.blobExists(containerName, objectName)) {
         blobStore.removeBlob(containerName, objectName);
         return true;
      }
      return false;
   }

   @Override
   public MarshalledEntry<K, V> load(Object key) {
      String objectName = encodeKey(key);
      Blob blob = blobStore.getBlob(containerName, objectName);

      if (blob == null) {
         return null;
      }

      BlobMetadata blobMetadata = blob.getMetadata();

      if(isExpired(blobMetadata)) {
         blobStore.removeBlob(containerName, objectName);
         return null;
      }

      final byte[] payloadByteArray;
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         Streams.copy(blob.getPayload().openStream(), bos);
         final byte[] payloadRaw = bos.toByteArray();

         HashCode expectedHashCode = blob.getMetadata().getContentMetadata().getContentMD5AsHashCode();
         // not all blobstores support md5 on GET request
         if (expectedHashCode != null){
            HashCode actualHash = HashCode.fromBytes(payloadRaw);
            if(expectedHashCode.equals(actualHash)) {
               throw new PersistenceException("MD5 hash failed when reading data from " + blob.getMetadata().getName());
            }
         }

         payloadByteArray = configuration.compress() ? uncompress(payloadRaw) : payloadRaw;
      } catch(Exception e) {
         throw new PersistenceException(e);
      }

      Map<String, String> ispnMetadata = blob.getMetadata().getUserMetadata();
      Date expiresDate = blobMetadata.getContentMetadata().getExpires();
      long ttl = -1, maxIdle = -1, now = initializationContext.getTimeService().wallClockTime();

      if (ispnMetadata != null) {
         try {
            ttl = ispnMetadata.containsKey(LIFESPAN) ? Long.parseLong(ispnMetadata.get(LIFESPAN)) : -1;
            maxIdle = ispnMetadata.containsKey(MAX_IDLE) ? Long.parseLong(ispnMetadata.get(MAX_IDLE)) : -1;
         } catch(NumberFormatException e) {
            //NO-OP default to -1 value
         }
      } else {
         if (expiresDate != null) {
            ttl = expiresDate.getTime() - now;
         }
      }

      Metadata metadata = new EmbeddedMetadata.Builder().lifespan(ttl, TimeUnit.MILLISECONDS)
            .maxIdle(maxIdle, TimeUnit.MILLISECONDS).build();
      InternalMetadata internalMetadata = new InternalMetadataImpl(metadata, now, now);

      try {
         return initializationContext.getMarshalledEntryFactory().newMarshalledEntry(key,
               unmarshall(payloadByteArray), internalMetadata);
      } catch (Exception e) {
         throw new PersistenceException(e);
      }
   }

   @Override
   public void process(KeyFilter<? super K> keyFilter, final CacheLoaderTask<K, V> cacheLoaderTask, Executor executor,
         boolean loadValue, boolean loadMetadata) {
      String nextMarker = null;
      PageSet<? extends StorageMetadata> pageSet;

      do {
         ListContainerOptions listOptions = nextMarker == null ? ListContainerOptions.NONE
               : ListContainerOptions.Builder.afterMarker(nextMarker);

         pageSet = blobStore.list(containerName, listOptions);

         ExecutorAllCompletionService eacs = new ExecutorAllCompletionService(executor);
         final TaskContext taskContext = new TaskContextImpl();

         Set<Object> entries = new HashSet<Object>(BATCH_SIZE);

         Iterator<? extends StorageMetadata> storageMetadataIterator = pageSet.iterator();
         while (storageMetadataIterator.hasNext()) {
            StorageMetadata blobMetadata = storageMetadataIterator.next();
            K key = (K) key2StringMapper.getKeyMapping(blobMetadata.getName());
            if (keyFilter == null || keyFilter.accept(key)) {
               entries.add(key);
            }
            if (entries.size() == BATCH_SIZE) {
               final Set<Object> batch = entries;
               entries = new HashSet<Object>(BATCH_SIZE);
               submitProcessTask(cacheLoaderTask, eacs, taskContext, batch, loadValue, loadMetadata);
            }
         }

         if (!entries.isEmpty()) {
            submitProcessTask(cacheLoaderTask, eacs, taskContext, entries, loadValue, loadMetadata);
         }
         eacs.waitUntilAllCompleted();
         if (eacs.isExceptionThrown()) {
            throw new PersistenceException("Process execution exception!", eacs.getFirstException());
         }

         nextMarker = pageSet.getNextMarker();
      } while (nextMarker != null);
   }

   private void submitProcessTask(final CacheLoaderTask<K, V> cacheLoaderTask, CompletionService<Void> ecs,
         final TaskContext taskContext, final Set<Object> batch, final boolean loadEntry, final boolean loadMetadata) {
      ecs.submit(new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            try {
               for (Object key : batch) {
                  if (taskContext.isStopped())
                     break;
                  if (!loadEntry && !loadMetadata) {
                     cacheLoaderTask.processEntry(
                           initializationContext.getMarshalledEntryFactory().newMarshalledEntry(key, (Object) null,
                                 null), taskContext);
                  } else {
                     cacheLoaderTask.processEntry(load(key), taskContext);
                  }
               }
            } catch (Exception e) {
               log.errorExecutingParallelStoreTask(e);
               throw e;
            }
            return null;
         }
      });
   }

   @Override
   public void purge(Executor executor, PurgeListener<? super K> purgeListener) {
      String nextMarker = null;
      PageSet<? extends StorageMetadata> pageSet;

      do {
         ListContainerOptions listOptions = nextMarker == null ? ListContainerOptions.NONE
               : ListContainerOptions.Builder.afterMarker(nextMarker);
         pageSet = blobStore.list(containerName, listOptions);

         ExecutorAllCompletionService eacs = new ExecutorAllCompletionService(executor);
         Set<String> entries = new HashSet<String>(BATCH_SIZE);

         Iterator<? extends StorageMetadata> storageMetadataIterator = pageSet.iterator();
         while (storageMetadataIterator.hasNext()) {
            StorageMetadata storageMetadata = storageMetadataIterator.next();
            if(storageMetadata.getType().equals(StorageType.BLOB)) {
                 Blob blob = blobStore.getBlob(containerName, storageMetadata.getName());
                 BlobMetadata blobMetadata = blob.getMetadata();

                 if (isExpired(blobMetadata)) {
                     entries.add(storageMetadata.getName());
                     if (entries.size() == BATCH_SIZE) {
                         final Set<String> batch = entries;
                         entries = new HashSet<String>(BATCH_SIZE);
                         submitPurgeTask(eacs, batch, purgeListener);
                     }
                 }
            }
         }

         if (!entries.isEmpty()) {
            submitPurgeTask(eacs, entries, purgeListener);
         }
         eacs.waitUntilAllCompleted();
         if (eacs.isExceptionThrown()) {
            throw new PersistenceException("Purge execution exception!", eacs.getFirstException());
         }

         nextMarker = pageSet.getNextMarker();
      } while (nextMarker != null);

   }

   private void submitPurgeTask(CompletionService<Void> ecs, final Set<String> batch, final PurgeListener<? super K> purgeListener) {
      ecs.submit(new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            try {
               for (String key : batch) {
                  blobStore.removeBlob(containerName, key);
                  purgeListener.entryPurged((K)key2StringMapper.getKeyMapping(key));
               }
            } catch (Exception e) {
               log.errorExecutingParallelStoreTask(e);
               throw e;
            }
            return null;
         }
      });
   }

   @Override
   public int size() {
      return (int) blobStore.countBlobs(containerName);
   }

   @Override
   public boolean contains(Object key) {
      String objectName = encodeKey(key);
      Blob blob = blobStore.getBlob(containerName, objectName);

      if (blob == null) {
         return false;
      }

      BlobMetadata blobMetadata = blob.getMetadata();

      if(isExpired(blobMetadata)) {
         blobStore.removeBlob(containerName, objectName);
         return false;
      } else {
         return true;
      }
   }

   public String getContainerName() {
      return containerName;
   }
   
   public void removeContainer() {
      blobStore.clearContainer(containerName);
      blobStore.deleteContainer(containerName);
   }
   
   /*package*/ BlobStore getBlobStore() {
      return blobStore;
   }
   
   protected boolean isExpired(BlobMetadata blobMetadata) {
      long now = initializationContext.getTimeService().wallClockTime();
      Map<String, String> ispnMetadata = blobMetadata.getUserMetadata();
      long et = -1;

      if (ispnMetadata != null && ispnMetadata.containsKey(EXPIRE_TIME)) {
         try {
            et = ispnMetadata.containsKey(EXPIRE_TIME) ? Long.parseLong(ispnMetadata.get(EXPIRE_TIME)) : -1;
         } catch(NumberFormatException e) {
            // fall back to blob store expires time
            if (blobMetadata.getContentMetadata().getExpires() != null)
               et = blobMetadata.getContentMetadata().getExpires().getTime();
         }
      } else {
         if (blobMetadata.getContentMetadata().getExpires() != null)
            et = blobMetadata.getContentMetadata().getExpires().getTime();
      }

      return (et > -1 && et < now);
   }

   private byte[] uncompress(byte[] compressedByteArray) throws IOException, PersistenceException {
      ByteArrayInputStream bis = new ByteArrayInputStream(compressedByteArray);

      GZIPInputStream is = new GZIPInputStream(bis);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      Streams.copy(is, bos);
      final byte[] uncompressedByteArray = bos.toByteArray();

      is.close();
      bis.close();
      bos.close();
      return uncompressedByteArray;
   }

   private byte[] compress(final byte[] uncompressedByteArray) throws IOException {
      InputStream input = new ByteArrayInputStream(uncompressedByteArray);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream output = new GZIPOutputStream(baos);

      Streams.copy(input, output);
      output.close();
      input.close();

      final byte[] compressedByteArray = baos.toByteArray();

      baos.close();

      return compressedByteArray;
   }

}
