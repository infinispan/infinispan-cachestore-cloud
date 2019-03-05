package org.infinispan.persistence.cloud.configuration.parser;

import javax.xml.stream.XMLStreamException;

import org.infinispan.configuration.serializing.AbstractStoreSerializer;
import org.infinispan.configuration.serializing.ConfigurationSerializer;
import org.infinispan.configuration.serializing.XMLExtendedStreamWriter;
import org.infinispan.persistence.cloud.configuration.CloudStoreConfiguration;

/**
 * CloudStoreConfigurationSerializer.
 *
 * @author Tristan Tarrant
 * @since 9.0
 */
public class CloudStoreConfigurationSerializer extends AbstractStoreSerializer implements ConfigurationSerializer<CloudStoreConfiguration> {

   @Override
   public void serialize(XMLExtendedStreamWriter writer, CloudStoreConfiguration configuration) throws XMLStreamException {
      writer.writeStartElement(Element.CLOUD_STORE.getLocalName());
      configuration.attributes().write(writer);
      writeCommonStoreSubAttributes(writer, configuration);
      writeCommonStoreElements(writer, configuration);
      writer.writeEndElement();
   }

}
