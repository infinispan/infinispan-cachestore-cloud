package org.infinispan.persistence.cloud;

import org.infinispan.Cache;
import org.infinispan.commons.io.ByteBufferFactoryImpl;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.marshall.core.MarshalledEntryFactoryImpl;
import org.infinispan.persistence.BaseStoreFunctionalTest;
import org.infinispan.persistence.InitializationContextImpl;
import org.infinispan.persistence.cloud.configuration.CloudStoreConfigurationBuilder;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = "unit", testName = "persistence.cloud.CloudCacheStoreTest")
public class CloudCacheStoreFunctionalIT extends BaseStoreFunctionalTest {

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
   }

   @AfterMethod(alwaysRun = true)
   private void nukeBuckets() throws Exception {
      for (String name : cacheManager.getCacheNames()) {
         CloudStore ccs = new CloudStore();
         Cache cache = cacheManager.getCache(name);
         ccs.init(new InitializationContextImpl(
                     cache.getAdvancedCache().getCacheConfiguration().persistence().stores().get(0), 
                     cache, 
                     TestingUtil.extractGlobalMarshaller(cacheManager),
                     cache.getAdvancedCache().getComponentRegistry().getTimeService(),
                     new ByteBufferFactoryImpl(),
                     new MarshalledEntryFactoryImpl()
                     ));
         ccs.start();
         ccs.removeContainer();
         ccs.stop();
      }
   }


   @Override
   protected PersistenceConfigurationBuilder createCacheStoreConfig(PersistenceConfigurationBuilder persistence,
         boolean preload) {
      CloudStoreConfigurationBuilder cfg = persistence.addStore(CloudStoreConfigurationBuilder.class);
      cfg.preload(preload)
         .provider(cs)
         .endpoint(csEndpoint)
         .location(csLocation)
         .identity(accessKey)
         .credential(secretKey)
         .container(csBucket)
         .compress(true);
      return persistence;
   }
   
   @Override
   @Test
   public void testPreloadAndExpiry() {
      if (TRANSIENT_PROVIDER.equals(cs)) {
         return; //Disabled until update to JClouds 2.0, where JCLOUDS-658 is fixed and filestore can be used for tests
      }
      super.testPreloadAndExpiry();
   }
   
   @Override
   @Test
   public void testPreloadStoredAsBinary() {
      if (TRANSIENT_PROVIDER.equals(cs)) {
         return; //Disabled until update to JClouds 2.0, where JCLOUDS-658 is fixed and filestore can be used for tests
      }
      super.testPreloadStoredAsBinary();
   }
}
