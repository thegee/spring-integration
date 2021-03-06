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

package org.springframework.integration.http;

/**
 * Modified version of the MockHttpServletResponse that sets the "Content-Type"
 * header so that it will be available from a ServletServerHttpResponse instance.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class MockHttpServletResponse extends org.springframework.mock.web.MockHttpServletResponse {

	public MockHttpServletResponse() {
		super();
	}


	@Override
	public void setContentType(String contentType) {
		this.addHeader("Content-Type", contentType);
	}

	@Override
	public String getContentType() {
		return (String) this.getHeader("Content-Type");
	}

}
