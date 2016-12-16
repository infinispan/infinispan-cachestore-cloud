package org.infinispan.persistence.cloud.configuration;

import static org.infinispan.test.TestingUtil.INFINISPAN_END_TAG;
import static org.infinispan.test.TestingUtil.InfinispanStartTag;
import static org.infinispan.test.TestingUtil.withCacheManager;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.cloud.CloudStore;
import org.infinispan.test.AbstractInfinispanTest;
import org.infinispan.test.CacheManagerCallable;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "persistence.cloud.configuration.XmlFileParsingTest")
public class XmlFileParsingTest<K, V> extends AbstractInfinispanTest {

   private EmbeddedCacheManager cacheManager;

   @AfterMethod
   public void cleanup() {
      TestingUtil.killCacheManagers(cacheManager);
   }

   public void testRemoteCacheStore() throws Exception {
      String config = InfinispanStartTag.LATEST +
            "<cache-container default-cache=\"default\">" +
            "   <local-cache name=\"default\">\n" +
            "      <persistence passivation=\"false\"> \n" +
            "         <cloud-store xmlns=\"urn:infinispan:config:store:cloud:7.2\" provider=\"transient\" location=\"test-location\" identity=\"me\" credential=\"s3cr3t\" container=\"test-container\" endpoint=\"http://test.endpoint\" compress=\"true\" overrides=\"key1=val1, key2=val2\" normalize-cache-names=\"true\" />\n" +
            "      </persistence>\n" +
            "   </local-cache>\n" +
            "</cache-container>" +
            INFINISPAN_END_TAG;

      InputStream is = new ByteArrayInputStream(config.getBytes());
      withCacheManager(new CacheManagerCallable(TestCacheManagerFactory.fromStream(is)) {
         @Override
         public void call() {
            Cache<Object, Object> cache = cm.getCache();
            cache.put(1, "v1");
            assertEquals("v1", cache.get(1));
            @SuppressWarnings("unchecked")
            CloudStore<K, V> store = (CloudStore<K, V>) TestingUtil.getFirstLoader(cache);
            assertEquals(store.getConfiguration().provider(), "transient");
            assertEquals(store.getConfiguration().location(), "test-location");
            assertEquals(store.getConfiguration().identity(), "me");
            assertEquals(store.getConfiguration().credential(), "s3cr3t");
            assertEquals(store.getConfiguration().container(), "test-container");
            assertEquals(store.getConfiguration().endpoint(), "http://test.endpoint");
            assertTrue(store.getConfiguration().compress());
            assertEquals(store.getConfiguration().overrides().get("key1"), "val1");
            assertEquals(store.getConfiguration().overrides().get("key2"), "val2");
            assertTrue(store.getConfiguration().normalizeCacheNames());
         }
      });
   }
}