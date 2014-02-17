/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.x.bus.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.lang.ClassUtils;

import org.springframework.messaging.Message;
import org.springframework.util.MimeType;


/**
 * 
 * @author David Turanski
 */
public class JavaToSerializedMessageConverter extends AbstractFromMessageConverter {

	public JavaToSerializedMessageConverter() {
		super(MimeType.valueOf("application/x-java-object"),
				MimeType.valueOf("application/x-java-serialized-object"));
	}

	@Override
	protected boolean supportsPayloadType(Class<?> clazz) {
		return ClassUtils.isAssignable(clazz, Serializable.class);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return ClassUtils.isAssignable(clazz, byte[].class);
	}

	@Override
	public Object convertFromInternal(Message<?> message, Class<?> targetClass) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(bos).writeObject(message.getPayload());
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
			return null;
		}

		return buildConvertedMessage(bos.toByteArray(), message.getHeaders(),
				MimeType.valueOf("application/x-java-serialized-object"));
	}

}
