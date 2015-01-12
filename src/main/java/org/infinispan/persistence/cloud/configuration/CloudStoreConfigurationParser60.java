package org.infinispan.persistence.cloud.configuration;

import static org.infinispan.commons.util.StringPropertyReplacer.replaceProperties;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ConfigurationParser;
import org.infinispan.configuration.parsing.ParseUtils;
import org.infinispan.configuration.parsing.Parser60;
import org.infinispan.configuration.parsing.XMLExtendedStreamReader;

/**
*
* CloudStoreConfigurationParser60.
*
* @author Galder Zamarreño
* @author Damiano Albani
* @since 6.0
*/
public class CloudStoreConfigurationParser60 implements ConfigurationParser {

   public CloudStoreConfigurationParser60() {
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
            Parser60.parseCommonStoreChildren(reader, builder);
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
         case KEY_TO_STRING_MAPPER: {
            builder.key2StringMapper(value);
            break;
         }
         default: {
            Parser60.parseCommonStoreAttributes(reader, builder, attributeName, value, i);
            break;
         }
         }
      }
   }
}
