/*
 * Copyright 2011-2014 the original author or authors.
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

package org.springframework.xd.integration.test;

import java.io.File;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;
import org.springframework.xd.integration.fixtures.Jobs;
import org.springframework.xd.integration.fixtures.Sinks;
import org.springframework.xd.integration.fixtures.Sources;
import org.springframework.xd.integration.util.ConfigUtil;
import org.springframework.xd.integration.util.JobUtils;
import org.springframework.xd.integration.util.StreamUtils;
import org.springframework.xd.integration.util.XdEc2Validation;
import org.springframework.xd.integration.util.XdEnvironment;
import org.springframework.xd.test.fixtures.AbstractModuleFixture;
import org.springframework.xd.test.fixtures.LogSink;
import org.springframework.xd.test.fixtures.SimpleFileSink;

/**
 * Base Class for Spring XD Integration classes
 *
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IntegrationTestConfig.class)
public abstract class AbstractIntegrationTest {

	private final static String STREAM_NAME = "ec2Test3";

	private final static String JOB_NAME = "ec2Job3";

	protected final static String XD_DELIMETER = " | ";


	@Autowired
	protected XdEnvironment xdEnvironment;

	@Autowired
	protected XdEc2Validation validation;

	protected URL adminServer;

	@Value("${xd_pause_time}")
	protected int pauseTime;

	@Value("${xd_run_on_ec2}")
	protected boolean isOnEc2;

	@Autowired
	protected Sources sources;

	@Autowired
	protected Sinks sinks;

	@Autowired
	protected Jobs jobs;

	@Autowired
	protected ConfigUtil configUtil;

	private boolean initialized = false;


	/**
	 * Initializes the environment before the test. Also verfies that the admin is up and at least one container is
	 * available.
	 *
	 */
	public void initializer() {
		if (!initialized) {
			adminServer = xdEnvironment.getAdminServerUrl();
			validation.verifyXDAdminReady(adminServer);
			validation.verifyAtLeastOneContainerAvailable(xdEnvironment.getContainerUrls(),
					xdEnvironment.getJmxPort());
			initialized = true;
		}
	}

	/**
	 * Destroys the temporary directory.
	 */
	@AfterClass
	public static void tearDownAfterClass() {
		File file = new File(StreamUtils.TMP_DIR);
		if (file.exists()) {
			file.delete();
		}

	}

	/**
	 * Destroys all streams in the xd cluster and calls initializer.
	 */
	@Before
	public void setup() {
		initializer();
		StreamUtils.destroyAllStreams(adminServer);
		JobUtils.destroyAllJobs(adminServer);
		waitForXD();
	}

	/**
	 * Destroys all stream created in the test.
	 */
	@After
	public void tearDown() {
		StreamUtils.destroyAllStreams(adminServer);
		JobUtils.destroyAllJobs(adminServer);
		waitForXD();
	}


	/**
	 * Creates a stream on the XD cluster defined by the test's Artifact or Environment variables Uses STREAM_NAME as
	 * default stream name.
	 *
	 * @param stream the stream definition
	 */
	public void stream(String stream) {
		Assert.hasText(stream, "stream needs to be poopulated with a definition and can not be null");
		stream(STREAM_NAME, stream);
	}

	/**
	 * Creates a stream on the XD cluster defined by the test's Artifact or Environment variables
	 *
	 * @param streamName the name of the stream
	 * @param stream the stream definition
	 */
	public void stream(String streamName, String stream) {
		Assert.hasText(streamName, "stream name can not be empty nor null");
		Assert.hasText(stream, "stream needs to be populated with a definition and can not be null");
		StreamUtils.stream(streamName, stream, adminServer);
		waitForXD();
	}

	/**
	 * Creates a job on the XD cluster defined by the test's Artifact or Environment variables Uses JOB_NAME as default
	 * job name.
	 *
	 * @param job the job definition
	 */
	public void job(String job) {
		Assert.hasText(job, "job needs to be poopulated with a definition and can not be null");
		job(JOB_NAME, job);
	}

	/**
	 * Creates a job on the XD cluster defined by the test's Artifact or Environment variables
	 *
	 * @param jobName the name of the job
	 * @param job the job definition
	 */
	public void job(String jobName, String job) {
		Assert.hasText(jobName, "job name can not be empty nor null");
		Assert.hasText(job, "job needs to be populated with a definition and can not be null");
		JobUtils.job(jobName, job, adminServer);
		waitForXD();
	}

	/**
	 * Launches a job with the test's JOB_NAME on the XD instance.
	 */
	public void jobLaunch() {
		JobUtils.launch(adminServer, JOB_NAME);
	}

	/**
	 * Launches a job on the XD instance
	 *
	 * @param jobName The name of the job to be launched
	 */
	public void jobLaunch(String jobName) {
		JobUtils.launch(adminServer, jobName);
	}

	/**
	 * Gets the URL of the container where the stream was deployed
	 *
	 * @param streamName Used to find the container that contains the stream.
	 * @return The URL that contains the stream.
	 */
	public URL getContainerForStream(String streamName) {
		Assert.hasText(streamName, "stream name can not be empty nor null");
		// Assuming one container for now.
		return xdEnvironment.getContainerUrls().get(0);
	}

	/**
	 * Verifies that the expected number of messages were received by all modules in a stream.
	 *
	 */
	public void assertReceived(int msgCountExpected) {
		waitForXD();

		validation.assertReceived(StreamUtils.replacePort(
				getContainerForStream(STREAM_NAME), xdEnvironment.getJmxPort()),
				STREAM_NAME, msgCountExpected);
	}

	/**
	 * Verifies that a message was received by the module.
	 *
	 */
	public void assertReceived(String moduleName, int msgCountExpected) {
		Assert.hasText(moduleName, "moduleName must not be empty nor null");
		waitForXD();

		validation.assertReceived(StreamUtils.replacePort(
				getContainerForStream(STREAM_NAME), xdEnvironment.getJmxPort()),
				STREAM_NAME, moduleName, msgCountExpected);
	}

	/**
	 * Verifies that the data stored by the sink is what was expected.
	 *
	 * @param data - expected data
	 * @param sinkInstance determines whether to look at the log or file for the result
	 */
	public void assertValid(String data, AbstractModuleFixture sinkInstance) {
		Assert.hasText(data, "data can not be empty nor null");
		Assert.notNull(sinkInstance, "sinkInstance must not be null");

		if (sinkInstance.getClass().equals(SimpleFileSink.class)) {
			assertValidFile(data, getContainerForStream(STREAM_NAME), STREAM_NAME);
		}
		if (sinkInstance.getClass().equals(LogSink.class)) {
			assertLogEntry(data, getContainerForStream(STREAM_NAME));
		}

	}

	/**
	 * Verifies that the data stored by the sink is what was expected.
	 *
	 * @param data - expected data
	 * @param sinkInstance determines whether to look at the log or file for the result
	 */
	public void assertContains(String data) {
		Assert.hasText(data, "data can not be empty nor null");
		assertFileContains(data, getContainerForStream(STREAM_NAME), STREAM_NAME);
	}

	/**
	 * Wait the "waitTime" for a stream to be deployed.
	 *
	 * @param waitTime the time in millis to wait.
	 * @return true if deployed else false.
	 */
	public boolean waitForStreamDeployment(int waitTime) {
		return waitForStreamDeployment(STREAM_NAME, waitTime);
	}

	/**
	 * Wait the "waitTime" for a stream to be deployed.
	 *
	 * @param streamName the name of stream to be evaluated.
	 * @param waitTime the time in millis to wait.
	 * @return true if deployed else false.
	 */
	public boolean waitForStreamDeployment(String streamName, int waitTime) {
		Assert.hasText(streamName, "streamName must not be empty nor null");
		return StreamUtils.waitForStreamDeployment(streamName, adminServer, waitTime);
	}


	/**
	 * Wait the "waitTime" for a job to be deployed.
	 *
	 * @param waitTime the time in millis to wait.
	 * @return true if deployed else false.
	 */
	public boolean waitForJobDeployment(int waitTime) {
		return waitForJobDeployment(JOB_NAME, waitTime);
	}

	/**
	 * Wait the "waitTime" for a job to be deployed.
	 *
	 * @param jobName the name of stream to be evaluated.
	 * @param waitTime the time in millis to wait.
	 * @return true if deployed else false.
	 */
	public boolean waitForJobDeployment(String jobName, int waitTime) {
		Assert.hasText(jobName, "jobName must not be empty nor null");
		return JobUtils.waitForJobDeployment(jobName, adminServer, waitTime);
	}


	/**
	 * Checks the file data to see if the data is contained in the file.
	 *
	 * @param data The data to validate the file content against.
	 * @param url The URL of the server that we will ssh, to get the data.
	 * @param streamName the name of the file we are retrieving from the remote server.
	 */
	private void assertFileContains(String data, URL url, String streamName)
	{
		waitForXD(pauseTime * 2000);
		String fileName = XdEnvironment.RESULT_LOCATION + "/" + streamName
				+ ".out";
		validation.verifyContentContains(xdEnvironment, url, fileName, data);
	}

	/**
	 * Checks the file data to see if it matches what is expected.
	 *
	 * @param data The data to validate the file content against.
	 * @param url The URL of the server that we will ssh, to get the data.
	 * @param streamName the name of the file we are retrieving from the remote server.
	 */
	private void assertValidFile(String data, URL url, String streamName)
	{
		waitForXD(pauseTime * 2000);
		String fileName = XdEnvironment.RESULT_LOCATION + "/" + streamName
				+ ".out";
		validation.verifyTestContent(xdEnvironment, url, fileName, data);
	}

	/**
	 * Checks the log to see if the data specified is in the log.
	 *
	 * @param data The data to check if it is in the log file
	 * @param url The URL of the server we will ssh, to get the data.
	 */
	private void assertLogEntry(String data, URL url)
	{
		waitForXD();
		validation.verifyContentContains(xdEnvironment, url, xdEnvironment.getContainerLogLocation(), data);
	}

	protected void waitForXD() {
		waitForXD(pauseTime * 1000);
	}

	protected void waitForXD(int millis) {
		try {
			Thread.sleep(millis);
		}
		catch (Exception ex) {
			// ignore
		}

	}

	public XdEnvironment getEnvironment() {
		return xdEnvironment;
	}

}
