package org.infinispan.persistence.cloud.configuration;

import java.util.Properties;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.SingletonStoreConfiguration;
import org.infinispan.persistence.cloud.CloudStore;

/**
 * CloudStoreConfiguration.
 *
 * @author Damiano Albani
 * @since 7.2
 */
@BuiltBy(CloudStoreConfigurationBuilder.class)
@ConfigurationFor(CloudStore.class)
public class CloudStoreConfiguration extends AbstractStoreConfiguration {
   private final String provider;
   private final String location;
   private final String identity;
   private final String credential;
   private final String container;
   private final String key2StringMapper;

   public CloudStoreConfiguration(boolean purgeOnStartup, boolean fetchPersistentState, boolean ignoreModifications,
                                   AsyncStoreConfiguration async, SingletonStoreConfiguration singletonStore,
                                   boolean preload, boolean shared, Properties properties,
                                   String provider, String location, String identity, String credential,
                                   String container, String key2StringMapper) {
      super(purgeOnStartup, fetchPersistentState, ignoreModifications, async, singletonStore, preload, shared, properties);
      this.provider = provider;
      this.location = location;
      this.identity = identity;
      this.credential = credential;
      this.container = container;
      this.key2StringMapper = key2StringMapper;
   }

   public String provider() {
      return provider;
   }

   public String location() {
      return location;
   }
   
   public String identity() {
      return identity;
   }

   public String credential() {
      return credential;
   }

   public String container() {
      return container;
   }

   public String key2StringMapper() {
      return key2StringMapper;
   }
}
