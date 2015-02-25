package org.infinispan.persistence.cloud.configuration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "persistence.cloud.configuration.ConfigurationTest")
public class ConfigurationTest {

   public void testCacheStoreConfiguration() {
      ConfigurationBuilder b = new ConfigurationBuilder();
      b.persistence().addStore(CloudStoreConfigurationBuilder.class)
            .provider("transient")
            .location("test-location")
            .identity("me")
            .credential("s3cr3t")
            .container("test-container")
            .compress(true);
      Configuration configuration = b.build();
      CloudStoreConfiguration store = (CloudStoreConfiguration) configuration.persistence().stores().get(0);
      assertEquals(store.provider(), "transient");
      assertEquals(store.location(), "test-location");
      assertEquals(store.identity(), "me");
      assertEquals(store.credential(), "s3cr3t");
      assertEquals(store.container(), "test-container");
      assertTrue(store.compress());

      b = new ConfigurationBuilder();
      b.persistence().addStore(CloudStoreConfigurationBuilder.class).read(store);
      Configuration configuration2 = b.build();
      CloudStoreConfiguration store2 = (CloudStoreConfiguration) configuration2.persistence().stores()
            .get(0);
      assertEquals(store2.provider(), "transient");
      assertEquals(store.location(), "test-location");
      assertEquals(store2.identity(), "me");
      assertEquals(store2.credential(), "s3cr3t");
      assertEquals(store.container(), "test-container");
      assertTrue(store.compress());
   }
}