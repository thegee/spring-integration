/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods and constants for IP adapter parsers.
 * 
 * @author Gary Russell
 * @since 2.0
 */
public abstract class IpAdapterParserUtils {

	static final String IP_PROTOCOL_ATTRIBUTE = "protocol";

	static final String UDP_MULTICAST = "multicast";

	static final String MULTICAST_ADDRESS = "multicast-address";

	static final String PORT = "port";

	static final String HOST = "host";

	static final String CHECK_LENGTH = "check-length";

	static final String SO_TIMEOUT = "so-timeout";

	static final String SO_RECEIVE_BUFFER_SIZE = "so-receive-bufffer-size";

	static final String SO_SEND_BUFFER_SIZE = "so-send-buffer-size";

	static final String RECEIVE_BUFFER_SIZE = "receive-buffer-size";

	static final String POOL_SIZE = "pool-size";

	static final String ACK = "acknowledge";

	static final String ACK_HOST = "ack-host";

	static final String ACK_PORT = "ack-port";

	static final String ACK_TIMEOUT = "ack-timeout";

	static final String MIN_ACKS_SUCCESS = "min-acks-for-success";

	static final String TIME_TO_LIVE = "time-to-live";


	/**
	 * Adds a constructor-arg to the bean definition with the value
	 * of the attribute whose name is provided if that attribute is
	 * defined in the given element.
	 * 
	 * @param beanDefinition the bean definition to be configured
	 * @param element the XML element where the attribute should be defined
	 * @param attributeName the name of the attribute whose value will be
	 * used to populate the property
	 */
	public static void addConstuctirValueIfAttributeDefined(BeanDefinitionBuilder builder,
			Element element, String attributeName, boolean trueFalse) {
		String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue)) {
			builder.addConstructorArgValue(attributeValue);
		}
	}

	/**
	 * Asserts that a protocol attribute (udp or tcp) is supplied,
	 * @param element
	 * @return The value of the attribute.
	 * @throws BeanCreationException if attribute not provided or invalid.
	 */
	static String getProtocol(Element element) {
		String protocol = element.getAttribute(IpAdapterParserUtils.IP_PROTOCOL_ATTRIBUTE);
		if (!StringUtils.hasText(protocol)) {
			throw new BeanCreationException(IpAdapterParserUtils.IP_PROTOCOL_ATTRIBUTE + 
					" is required for an IP channel adapter");
		}
		if (!protocol.equals("tcp") && !protocol.equals("udp")) {
			throw new BeanCreationException(IpAdapterParserUtils.IP_PROTOCOL_ATTRIBUTE + 
					" must be 'tcp' or 'udp' for an IP channel adapter");
		}
		return protocol;
	}

	/**
	 * Asserts that a port attribute is supplied.
	 * @param element
	 * @return The value of the attribute.
	 * @throws BeanCreationException if attribute is not provided.
	 */
	static String getPort(Element element) {
		String port = element.getAttribute(IpAdapterParserUtils.PORT);
		if (!StringUtils.hasText(port)) {
			throw new BeanCreationException(IpAdapterParserUtils.PORT +
					" is required for IP channel adapters");
		}
		return port;
	}

	/**
	 * Gets the multicast attribute, if present; if not returns 'false'.
	 * @param element
	 * @return The value of the attribute or false.
	 */
	static String getMulticast(Element element) {
		String multicast = element.getAttribute(IpAdapterParserUtils.UDP_MULTICAST);
		if (!StringUtils.hasText(multicast)) {
			multicast = "false";
		}
		return multicast;
	}

	/**
	 * Sets the common port attributes on the bean being built (timeout, receive buffer size,
	 * send buffer size).
	 * @param builder 
	 * @param element
	 */
	static void addCommonSocketOptions(BeanDefinitionBuilder builder, Element element) {
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SO_TIMEOUT);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SO_RECEIVE_BUFFER_SIZE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SO_SEND_BUFFER_SIZE);
	}

}