/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.BeforeClass;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ClasspathTestModuleRegistry;
import org.springframework.xd.dirt.module.CompositeModuleRegistry;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.server.SingleNodeMain;
import org.springframework.xd.dirt.server.SingleNodeServer;
import org.springframework.xd.dirt.server.options.SingleNodeOptions;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.SimpleModule;


/**
 * @author David Turanski
 */
public abstract class AbstractStreamTests {

	private static StreamDeployer streamDeployer;

	private static ModuleDeployer moduleDeployer;

	@BeforeClass
	public static void startXDSingleNode() throws Exception {
		SingleNodeServer singleNode = SingleNodeMain.launchSingleNodeServer(new SingleNodeOptions());

		ConfigurableApplicationContext containerContext = (ConfigurableApplicationContext) singleNode.getContainer().getApplicationContext();

		ConfigurableApplicationContext adminContext = singleNode.getAdminServer().getApplicationContext();
		ClasspathTestModuleRegistry cp = new ClasspathTestModuleRegistry();
		CompositeModuleRegistry cmr1 = containerContext.getBean(CompositeModuleRegistry.class);
		cmr1.addDelegate(cp);
		CompositeModuleRegistry cmr2 = adminContext.getBean(CompositeModuleRegistry.class);
		cmr2.addDelegate(cp);
		streamDeployer = adminContext.getBean(StreamDeployer.class);
		moduleDeployer = containerContext.getBean(ModuleDeployer.class);
	}

	protected static void deployStream(String name, String config) {
		streamDeployer.save(new StreamDefinition(name, config));
		streamDeployer.deploy(name);
	}

	protected static void undeployStream(String name) {
		streamDeployer.undeploy(name);
	}

	protected static SimpleModule getDeployedModule(String streamName, int index) {
		Map<Integer, Module> streamModules = getStreamModules(streamName);
		return (SimpleModule) streamModules.get(index);
	}

	protected static SimpleModule getDeployedSource(String streamName) {
		Map<Integer, Module> streamModules = getStreamModules(streamName);
		return (SimpleModule) streamModules.get(0);
	}

	protected static SimpleModule getDeployedSink(String streamName) {
		Map<Integer, Module> streamModules = getStreamModules(streamName);
		return (SimpleModule) streamModules.get(streamModules.size() - 1);
	}

	protected static Map<Integer, Module> getStreamModules(String streamName) {
		Map<String, Map<Integer, Module>> deployedModules = moduleDeployer.getDeployedModules();
		return deployedModules.get(streamName);
	}

	protected static MessageChannel getSourceOutputChannel(String streamName) {
		SimpleModule source = getDeployedSource(streamName);
		return source.getComponent("output", MessageChannel.class);
	}

	protected static SubscribableChannel getSinkInputChannel(String streamName) {
		SimpleModule sink = getDeployedSink(streamName);
		return sink.getComponent("input", SubscribableChannel.class);
	}


	protected void sendMessageAndVerifyOutput(String streamName, Message<?> message, MessageTest test) {
		Assert.notNull(streamName, "streamName cannot be null");
		Assert.notNull(test, "test cannot be null");
		Assert.notNull(message, "message cannot be null");

		MessageChannel producer = getSourceOutputChannel(streamName);
		SubscribableChannel consumer = getSinkInputChannel(streamName);
		consumer.subscribe(test);
		producer.send(message);
		assertTrue(test.getMessageHandled());
	}

	protected void sendPayloadAndVerifyOutput(String streamName, Object payload, MessageTest test) {
		Assert.notNull(payload, "payload cannot be null");
		sendMessageAndVerifyOutput(streamName, new GenericMessage<Object>(payload), test);
	}

	protected void sendPayloadAndVerifyTappedOutput(String streamName, Object payload, String moduleToTap,
			MessageTest test) {
		Assert.notNull(payload, "payload cannot be null");
		sendMessageAndVerifyTappedOutput(streamName, new GenericMessage<Object>(payload), moduleToTap, test);
	}

	protected void sendMessageAndVerifyTappedOutput(String streamName, Message<?> message, String moduleToTap,
			MessageTest test) {
		Assert.notNull(streamName, "streamName cannot be null");
		Assert.notNull(test, "test cannot be null");
		Assert.notNull(message, "message cannot be null");

		String tapName = streamName + "Tap";
		String tapChannel = "tap:" + streamName;
		if (moduleToTap != null) {
			tapChannel = tapChannel + "." + moduleToTap;
		}

		deployStream(
				tapName,
				tapChannel + " > sink");

		MessageChannel producer = getSourceOutputChannel(streamName);
		SubscribableChannel consumer = getSinkInputChannel(tapName);

		consumer.subscribe(test);
		producer.send(message);
		assertTrue(test.getMessageHandled());

		undeployStream(tapName);
	}
}


abstract class MessageTest implements MessageHandler {

	private boolean messageHandled;

	public boolean getMessageHandled() {
		return this.messageHandled;
	}

	@Override
	public final void handleMessage(Message<?> message) throws MessagingException {
		this.test(message);
		messageHandled = true;
	}

	protected abstract void test(Message<?> message);

}
