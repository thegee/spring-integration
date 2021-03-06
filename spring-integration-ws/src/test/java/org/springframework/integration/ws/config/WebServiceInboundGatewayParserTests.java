/*
 *  Copyright 2002-2010 the original author or authors.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.integration.ws.config;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.Source;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.ws.MarshallingWebServiceInboundGateway;
import org.springframework.integration.ws.SimpleWebServiceInboundGateway;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.support.AbstractMarshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.ws.context.DefaultMessageContext;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapHeader;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class WebServiceInboundGatewayParserTests {

	@Autowired
	@Qualifier("requestsMarshalling")
	PollableChannel requestsMarshalling;
	
	@Autowired
	@Qualifier("requestsSimple")
	PollableChannel requestsSimple;
	
	@Autowired
	@Qualifier("customErrorChannel")
	MessageChannel customErrorChannel;
	
	@Autowired
	@Qualifier("requestsVerySimple")
	MessageChannel requestsVerySimple;

	@Test
	public void configOk() throws Exception {
		// config valid
	}

	//Simple
	@Autowired
	@Qualifier("simple")
	SimpleWebServiceInboundGateway simpleGateway;

	@Test
	public void simpleGatewayProperties() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(simpleGateway);
		assertThat(
				(MessageChannel) accessor.getPropertyValue("requestChannel"),
				is(requestsVerySimple));
		
		assertThat(
				(MessageChannel) accessor.getPropertyValue("errorChannel"),
				is(customErrorChannel));
	}

	//extractPayload = false
	@Autowired
	@Qualifier("extractsPayload")
	SimpleWebServiceInboundGateway payloadExtractingGateway;

	@Test
	public void extractPayloadSet() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				payloadExtractingGateway);
		assertThat((Boolean) accessor.getPropertyValue("extractPayload"),
				is(false));
	}

	//marshalling
	@Autowired
	@Qualifier("marshalling")
	MarshallingWebServiceInboundGateway marshallingGateway;
	
	@Autowired
	AbstractMarshaller marshaller;

	@Test
	public void marshallersSet() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(marshallingGateway);
		assertThat((AbstractMarshaller) accessor.getPropertyValue("marshaller"),
				is(marshaller));
		assertThat((AbstractMarshaller) accessor.getPropertyValue("unmarshaller"),
				is(marshaller));
		assertTrue("messaging gateway is not running", marshallingGateway.isRunning());

		assertThat(
				(MessageChannel) accessor.getPropertyValue("errorChannel"),
				is(customErrorChannel));
	}

	@Test
	public void testMessageHistoryWithMarshallingGateway() throws Exception {
		MessageContext context = new DefaultMessageContext(new StubMessageFactory());
		Unmarshaller unmarshaller = mock(Unmarshaller.class);
		when(unmarshaller.unmarshal((Source)Mockito.any())).thenReturn("hello");
		marshallingGateway.setUnmarshaller(unmarshaller);
		marshallingGateway.invoke(context);
		Message<?> message = requestsMarshalling.receive(100);
		MessageHistory history = MessageHistory.read(message);
		assertNotNull(history);
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "marshalling", 0);
		assertNotNull(componentHistoryRecord);
		assertEquals("ws:outbound-gateway", componentHistoryRecord.get("type"));
	}

	@Test
	public void testMessageHistoryWithSimpleGateway() throws Exception {
		MessageContext context = new DefaultMessageContext(new StubMessageFactory());
		payloadExtractingGateway.invoke(context);
		Message<?> message = requestsSimple.receive(100);
		MessageHistory history = MessageHistory.read(message);
		assertNotNull(history);
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "extractsPayload", 0);
		System.out.println(componentHistoryRecord);
		assertNotNull(componentHistoryRecord);
		assertEquals("ws:outbound-gateway", componentHistoryRecord.get("type"));
	}

	@Autowired
	private SimpleWebServiceInboundGateway headerMappingGateway;

	@Autowired
	private HeaderMapper<SoapHeader> testHeaderMapper;

	@Test
	public void testHeaderMapperReference() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(headerMappingGateway);
		Object headerMapper = accessor.getPropertyValue("headerMapper");
		assertEquals(testHeaderMapper, headerMapper);
	}


	@SuppressWarnings("unused")
	private static class TestHeaderMapper implements HeaderMapper<SoapHeader> {

		public void fromHeaders(MessageHeaders headers, SoapHeader target) {
		}

		public Map<String, ?> toHeaders(SoapHeader source) {
			return Collections.emptyMap();
		}
	}

}
