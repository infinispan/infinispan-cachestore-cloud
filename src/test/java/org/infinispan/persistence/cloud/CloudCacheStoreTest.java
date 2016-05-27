package org.infinispan.persistence.cloud;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.BaseStoreTest;
import org.infinispan.persistence.cloud.configuration.CloudStoreConfigurationBuilder;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "persistence.cloud.CloudCacheStoreTest")
public class CloudCacheStoreTest<K, V> extends BaseStoreTest {

   private AdvancedLoadWriteStore<K, V> buildCloudCacheStoreWithStubCloudService()
         throws PersistenceException {
      CloudStore<K, V> cs = new CloudStore<K, V>();
      ConfigurationBuilder cfgBuilder = TestCacheManagerFactory.getDefaultCacheConfiguration(false);
      cfgBuilder.persistence().addStore(CloudStoreConfigurationBuilder.class)
            .preload(true)
            .provider("transient")
            .location("test-location")
            .identity("me")
            .credential("s3cr3t")
            .container("test-container")
            .compress(true);
      cs.init(createContext(cfgBuilder.build()));
      return cs;
   }

   @Override
   protected AdvancedLoadWriteStore<K, V> createStore() throws Exception {
      AdvancedLoadWriteStore<K, V> store = buildCloudCacheStoreWithStubCloudService();
      store.start();
      return store;
   }

   @Override
   @Test(enabled = false, description = "Disabled until update to JClouds 2.0, where JCLOUDS-658 is fixed and filestore can be used for tests")
   public void testStopStartDoesNotNukeValues() throws InterruptedException, PersistenceException {
      //NO-OP
   }

   @Override
   protected boolean storePurgesAllExpired() {
      return false;
   }
}
