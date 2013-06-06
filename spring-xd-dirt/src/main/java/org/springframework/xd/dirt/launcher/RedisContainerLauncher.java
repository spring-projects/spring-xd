/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.launcher;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.container.DefaultContainer;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.event.ContainerStartedEvent;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author David Turanski
 */
public class RedisContainerLauncher implements ContainerLauncher, ApplicationEventPublisherAware {

	private final RedisAtomicLong ids;

	private volatile ApplicationEventPublisher eventPublisher;

	private static Log logger = LogFactory.getLog(RedisContainerLauncher.class);

	public RedisContainerLauncher(RedisConnectionFactory connectionFactory) {
		this.ids = new RedisAtomicLong("idsequence", connectionFactory);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public Container launch() {
		long id = ids.incrementAndGet();
		DefaultContainer container = new DefaultContainer(id + "");
		container.start();
		container.addListener(new ShutdownListener(container));
		this.eventPublisher.publishEvent(new ContainerStartedEvent(container));
		return container;
	}

	public static void main(String[] args) {
		setXDProperties(args);
		ClassPathXmlApplicationContext context = null;
		try {
			context = new ClassPathXmlApplicationContext("META-INF/spring/launcher.xml");
		}
		catch (BeanCreationException e) {
			if (e.getCause() instanceof RedisConnectionFailureException) {
				logger.fatal(e.getCause().getMessage());
				System.err.println("Redis does not seem to be running. Did you install and start Redis? " +
						"Please see the Getting Started section of the guide for instructions.");
				System.exit(1);
			}
		}
		context.registerShutdownHook();
		ContainerLauncher launcher = context.getBean(ContainerLauncher.class);
		launcher.launch();
	}

	/**
	 * Set xd.home and xd.transport system properties
	 * @param args
	 */
	private static void setXDProperties(String[] args) {
		String xdhome = System.getProperty("xd.home");
		if (!StringUtils.hasText(xdhome)) {
			xdhome = (args.length > 0) ? args[0] : "..";
			System.setProperty("xd.home", xdhome);
		}
		String xdtransport = System.getProperty("xd.transport");
		if (!StringUtils.hasText(xdtransport)) {
			xdtransport = (args.length > 1) ? args[1] : "redis";
			System.setProperty("xd.transport", xdtransport);
		}
		logger.info("xd.home=" + new File(xdhome).getAbsolutePath()
				+ ", xd.transport=" + xdtransport);
	}


	private static class ShutdownListener implements ApplicationListener<ContextClosedEvent> {

		private final Container container;

		ShutdownListener(Container container) {
			this.container = container;
		}

		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			container.stop();
		}
	}

}
