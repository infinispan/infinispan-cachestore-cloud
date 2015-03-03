package org.infinispan.persistence.cloud.configuration;

import org.infinispan.configuration.cache.StoreConfigurationChildBuilder;
import org.infinispan.persistence.keymappers.MarshallingTwoWayKey2StringMapper;

/**
 * CloudStoreConfigurationChildBuilder.
 *
 * @author Tristan Tarrant
 * @author Damiano Albani
 * @since 7.2
 */
public interface CloudStoreConfigurationChildBuilder<S> extends StoreConfigurationChildBuilder<S> {
   /**
    * The name of JCloud BlobStore provider.
    */
   CloudStoreConfigurationBuilder provider(String provider);
   
   /**
    * BlobStore location ID provided by provider.
    */
   CloudStoreConfigurationBuilder location(String location);

   /**
    * Login for current JClouds BlobStore.
    */
   CloudStoreConfigurationBuilder identity(String identity);

   /**
    * Credentials for current login and JClouds BlobStore.
    */
   CloudStoreConfigurationBuilder credential(String credential);

   /**
    * BlobStore container name. Actual container name will be construct $containerName_$cacheName.
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
   
   /**
    * Payload compression option - if true, payload will be compressed before passed to BlobStore.
    */
   CloudStoreConfigurationBuilder compress(boolean compress);
}
