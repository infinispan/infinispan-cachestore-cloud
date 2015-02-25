package org.infinispan.persistence.cloud.configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumerates the attributes used by the Cloud cache store configuration
 *
 * @author Tristan Tarrant
 * @author Damiano Albani
 * @since 7.2
 */
public enum Attribute {
   // must be first
   UNKNOWN(null),

   PROVIDER("provider"),
   LOCATION("location"),
   IDENTITY("identity"),
   CREDENTIAL("credential"),
   CONTAINER("container"),
   KEY_TO_STRING_MAPPER("key-to-string-mapper"),
   COMPRESS("compress")
   ;

   private final String name;

   private Attribute(final String name) {
      this.name = name;
   }

   /**
    * Get the local name of this element.
    *
    * @return the local name
    */
   public String getLocalName() {
      return name;
   }

   private static final Map<String, Attribute> attributes;

   static {
      final Map<String, Attribute> map = new HashMap<String, Attribute>(64);
      for (Attribute attribute : values()) {
         final String name = attribute.getLocalName();
         if (name != null) {
            map.put(name, attribute);
         }
      }
      attributes = map;
   }

   public static Attribute forName(final String localName) {
      final Attribute attribute = attributes.get(localName);
      return attribute == null ? UNKNOWN : attribute;
   }
}
