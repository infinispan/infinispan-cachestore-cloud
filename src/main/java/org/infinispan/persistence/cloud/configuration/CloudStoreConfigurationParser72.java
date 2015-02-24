package org.infinispan.persistence.cloud.configuration;

import static org.infinispan.commons.util.StringPropertyReplacer.replaceProperties;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ConfigurationParser;
import org.infinispan.configuration.parsing.Namespace;
import org.infinispan.configuration.parsing.Namespaces;
import org.infinispan.configuration.parsing.ParseUtils;
import org.infinispan.configuration.parsing.Parser71;
import org.infinispan.configuration.parsing.XMLExtendedStreamReader;
import org.kohsuke.MetaInfServices;


/**
*
* CloudStoreConfigurationParser71.
* 
* @since 7.2
*/
@MetaInfServices
@Namespaces({
      @Namespace(uri = "urn:infinispan:config:store:cloud:7.2",
                 root = CloudStoreConfigurationParser72.ROOT_ELEMENT),
      @Namespace(root = CloudStoreConfigurationParser72.ROOT_ELEMENT)
})
public class CloudStoreConfigurationParser72 implements ConfigurationParser {

   public static final String ROOT_ELEMENT = "cloud-store";
   
   public CloudStoreConfigurationParser72() {
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
            Parser71.parseStoreElement(reader, builder);
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
         case KEY_TO_STRING_MAPPER: {
            builder.key2StringMapper(value);
            break;
         }
         default: {
            Parser71.parseStoreAttribute(reader, i, builder);
            break;
         }
         }
      }
   }

   @Override
   public Namespace[] getNamespaces() {
      return ParseUtils.getNamespaceAnnotations(getClass());
   }
}
