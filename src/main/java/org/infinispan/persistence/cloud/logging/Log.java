/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2000 - 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.infinispan.persistence.cloud.logging;

import org.infinispan.commons.CacheConfigurationException;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log abstraction for the cloud cache store. For this module, message ids
 * ranging from 7001 to 8000 inclusively have been reserved.
 *
 * @author Galder Zamarreño
 * @author Damiano Albani
 * @since 7.2
 */
@MessageLogger(projectCode = "ISPN")
public interface Log extends org.infinispan.util.logging.Log {
   @Message(value = "Provider not specified", id = 7001)
   CacheConfigurationException providerNotSpecified();
   
   @Message(value = "Location not specified", id = 7002)
   CacheConfigurationException locationNotSpecified();

   @Message(value = "Identity not specified", id = 7003)
   CacheConfigurationException identityNotSpecified();

   @Message(value = "Credential not specified", id = 7004)
   CacheConfigurationException credentialNotSpecified();

   @Message(value = "Container not specified", id = 7005)
   CacheConfigurationException containerNotSpecified();
}
