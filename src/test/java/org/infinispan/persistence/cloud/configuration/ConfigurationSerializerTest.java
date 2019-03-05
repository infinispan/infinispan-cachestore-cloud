package org.infinispan.persistence.cloud.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.serializer.AbstractConfigurationSerializerTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(testName = "persistence.cloud.configuration.ConfigurationSerializerTest", groups="functional")
public class ConfigurationSerializerTest extends AbstractConfigurationSerializerTest {

   @DataProvider(name = "configurationFiles")
   public Object[][] configurationFiles() {
      return new Object[][] {
            {Paths.get("cloud-config.xml")},
      };
   }

   @Test(dataProvider="configurationFiles")
   public void configurationSerializationTest(Path config) throws Exception {
      super.configurationSerializationTest(config);
   }

   @Override
   protected void compareStoreConfiguration(String name, StoreConfiguration beforeStore, StoreConfiguration afterStore) {
      super.compareStoreConfiguration(name, beforeStore, afterStore);
      CloudStoreConfiguration before = (CloudStoreConfiguration) beforeStore;
      CloudStoreConfiguration after = (CloudStoreConfiguration) afterStore;
   }
}
