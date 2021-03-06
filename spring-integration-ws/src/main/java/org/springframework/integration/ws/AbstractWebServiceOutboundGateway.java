/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.ws;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UriUtils;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 * Base class for outbound Web Service-invoking Messaging Gateways.
 * 
 * @author Mark Fisher
 * @author Jonas Partner
 */
public abstract class AbstractWebServiceOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final WebServiceTemplate webServiceTemplate;

	private final UriTemplate uriTemplate;

	private final DestinationProvider destinationProvider;

	private final Map<String, Expression> uriVariableExpressions = new HashMap<String, Expression>();

	private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

	private volatile WebServiceMessageCallback requestCallback;

	private volatile boolean ignoreEmptyResponses = true;


	public AbstractWebServiceOutboundGateway(String uri, WebServiceMessageFactory messageFactory) {
		Assert.hasText(uri, "URI must not be empty");
		this.webServiceTemplate = (messageFactory != null) ?
				new WebServiceTemplate(messageFactory) : new WebServiceTemplate();
		this.destinationProvider = null;
		this.uriTemplate = new HttpUrlTemplate(uri);
	}

	public AbstractWebServiceOutboundGateway(DestinationProvider destinationProvider, WebServiceMessageFactory messageFactory) {
		Assert.notNull(destinationProvider, "DestinationProvider must not be null");
		this.webServiceTemplate = (messageFactory != null) ?
				new WebServiceTemplate(messageFactory) : new WebServiceTemplate();
		this.destinationProvider = destinationProvider;
		// we always call WebServiceTemplate methods with an explicit URI argument,
		// but in case the WebServiceTemplate is accessed directly we'll set this:
		this.webServiceTemplate.setDestinationProvider(destinationProvider);
		this.uriTemplate = null;
	}


	/**
	 * Set the Map of URI variable expressions to evaluate against the outbound message
	 * when replacing the variable placeholders in a URI template.
	 */
	public void setUriVariableExpressions(Map<String, Expression> uriVariableExpressions) {
		synchronized (this.uriVariableExpressions) {
			this.uriVariableExpressions.clear();
			this.uriVariableExpressions.putAll(uriVariableExpressions);
		}
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		this.setOutputChannel(replyChannel);
	}

	/**
	 * Specify whether empty String response payloads should be ignored.
	 * The default is <code>true</code>. Set this to <code>false</code> if
	 * you want to send empty String responses in reply Messages.
	 */
	public void setIgnoreEmptyResponses(boolean ignoreEmptyResponses) {
		this.ignoreEmptyResponses = ignoreEmptyResponses;
	}

	public void setMessageFactory(WebServiceMessageFactory messageFactory) {
		this.webServiceTemplate.setMessageFactory(messageFactory);
	}

	public void setRequestCallback(WebServiceMessageCallback requestCallback) {
		this.requestCallback = requestCallback;
	}

	public void setFaultMessageResolver(FaultMessageResolver faultMessageResolver) {
		this.webServiceTemplate.setFaultMessageResolver(faultMessageResolver);
	}

	public void setMessageSender(WebServiceMessageSender messageSender) {
		this.webServiceTemplate.setMessageSender(messageSender);
	}

	public void setMessageSenders(WebServiceMessageSender[] messageSenders) {
		this.webServiceTemplate.setMessageSenders(messageSenders);
	}

	public void setInterceptors(ClientInterceptor[] interceptors) {
		this.webServiceTemplate.setInterceptors(interceptors);
	}

	@Override
	public void onInit() {
		super.onInit();
		BeanFactory beanFactory = this.getBeanFactory();
		if (beanFactory != null) {
			this.evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}
		ConversionService conversionService = this.getConversionService();
		if (conversionService != null) {
			this.evaluationContext.setTypeConverter(new StandardTypeConverter(conversionService));
		}
		this.evaluationContext.addPropertyAccessor(new MapAccessor());
	}

	protected WebServiceTemplate getWebServiceTemplate() {
		return this.webServiceTemplate;
	}

	@Override
	public final Object handleRequestMessage(Message<?> message) {
		URI uri = prepareUri(message);
		if (uri == null) {
			throw new MessageDeliveryException(message, "Failed to determine URI for " +
					"Web Service request in outbound gateway: " + this.getComponentName());
		}
		Object responsePayload = this.doHandle(uri.toString(), message.getPayload(), this.getRequestCallback(message));
		if (responsePayload != null) {
			boolean shouldIgnore = (this.ignoreEmptyResponses
					&& responsePayload instanceof String && !StringUtils.hasText((String) responsePayload));
			if (!shouldIgnore) {
				return responsePayload;
			}
		}
		return null;
	}

	protected abstract Object doHandle(String uri, Object requestPayload, WebServiceMessageCallback requestCallback);


	private URI prepareUri(Message<?> requestMessage) {
		if (this.destinationProvider != null) {
			return this.destinationProvider.getDestination();
		}
		Map<String, Object> uriVariables = new HashMap<String, Object>();
		for (Map.Entry<String, Expression> entry : this.uriVariableExpressions.entrySet()) {
			Object value = entry.getValue().getValue(this.evaluationContext, requestMessage, String.class);
			uriVariables.put(entry.getKey(), value);
		}
		return this.uriTemplate.expand(uriVariables);
	}

	private WebServiceMessageCallback getRequestCallback(Message<?> requestMessage) {
		String soapAction = requestMessage.getHeaders().get(WebServiceHeaders.SOAP_ACTION, String.class);
		return (soapAction != null) ?
				new TypeCheckingSoapActionCallback(soapAction, this.requestCallback) : this.requestCallback;
	}


	private static class TypeCheckingSoapActionCallback extends SoapActionCallback {

		private final WebServiceMessageCallback callbackDelegate;

		TypeCheckingSoapActionCallback(String soapAction, WebServiceMessageCallback callbackDelegate) {
			super(soapAction);
			this.callbackDelegate = callbackDelegate;
		}

		@Override
		public void doWithMessage(WebServiceMessage message) throws IOException {
			if (message instanceof SoapMessage) {
				super.doWithMessage(message);
			}
			if (this.callbackDelegate != null) {
				try {
					this.callbackDelegate.doWithMessage(message);
				}
				catch (Exception e) {
					throw new MessagingException("error occurred in WebServiceMessageCallback", e);
				}
			}
		}
	}


	/**
	 * HTTP-specific subclass of UriTemplate, overriding the encode method.
	 * This was copied from RestTemplate in version 3.0.6 (since it's private)
	 */
	@SuppressWarnings("serial")
	private static class HttpUrlTemplate extends UriTemplate {

		public HttpUrlTemplate(String uriTemplate) {
			super(uriTemplate);
		}

		@Override
		protected URI encodeUri(String uri) {
			try {
				String encoded = UriUtils.encodeHttpUrl(uri, "UTF-8");
				return new URI(encoded);
			}
			catch (UnsupportedEncodingException ex) {
				// should not happen, UTF-8 is always supported
				throw new IllegalStateException(ex);
			}
			catch (URISyntaxException ex) {
				throw new IllegalArgumentException("Could not create HTTP URL from [" + uri + "]: " + ex, ex);
			}
		}
	}
	
}
