package org.infinispan.persistence.cloud;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.infinispan.commons.util.Util;
import org.infinispan.executors.ExecutorAllCompletionService;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.InternalMetadata;
import org.infinispan.metadata.InternalMetadataImpl;
import org.infinispan.metadata.Metadata;
import org.infinispan.persistence.TaskContextImpl;
import org.infinispan.persistence.cloud.configuration.CloudStoreConfiguration;
import org.infinispan.persistence.cloud.logging.Log;
import org.infinispan.persistence.keymappers.MarshallingTwoWayKey2StringMapper;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.util.logging.LogFactory;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;

import com.google.common.io.ByteSource;
import com.google.common.net.MediaType;

/**
 * The CloudStore implementation that utilizes <a href="http://code.google.com/p/jclouds">JClouds</a> to
 * communicate with cloud storage providers such as <a href="http://aws.amazon.com/s3/">Amazon's S3<a>, <a
 * href="http://www.rackspacecloud.com/cloud_hosting_products/files">Rackspace's Cloudfiles</a>, or any other such
 * provider supported by JClouds.
 * <p/>
 *
 * @author Manik Surtani
 * @author Adrian Cole
 * @author Damiano Albani
 * @since 4.0
 */
public class CloudStore implements AdvancedLoadWriteStore {
   private static final Log log = LogFactory.getLog(CloudStore.class, Log.class);

   private CloudStoreConfiguration configuration;
   private InitializationContext initializationContext;

   private MarshallingTwoWayKey2StringMapper key2StringMapper;

   private BlobStoreContext blobStoreContext;
   private BlobStore blobStore;

   @Override
   public void init(InitializationContext initializationContext) {
      configuration = initializationContext.getConfiguration();
      this.initializationContext = initializationContext;
   }

   @Override
   public void start() {
      key2StringMapper = Util.getInstance(configuration.key2StringMapper(),
                                          initializationContext.getCache().getAdvancedCache().getClassLoader());
      key2StringMapper.setMarshaller(initializationContext.getMarshaller());

      blobStoreContext = ContextBuilder.newBuilder(configuration.provider())
                                       .credentials(configuration.identity(), configuration.credential())
                                       .buildView(BlobStoreContext.class);

      blobStore = blobStoreContext.getBlobStore();

      String container = configuration.container();

      if (!blobStore.containerExists(container)) {
         // TODO assert(created)
      }
   }

   @Override
   public void stop() {
      blobStoreContext.close();
   }

   private String encodeKey(Object key) {
      return key2StringMapper.getStringMapping(key);
   }

   private Object decodeKey(String key) {
      return key2StringMapper.getKeyMapping(key);
   }

   private byte[] marshall(MarshalledEntry entry) throws IOException, InterruptedException {
      return initializationContext.getMarshaller().objectToByteBuffer(entry.getValue());
   }

   private Object unmarshall(InputStream stream) throws IOException, ClassNotFoundException {
      return initializationContext.getMarshaller().objectFromInputStream(stream);
   }

   @Override
   public void write(MarshalledEntry entry) {
      String objectName = encodeKey(entry.getKey());
      try {
         ByteSource payload = ByteSource.wrap(marshall(entry));

         Date expiresDate = null;

         InternalMetadata metadata = entry.getMetadata();
         if (metadata != null && metadata.expiryTime() > -1) {
            expiresDate = new Date(metadata.expiryTime());
         }

         Blob blob = blobStore.blobBuilder(objectName)
                              .payload(payload)
                              .contentLength(payload.size())
                              .contentType(MediaType.OCTET_STREAM.toString())
                              .expires(expiresDate)
                              .build();

         blobStore.putBlob(configuration.container(), blob);
      } catch (Exception e) {
         throw new PersistenceException(e);
      }
   }

   @Override
   public void clear() {
      blobStore.clearContainer(configuration.container());
   }

   @Override
   public boolean delete(Object key) {
      String objectName = encodeKey(key);

      blobStore.removeBlob(configuration.container(), objectName);

      return true;
   }

   @Override
   public MarshalledEntry load(Object key) {
      String objectName = encodeKey(key);

      Blob blob = blobStore.getBlob(configuration.container(), objectName);

      if (blob == null) {
         return null;
      }

      BlobMetadata blobMetadata = blob.getMetadata();

      Date expiresDate = blobMetadata.getContentMetadata().getExpires();

      long ttl = -1,
           maxIdle = -1,
           now = initializationContext.getTimeService().wallClockTime();

      if (expiresDate != null) {
         ttl = expiresDate.getTime() - now;
      }

      Metadata metadata = new EmbeddedMetadata.Builder()
                                              .lifespan(ttl, TimeUnit.MILLISECONDS)
                                              .maxIdle(maxIdle, TimeUnit.MILLISECONDS)
                                              .build();
      InternalMetadata internalMetadata;
      if (metadata.lifespan() > -1) {
         internalMetadata = new InternalMetadataImpl(metadata, now, now);
      } else {
         internalMetadata = new InternalMetadataImpl(metadata, -1, -1);
      }

      try {
         return initializationContext.getMarshalledEntryFactory()
                                     .newMarshalledEntry(key,
                                                         unmarshall(blob.getPayload().openStream()), internalMetadata);
      } catch (Exception e) {
         throw new PersistenceException(e);
      }
   }

   @Override
   public void process(KeyFilter keyFilter, final CacheLoaderTask cacheLoaderTask, Executor executor, boolean loadValue, boolean loadMetadata) {
      String nextMarker = null;
      PageSet<? extends StorageMetadata> pageSet;

      do {
         ListContainerOptions listOptions = ListContainerOptions.Builder.afterMarker(nextMarker);

         pageSet = blobStore.list(configuration.container(), listOptions);

         ExecutorAllCompletionService eacs = new ExecutorAllCompletionService(executor);
         final TaskContext taskContext = new TaskContextImpl();

         int batchSize = 1000;
         Set<Object> entries = new HashSet<Object>(batchSize);

         Iterator<? extends StorageMetadata> storageMetadataIterator = pageSet.iterator();
         while (storageMetadataIterator.hasNext()) {
            StorageMetadata blobMetadata = storageMetadataIterator.next();
            Object key = key2StringMapper.getKeyMapping(blobMetadata.getName());
            if (keyFilter == null || keyFilter.shouldLoadKey(key))
               entries.add(key);
            if (entries.size() == batchSize) {
               final Set<Object> batch = entries;
               entries = new HashSet<Object>(batchSize);
               submitProcessTask(cacheLoaderTask, eacs, taskContext, batch, loadValue, loadMetadata);
            }
         }

         if (!entries.isEmpty()) {
            submitProcessTask(cacheLoaderTask, eacs, taskContext, entries, loadValue, loadMetadata);
         }
         eacs.waitUntilAllCompleted();
         if (eacs.isExceptionThrown()) {
            throw new PersistenceException("Execution exception!", eacs.getFirstException());
         }

         nextMarker = pageSet.getNextMarker();
      } while (nextMarker != null);
   }

   private void submitProcessTask(final CacheLoaderTask cacheLoaderTask, CompletionService ecs,
                                  final TaskContext taskContext, final Set<Object> batch, final boolean loadEntry,
                                  final boolean loadMetadata) {
      ecs.submit(new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            try {
               for (Object key : batch) {
                  if (taskContext.isStopped())
                     break;
                  if (!loadEntry && !loadMetadata) {
                     cacheLoaderTask.processEntry(initializationContext.getMarshalledEntryFactory().newMarshalledEntry(key, (Object) null, null), taskContext);
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
   public void purge(Executor executor, PurgeListener purgeListener) {
      // This should be handled by the remote server
   }

   @Override
   public int size() {
      return (int) blobStore.countBlobs(configuration.container());
   }

   @Override
   public boolean contains(Object key) {
      String objectName = encodeKey(key);

      return blobStore.blobExists(configuration.container(), objectName);
   }
}
