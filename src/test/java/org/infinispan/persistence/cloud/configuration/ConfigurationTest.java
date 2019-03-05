package org.infinispan.persistence.cloud.configuration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Properties;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "persistence.cloud.configuration.ConfigurationTest")
public class ConfigurationTest {

   public void testCacheStoreConfiguration() {
      Properties props = new Properties();
      props.put("key1", "val1");
      props.put("key2", "val2");
      ConfigurationBuilder b = new ConfigurationBuilder();
      b.persistence().addStore(CloudStoreConfigurationBuilder.class)
            .provider("transient")
            .location("test-location")
            .identity("me")
            .credential("s3cr3t")
            .container("test-container")
            .endpoint("http://test.endpoint")
            .compress(true)
            .properties(props)
            .normalizeCacheNames(true);
      
            
      Configuration configuration = b.build();
      CloudStoreConfiguration store = (CloudStoreConfiguration) configuration.persistence().stores().get(0);
      assertEquals(store.provider(), "transient");
      assertEquals(store.location(), "test-location");
      assertEquals(store.identity(), "me");
      assertEquals(store.credential(), "s3cr3t");
      assertEquals(store.container(), "test-container");
      assertEquals(store.endpoint(), "http://test.endpoint");
      assertTrue(store.compress());
      assertEquals(store.properties().get("key1"), "val1");
      assertEquals(store.properties().get("key2"), "val2");
      assertTrue(store.normalizeCacheNames());

      b = new ConfigurationBuilder();
      b.persistence().addStore(CloudStoreConfigurationBuilder.class).read(store);
      Configuration configuration2 = b.build();
      CloudStoreConfiguration store2 = (CloudStoreConfiguration) configuration2.persistence().stores()
            .get(0);
      assertEquals(store2.provider(), "transient");
      assertEquals(store2.location(), "test-location");
      assertEquals(store2.identity(), "me");
      assertEquals(store2.credential(), "s3cr3t");
      assertEquals(store2.container(), "test-container");
      assertEquals(store2.endpoint(), "http://test.endpoint");
      assertTrue(store2.compress());
      assertEquals(store2.properties().get("key1"), "val1");
      assertEquals(store2.properties().get("key2"), "val2");
      assertTrue(store2.normalizeCacheNames());
      assertTrue(store2.normalizeCacheNames());
   }
}