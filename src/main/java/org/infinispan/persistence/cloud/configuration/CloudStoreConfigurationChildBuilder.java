package org.infinispan.persistence.cloud.configuration;

import org.infinispan.configuration.cache.StoreConfigurationChildBuilder;
import org.infinispan.persistence.keymappers.MarshallingTwoWayKey2StringMapper;

/**
 * CloudStoreConfigurationChildBuilder.
 *
 * @author Tristan Tarrant
 * @author Damiano Albani
 * @since 6.0
 */
public interface CloudStoreConfigurationChildBuilder<S> extends StoreConfigurationChildBuilder<S> {
   /**
    * TODO
    */
   CloudStoreConfigurationBuilder provider(String provider);

   /**
    * TODO
    */
   CloudStoreConfigurationBuilder identity(String identity);

   /**
    * TODO
    */
   CloudStoreConfigurationBuilder credential(String credential);

   /**
    * TODO
    */
   CloudStoreConfigurationBuilder container(String container);

   /**
    * The class name of a {@link org.infinispan.persistence.keymappers.Key2StringMapper} to use for mapping keys to strings suitable for
    * RESTful retrieval/storage. Defaults to {@link org.infinispan.persistence.keymappers.MarshalledValueOrPrimitiveMapper}
    */
   CloudStoreConfigurationBuilder key2StringMapper(String key2StringMapper);

   /**
    * The class of a {@link org.infinispan.persistence.keymappers.Key2StringMapper} to use for mapping keys to strings suitable for
    * RESTful retrieval/storage. Defaults to {@link org.infinispan.persistence.keymappers.MarshalledValueOrPrimitiveMapper}
    */
   CloudStoreConfigurationBuilder key2StringMapper(Class<? extends MarshallingTwoWayKey2StringMapper> klass);
}
