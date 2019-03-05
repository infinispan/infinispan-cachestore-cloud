package org.infinispan.persistence.cloud.configuration;

import static org.infinispan.persistence.cloud.configuration.CloudStoreConfiguration.COMPRESS;
import static org.infinispan.persistence.cloud.configuration.CloudStoreConfiguration.CONTAINER;
import static org.infinispan.persistence.cloud.configuration.CloudStoreConfiguration.CREDENTIAL;
import static org.infinispan.persistence.cloud.configuration.CloudStoreConfiguration.ENDPOINT;
import static org.infinispan.persistence.cloud.configuration.CloudStoreConfiguration.IDENTITY;
import static org.infinispan.persistence.cloud.configuration.CloudStoreConfiguration.KEY2STRING_MAPPER;
import static org.infinispan.persistence.cloud.configuration.CloudStoreConfiguration.LOCATION;
import static org.infinispan.persistence.cloud.configuration.CloudStoreConfiguration.NORMALIZE;
import static org.infinispan.persistence.cloud.configuration.CloudStoreConfiguration.PROVIDER;

import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.persistence.cloud.CloudStore;
import org.infinispan.persistence.cloud.logging.Log;
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

   public CloudStoreConfigurationBuilder(PersistenceConfigurationBuilder builder) {
      super(builder, CloudStoreConfiguration.attributeDefinitionSet());
   }

   @Override
   public CloudStoreConfigurationBuilder self() {
      return this;
   }

   @Override
   public CloudStoreConfigurationBuilder provider(String provider) {
      this.attributes.attribute(PROVIDER).set(provider);
      return self();
   }
   
   @Override
   public CloudStoreConfigurationBuilder location(String location) {
      this.attributes.attribute(LOCATION).set(location);
      return self();
   }

   @Override
   public CloudStoreConfigurationBuilder identity(String identity) {
      this.attributes.attribute(IDENTITY).set(identity);
      return self();
   }

   @Override
   public CloudStoreConfigurationBuilder credential(String credential) {
      this.attributes.attribute(CREDENTIAL).set(credential);
      return self();
   }

   @Override
   public CloudStoreConfigurationBuilder container(String container) {
      this.attributes.attribute(CONTAINER).set(container);
      return self();
   }
   
   @Override
   public CloudStoreConfigurationBuilder endpoint(String endpoint) {
      this.attributes.attribute(ENDPOINT).set(endpoint);
      return self();
   }

   @Override
   public CloudStoreConfigurationBuilder key2StringMapper(String key2StringMapper) {
      this.attributes.attribute(KEY2STRING_MAPPER).set(key2StringMapper);
      return self();
   }

   @Override
   public CloudStoreConfigurationBuilder key2StringMapper(Class<? extends MarshallingTwoWayKey2StringMapper> klass) {
      this.attributes.attribute(KEY2STRING_MAPPER).set(klass.getName());
      return self();
   }
   
   @Override
   public CloudStoreConfigurationBuilder compress(boolean compress) {
      this.attributes.attribute(COMPRESS).set(compress);
      return self();
   }
   
   @Override
   public CloudStoreConfigurationBuilder normalizeCacheNames(boolean normalizeCacheNames) {
      this.attributes.attribute(NORMALIZE).set(normalizeCacheNames);
      return self();
   }

   @Override
   public CloudStoreConfiguration create() {
      return new CloudStoreConfiguration(attributes.protect(), async.create(), singletonStore.create());
   }

   @Override
   public CloudStoreConfigurationBuilder read(CloudStoreConfiguration template) {
      super.read(template);
      return self();
   }

   @Override
   public void validate() {
      if (attributes.attribute(PROVIDER).isNull()) {
         throw log.providerNotSpecified();
      }
      if (attributes.attribute(IDENTITY).isNull()) {
         throw log.identityNotSpecified();
      }
      if (attributes.attribute(CREDENTIAL).isNull()) {
         throw log.credentialNotSpecified();
      }
      if (attributes.attribute(CONTAINER).isNull()) {
         throw log.containerNotSpecified();
      }
   }
}
