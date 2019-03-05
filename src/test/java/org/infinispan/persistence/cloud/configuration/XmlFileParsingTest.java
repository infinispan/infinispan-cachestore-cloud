package org.infinispan.persistence.cloud.configuration;

import static org.infinispan.test.TestingUtil.withCacheManager;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.cloud.CloudStore;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.test.CacheManagerCallable;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "persistence.cloud.configuration.XmlFileParsingTest")
public class XmlFileParsingTest<K, V> extends AbstractInfinispanTest {

   @DataProvider(name = "configurationFiles")
   public Object[][] configurationFiles() {
      return new Object[][]{{"7.2.xml"}, {"8.0.xml"}, {"9.0.xml"}};
   }

   private EmbeddedCacheManager cacheManager;

   @AfterMethod
   public void cleanup() {
      TestingUtil.killCacheManagers(cacheManager);
   }

   @Test(dataProvider = "configurationFiles")
   public void testParseAndConstructUnifiedXmlFile(String config) throws IOException {
      String[] parts = config.split("\\.");
      int major = Integer.parseInt(parts[0]);
      int minor = Integer.parseInt(parts[1]);
      final int version = major * 10 + minor;
      withCacheManager(new CacheManagerCallable(
            TestCacheManagerFactory.fromXml(config, true, false, false)) {
         @Override
         public void call() {
            switch (version) {
               case 90:
                  configurationCheck90(cm);
                  break;
               case 80:
                  configurationCheck80(cm);
                  break;
               case 72:
                  configurationCheck72(cm);
                  break;
               default:
                  throw new IllegalArgumentException("Unknown configuration version " + version);
            }
         }
      });
   }

   private void configurationCheck90(EmbeddedCacheManager cm) {
      configurationCheck80(cm);
   }

   private void configurationCheck80(EmbeddedCacheManager cm) {
      configurationCheck72(cm);
   }

   private void configurationCheck72(EmbeddedCacheManager cm) {
      Cache<Object, Object> cache = cm.getCache();
      cache.put(1, "v1");
      assertEquals("v1", cache.get(1));
      @SuppressWarnings("unchecked")
      CloudStore<K, V> store = (CloudStore<K, V>) TestingUtil.getFirstLoader(cache);
      CloudStoreConfiguration configuration = store.getConfiguration();
      assertEquals(configuration.provider(), "transient");
      assertEquals(configuration.location(), "test-location");
      assertEquals(configuration.identity(), "me");
      assertEquals(configuration.credential(), "s3cr3t");
      assertEquals(configuration.container(), "test-container");
      assertEquals(configuration.endpoint(), "http://test.endpoint");
      assertTrue(configuration.compress());
      assertEquals(configuration.properties().get("key1"), "val1");
      assertEquals(configuration.properties().get("key2"), "val2");
      assertTrue(configuration.normalizeCacheNames());
   }
}