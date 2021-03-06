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

package org.springframework.integration.rmi;

import org.springframework.integration.gateway.AbstractRemotingOutboundGateway;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

/**
 * An outbound Messaging Gateway for RMI-based remoting.
 * 
 * @author Mark Fisher
 */
@SuppressWarnings("deprecation")
public class RmiOutboundGateway extends AbstractRemotingOutboundGateway {

	public RmiOutboundGateway(String url) {
		super(url);
	}


	@Override
	public RequestReplyExchanger createProxy(String url) {
		RmiProxyFactoryBean proxyFactory = new RmiProxyFactoryBean();
		proxyFactory.setServiceInterface(RequestReplyExchanger.class);
		proxyFactory.setServiceUrl(url);
		proxyFactory.setLookupStubOnStartup(false);
		proxyFactory.setRefreshStubOnConnectFailure(true);
		proxyFactory.afterPropertiesSet();
		return (RequestReplyExchanger) proxyFactory.getObject();
	}

}
