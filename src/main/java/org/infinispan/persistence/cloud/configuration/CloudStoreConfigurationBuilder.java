package org.infinispan.persistence.cloud.configuration;

import java.util.Properties;

import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.persistence.cloud.CloudStore;
import org.infinispan.persistence.cloud.logging.Log;
import org.infinispan.persistence.keymappers.MarshalledValueOrPrimitiveMapper;
import org.infinispan.persistence.keymappers.MarshallingTwoWayKey2StringMapper;
import org.infinispan.util.logging.LogFactory;

/**
 * CloudStoreConfigurationBuilder. Configures a {@link CloudStore}
 *
 * @author Tristan Tarrant
 * @author Damiano Albani
 * @since 7.2
 */
public class CloudStoreConfigurationBuilder extends AbstractStoreConfigurationBuilder<CloudStoreConfiguration, CloudStoreConfigurationBuilder>
implements CloudStoreConfigurationChildBuilder<CloudStoreConfigurationBuilder> {
   private static final Log log = LogFactory.getLog(CloudStoreConfigurationBuilder.class, Log.class);

   private String provider;
   private String location;
   private String identity;
   private String credential;
   private String container;
   private String endpoint;
   private String key2StringMapper = MarshalledValueOrPrimitiveMapper.class.getName();
   private boolean compress;
   private Properties overrides;

   public CloudStoreConfigurationBuilder(PersistenceConfigurationBuilder builder) {
      super(builder);
   }

   @Override
   public CloudStoreConfigurationBuilder self() {
      return this;
   }

   @Override
   public CloudStoreConfigurationBuilder provider(String provider) {
      this.provider = provider;
      return this;
   }
   
   @Override
   public CloudStoreConfigurationBuilder location(String location) {
      this.location = location;
      return this;
   }

   @Override
   public CloudStoreConfigurationBuilder identity(String identity) {
      this.identity = identity;
      return this;
   }

   @Override
   public CloudStoreConfigurationBuilder credential(String credential) {
      this.credential = credential;
      return this;
   }

   @Override
   public CloudStoreConfigurationBuilder container(String container) {
      this.container = container;
      return this;
   }
   
   @Override
   public CloudStoreConfigurationBuilder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
   }

   @Override
   public CloudStoreConfigurationBuilder key2StringMapper(String key2StringMapper) {
      this.key2StringMapper = key2StringMapper;
      return this;
   }

   @Override
   public CloudStoreConfigurationBuilder key2StringMapper(Class<? extends MarshallingTwoWayKey2StringMapper> klass) {
      this.key2StringMapper = klass.getName();
      return this;
   }
   
   @Override
   public CloudStoreConfigurationBuilder compress(boolean compress) {
      this.compress = compress;
      return this;
   }
   
   @Override
   public CloudStoreConfigurationBuilder overrides(Properties overrides) {
      this.overrides = overrides;
      return this;
   }

   @Override
   public CloudStoreConfiguration create() {
      return new CloudStoreConfiguration(purgeOnStartup, fetchPersistentState, ignoreModifications, async.create(),
                                         singletonStore.create(), preload, shared, properties,
                                         provider, location, identity, credential, container, endpoint, key2StringMapper, 
                                         compress, overrides);
   }

   @Override
   public CloudStoreConfigurationBuilder read(CloudStoreConfiguration template) {
      this.provider = template.provider();
      this.location = template.location();
      this.identity = template.identity();
      this.credential = template.credential();
      this.container = template.container();
      this.endpoint = template.endpoint();
      this.key2StringMapper = template.key2StringMapper();
      this.compress = template.compress();
      this.overrides = template.overrides();

      // AbstractStore-specific configuration
      fetchPersistentState = template.fetchPersistentState();
      ignoreModifications = template.ignoreModifications();
      properties = template.properties();
      purgeOnStartup = template.purgeOnStartup();
      shared = template.shared();
      preload = template.preload();
      async.read(template.async());
      singletonStore.read(template.singletonStore());
      return this;
   }

   @Override
   public void validate() {
      if (provider == null) {
         throw log.providerNotSpecified();
      }
      if (identity == null) {
         throw log.identityNotSpecified();
      }
      if (credential == null) {
         throw log.credentialNotSpecified();
      }
      if (container == null) {
         throw log.containerNotSpecified();
      }
   }
}
