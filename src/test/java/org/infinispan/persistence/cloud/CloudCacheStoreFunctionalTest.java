package org.infinispan.persistence.cloud;

import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.persistence.BaseStoreFunctionalTest;
import org.infinispan.persistence.cloud.configuration.CloudStoreConfigurationBuilder;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "persistence.cloud.CloudCacheStoreTest")
public class CloudCacheStoreFunctionalTest extends BaseStoreFunctionalTest {

   @Override
   protected PersistenceConfigurationBuilder createCacheStoreConfig(PersistenceConfigurationBuilder persistence,
         boolean preload) {
      CloudStoreConfigurationBuilder store = persistence.addStore(CloudStoreConfigurationBuilder.class)
            .preload(preload);
      ((CloudStoreConfigurationBuilder)store)
            .provider("transient")
            .location("test-location")
            .identity("me")
            .credential("s3cr3t")
            .container("test-container")
            .compress(true);
      return persistence;
   }
   
   @Override
   @Test(enabled = false, description = "Disabled until update to JClouds 2.0, where JCLOUDS-658 is fixed and filestore can be used for tests")
   public void testPreloadAndExpiry() {
      //NO-OP 
   }
   
   @Override
   @Test(enabled = false, description = "Disabled until update to JClouds 2.0, where JCLOUDS-658 is fixed and filestore can be used for tests")
   public void testPreloadStoredAsBinary() {
      //NO-OP
   }

}
