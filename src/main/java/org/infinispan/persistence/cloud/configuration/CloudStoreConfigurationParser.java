package org.infinispan.persistence.cloud.configuration;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ConfigurationParser;
import org.infinispan.configuration.parsing.Namespace;
import org.infinispan.configuration.parsing.Namespaces;
import org.infinispan.configuration.parsing.ParseUtils;
import org.infinispan.configuration.parsing.Parser;
import org.infinispan.configuration.parsing.XMLExtendedStreamReader;
import org.kohsuke.MetaInfServices;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Properties;

import static org.infinispan.commons.util.StringPropertyReplacer.replaceProperties;


/**
*
* CloudStoreConfigurationParser
*
* @since 7.2
*/
@MetaInfServices
@Namespaces({
      @Namespace(uri = "urn:infinispan:config:store:cloud:8.0",
            root = CloudStoreConfigurationParser.ROOT_ELEMENT),
      @Namespace(uri = "urn:infinispan:config:store:cloud:7.2",
                 root = CloudStoreConfigurationParser.ROOT_ELEMENT),
      @Namespace(root = CloudStoreConfigurationParser.ROOT_ELEMENT)
})
public class CloudStoreConfigurationParser implements ConfigurationParser {

   public static final String ROOT_ELEMENT = "cloud-store";
   public static final String OVERRIDES_SEPARATOR = ",";
   public static final String PROPERTY_SEPARATOR = "=";

   public CloudStoreConfigurationParser() {
   }

   @Override
   public void readElement(final XMLExtendedStreamReader reader, final ConfigurationBuilderHolder holder)
         throws XMLStreamException {
      ConfigurationBuilder builder = holder.getCurrentConfigurationBuilder();

      Element element = Element.forName(reader.getLocalName());
      switch (element) {
      case CLOUD_STORE: {
         parseCloudStore(reader, builder.persistence(), holder.getClassLoader());
         break;
      }
      default: {
         throw ParseUtils.unexpectedElement(reader);
      }
      }
   }

   private void parseCloudStore(final XMLExtendedStreamReader reader, PersistenceConfigurationBuilder loadersBuilder,
         ClassLoader classLoader) throws XMLStreamException {
      CloudStoreConfigurationBuilder builder = new CloudStoreConfigurationBuilder(loadersBuilder);
      parseCloudStoreAttributes(reader, builder, classLoader);

      while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
         Element element = Element.forName(reader.getLocalName());
         switch (element) {
         default: {
            Parser.parseStoreElement(reader, builder);
            break;
         }
         }
      }
      loadersBuilder.addStore(builder);
   }

   private void parseCloudStoreAttributes(XMLExtendedStreamReader reader, CloudStoreConfigurationBuilder builder, ClassLoader classLoader)
         throws XMLStreamException {
      for (int i = 0; i < reader.getAttributeCount(); i++) {
         ParseUtils.requireNoNamespaceAttribute(reader, i);
         String value = replaceProperties(reader.getAttributeValue(i));
         String attributeName = reader.getAttributeLocalName(i);
         Attribute attribute = Attribute.forName(attributeName);
         switch (attribute) {
         case PROVIDER: {
            builder.provider(value);
            break;
         }
         case LOCATION: {
            builder.location(value);
            break;
         }
         case IDENTITY: {
            builder.identity(value);
            break;
         }
         case CREDENTIAL: {
            builder.credential(value);
            break;
         }
         case CONTAINER: {
            builder.container(value);
            break;
         }
         case ENDPOINT: {
            builder.endpoint(value);
            break;
         }
         case KEY_TO_STRING_MAPPER: {
            builder.key2StringMapper(value);
            break;
         }
         case COMPRESS: {
            builder.compress(Boolean.parseBoolean(value));
            break;
         }
         case OVERRIDES: {
            try {
               builder.overrides(parseProperties(value));
            } catch(IllegalArgumentException e) {
               ParseUtils.invalidAttributeValue(reader, i);
            }
            break;
         }
         case NORMALIZE_CACHE_NAMES: {
            builder.normalizeCacheNames(Boolean.parseBoolean(value));
            break;
         }
         default: {
            Parser.parseStoreAttribute(reader, i, builder);
            break;
         }
         }
      }
   }

   @Override
   public Namespace[] getNamespaces() {
      return ParseUtils.getNamespaceAnnotations(getClass());
   }

   private Properties parseProperties(String properties) throws IllegalArgumentException {
      if (properties == null || properties.isEmpty())
         return null;

      Properties overrides = new Properties();
      String[] props = properties.split(OVERRIDES_SEPARATOR);
      for(String prop : props) {
         String[] keyVal = prop.split(PROPERTY_SEPARATOR);
         if (keyVal.length != 2 || keyVal[0] == null || keyVal[1] == null)
            throw new IllegalArgumentException();

         overrides.put(keyVal[0].trim(), keyVal[1].trim());
      }

      return overrides;
   }
}
