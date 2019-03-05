package org.infinispan.persistence.cloud.configuration;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.SingletonStoreConfiguration;
import org.infinispan.configuration.serializing.SerializedWith;
import org.infinispan.persistence.cloud.CloudStore;
import org.infinispan.persistence.cloud.configuration.parser.CloudStoreConfigurationSerializer;
import org.infinispan.persistence.keymappers.WrappedByteArrayOrPrimitiveMapper;

/**
 * CloudStoreConfiguration.
 *
 * @author Damiano Albani
 * @since 7.2
 */
@BuiltBy(CloudStoreConfigurationBuilder.class)
@ConfigurationFor(CloudStore.class)
@SerializedWith(CloudStoreConfigurationSerializer.class)
public class CloudStoreConfiguration extends AbstractStoreConfiguration {
   final static AttributeDefinition<String> PROVIDER = AttributeDefinition.builder("provider", null, String.class).immutable().build();
   final static AttributeDefinition<String> LOCATION = AttributeDefinition.builder("location", null, String.class).immutable().build();
   final static AttributeDefinition<String> IDENTITY = AttributeDefinition.builder("identity", null, String.class).immutable().build();
   final static AttributeDefinition<String> CREDENTIAL = AttributeDefinition.builder("credential", null, String.class).immutable().build();
   final static AttributeDefinition<String> CONTAINER = AttributeDefinition.builder("container", null, String.class).immutable().build();
   final static AttributeDefinition<String> ENDPOINT = AttributeDefinition.builder("endpoint", null, String.class).immutable().build();
   final static AttributeDefinition<Boolean> COMPRESS = AttributeDefinition.builder("compress", false).immutable().build();
   final static AttributeDefinition<Boolean> NORMALIZE = AttributeDefinition.builder("normalize-cache-names", false).immutable().build();
   static final AttributeDefinition<String> KEY2STRING_MAPPER = AttributeDefinition.builder("key2StringMapper" , WrappedByteArrayOrPrimitiveMapper.class.getName()).immutable().xmlName("key-to-string-mapper").build();

   public static AttributeSet attributeDefinitionSet() {
      return new AttributeSet(CloudStoreConfiguration.class, AbstractStoreConfiguration.attributeDefinitionSet(), PROVIDER, LOCATION, IDENTITY, CREDENTIAL, CONTAINER, ENDPOINT, COMPRESS, NORMALIZE, KEY2STRING_MAPPER);
   }

   private final Attribute<String> provider;
   private final Attribute<String> location;
   private final Attribute<String> identity;
   private final Attribute<String> credential;
   private final Attribute<String> container;
   private final Attribute<String> endpoint;
   private final Attribute<Boolean> compress;
   private final Attribute<Boolean> normalizeCacheNames;
   private final Attribute<String> key2StringMapper;


   public CloudStoreConfiguration(AttributeSet attributeSet, AsyncStoreConfiguration async, SingletonStoreConfiguration singletonStore) {
      super(attributeSet, async, singletonStore);
      this.provider = attributeSet.attribute(PROVIDER);
      this.location = attributeSet.attribute(LOCATION);
      this.identity = attributeSet.attribute(IDENTITY);
      this.credential = attributeSet.attribute(CREDENTIAL);
      this.container = attributeSet.attribute(CONTAINER);
      this.endpoint = attributeSet.attribute(ENDPOINT);
      this.compress = attributeSet.attribute(COMPRESS);
      this.normalizeCacheNames = attributeSet.attribute(NORMALIZE);
      this.key2StringMapper = attributeSet.attribute(KEY2STRING_MAPPER);
   }

   public String provider() {
      return provider.get();
   }

   public String location() {
      return location.get();
   }

   public String identity() {
      return identity.get();
   }

   public String credential() {
      return credential.get();
   }

   public String container() {
      return container.get();
   }

   public String endpoint() {
      return endpoint.get();
   }

   public String key2StringMapper() {
      return key2StringMapper.get();
   }

   public boolean compress() {
      return compress.get();
   }

   public boolean normalizeCacheNames() {
      return normalizeCacheNames.get();
   }
}
