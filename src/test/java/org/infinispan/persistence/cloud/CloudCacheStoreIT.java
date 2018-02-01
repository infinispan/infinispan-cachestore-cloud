package org.infinispan.persistence.cloud;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.marshall.core.MarshalledValue;
import org.infinispan.persistence.BaseStoreTest;
import org.infinispan.persistence.cloud.configuration.CloudStoreConfigurationBuilder;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.net.MediaType;

@Test(groups = "unit", testName = "persistence.cloud.CloudCacheStoreTest")
public class CloudCacheStoreIT<K, V> extends BaseStoreTest {
   
   private static final String TRANSIENT_PROVIDER = "transient";
   
   private String csBucket;
   private String accessKey;
   private String secretKey;
   private String cs;
   private String csEndpoint;
   private String csLocation;

   private static final String sysUsername = System.getProperty("infinispan.test.jclouds.username");
   private static final String sysPassword = System.getProperty("infinispan.test.jclouds.password");
   private static final String sysService = System.getProperty("infinispan.test.jclouds.service");
   private static final String sysEndpoint = System.getProperty("infinispan.test.jclouds.endpoint");
   private static final String sysLocation = System.getProperty("infinispan.test.jclouds.location");

   @BeforeTest
   @Parameters({"infinispan.test.jclouds.username", "infinispan.test.jclouds.password", "infinispan.test.jclouds.service", "infinispan.test.jclouds.endpoint", "infinispan.test.jclouds.location"})
   protected void setUpClient(@Optional String JcloudsUsername,
                              @Optional String JcloudsPassword,
                              @Optional String JcloudsService,
                              @Optional String JcloudsEndpoint,
                              @Optional String JcloudsLocation) throws Exception {

      accessKey = (JcloudsUsername == null) ? sysUsername : JcloudsUsername;
      secretKey = (JcloudsPassword == null) ? sysPassword : JcloudsPassword;
      cs = (JcloudsService == null) ? sysService : JcloudsService;
      csEndpoint = (JcloudsEndpoint == null) ? sysEndpoint : JcloudsEndpoint;
      csLocation = (JcloudsService == null) ? sysLocation : JcloudsLocation;

      if (accessKey == null || accessKey.trim().length() == 0 || secretKey == null || secretKey.trim().length() == 0) {
         accessKey = "dummy";
         secretKey = "dummy";
      }
      csBucket = (System.getProperty("user.name") + "." + this.getClass().getSimpleName()).toLowerCase().replace('.', '-'); // azure limitation on no periods
      csBucket = csBucket.length() > 20 ? csBucket.substring(0, 20): csBucket;//limitation on length
      System.out.printf("accessKey: %1$s, bucket: %2$s%n", accessKey, csBucket);
   }

   private AdvancedLoadWriteStore<K, V> buildCloudCacheStoreWithStubCloudService()
         throws PersistenceException {
      CloudStore<K, V> cloudStore = new CloudStore<K, V>();
      ConfigurationBuilder cfgBuilder = TestCacheManagerFactory.getDefaultCacheConfiguration(false);
      cfgBuilder.persistence().addStore(CloudStoreConfigurationBuilder.class)
         .preload(true)
         .provider(cs)
         .endpoint(csEndpoint)
         .location(csLocation)
         .identity(accessKey)
         .credential(secretKey)
         .container(csBucket)
         .compress(true)
         .normalizeCacheNames(true);
      cloudStore.init(createContext(cfgBuilder.build()));
      return cloudStore;
   }
   
   @Override
   protected AdvancedLoadWriteStore<K, V> createStore() throws Exception {
      AdvancedLoadWriteStore<K, V> store = buildCloudCacheStoreWithStubCloudService();
      store.start();
      return store;
   }
   
   @AfterMethod(alwaysRun = true)
   @Override
   public void tearDown() throws PersistenceException {
      if (cl != null) {
         cl.clear();
         cl.stop();

      }
   }

   @Override
   protected boolean storePurgesAllExpired() {
      return false;
   }
   
   @Override
   @Test
   public void testStopStartDoesNotNukeValues() throws InterruptedException, PersistenceException {
      if (TRANSIENT_PROVIDER.equals(cs)) {
         return; //Disabled until update to JClouds 2.0, where JCLOUDS-658 is fixed and filestore can be used for tests
      }
      super.testStopStartDoesNotNukeValues();
   }
   
   @Test
   public void testJCloudsMetadataTest() throws IOException {
      String blobName = "myBlob";
      String containerName = (csBucket + "MetadataTest").toLowerCase();
      BlobStore blobStore = ((CloudStore) cl).getBlobStore();
         
      if (!blobStore.containerExists(containerName)) {
         Location location = new LocationBuilder().scope(LocationScope.REGION).description("test").id(csLocation).build();
         blobStore.createContainerInLocation(location, containerName);
         TestingUtil.sleepThread(10000);
      }

      String payload = "Hello world";
      Blob blob = blobStore.blobBuilder(blobName)
            .payload(payload)
            .contentLength(payload.length())
            .contentType(MediaType.OCTET_STREAM)
            .userMetadata(Collections.singletonMap("hello", "world"))
            .build();
      blobStore.putBlob(containerName, blob);

      blob = blobStore.getBlob(containerName, blobName);
      assertEquals(blob.getMetadata().getUserMetadata().get("hello"), "world");

      PageSet<? extends StorageMetadata> ps = blobStore.list(containerName, ListContainerOptions.Builder.withDetails());
      for (StorageMetadata sm : ps) {
         assertEquals(sm.getUserMetadata().get("hello"), "world");
      }
      
      blobStore.deleteContainer(containerName);
   }
   
   @Test
   public void testNegativeHashCodes() throws PersistenceException {
      ObjectWithNegativeHashcode nho = new ObjectWithNegativeHashcode();
      MarshalledValue mvKey = new MarshalledValue(nho, getMarshaller());
      cl.write(marshalledEntry(mvKey, "hello", null));
      MarshalledEntry<Object, Object> ice = cl.load(mvKey);
      assertEquals(((MarshalledValue)ice.getKey()).get(), nho);
      assertEquals(ice.getValue(), "hello");
   }

   private static class ObjectWithNegativeHashcode implements Serializable {
      private static final long serialVersionUID = 1L;
      String s = "hello";

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         ObjectWithNegativeHashcode blah = (ObjectWithNegativeHashcode) o;
         return !(s != null ? !s.equals(blah.s) : blah.s != null);
      }

      @Override
      public int hashCode() {
         return -700;
      }
   }

}
