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

package org.springframework.commons.serializer.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.springframework.commons.serializer.InputStreamingConverter;
import org.springframework.commons.serializer.OutputStreamingConverter;


/**
 * Converter that implements both input and output streaming using Java
 * Serialization.
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public class JavaStreamingConverter 
		implements InputStreamingConverter<Object>, 
		           OutputStreamingConverter<Object> {

	public Object convert(InputStream inputStream) throws IOException {
		ObjectInputStream objectInputStream = null;
		try {
			objectInputStream = new ObjectInputStream(inputStream);
			return objectInputStream.readObject();
		} catch (ClassNotFoundException e) {
			if (objectInputStream != null) {
				objectInputStream.close();
			}
			throw new IOException(e);
		}
	}

	public void convert(Object object, OutputStream outputStream)
			throws IOException {
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
		objectOutputStream.writeObject(object);
		objectOutputStream.flush();
	}

}