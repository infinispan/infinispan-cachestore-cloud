package org.infinispan.persistence.cloud.configuration;

import org.infinispan.configuration.cache.AbstractStoreConfigurationChildBuilder;
import org.infinispan.persistence.keymappers.MarshallingTwoWayKey2StringMapper;

/**
 * AbstractCloudStoreConfigurationChildBuilder.
 *
 * @author Damiano Albani
 * @since 7.2
 */
public abstract class AbstractCloudStoreConfigurationChildBuilder<S> extends AbstractStoreConfigurationChildBuilder<S> implements CloudStoreConfigurationChildBuilder<S> {
   private final CloudStoreConfigurationBuilder builder;

   protected AbstractCloudStoreConfigurationChildBuilder(CloudStoreConfigurationBuilder builder) {
      super(builder);
      this.builder = builder;
   }

   @Override
   public CloudStoreConfigurationBuilder provider(String provider) {
      return builder.provider(provider);
   }
   
   @Override
   public CloudStoreConfigurationBuilder location(String location) {
      return builder.location(location);
   }

   @Override
   public CloudStoreConfigurationBuilder identity(String identity) {
      return builder.identity(identity);
   }

   @Override
   public CloudStoreConfigurationBuilder credential(String credential) {
      return builder.credential(credential);
   }

   @Override
   public CloudStoreConfigurationBuilder container(String container) {
      return builder.location(container);
   }

   @Override
   public CloudStoreConfigurationBuilder key2StringMapper(String key2StringMapper) {
      return builder.key2StringMapper(key2StringMapper);
   }

   @Override
   public CloudStoreConfigurationBuilder key2StringMapper(Class<? extends MarshallingTwoWayKey2StringMapper> klass) {
      return builder.key2StringMapper(klass);
   }
}
