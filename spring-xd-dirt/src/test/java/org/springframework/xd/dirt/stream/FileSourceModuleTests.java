/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.integration.file.FileHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.ContentTypeResolver;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.xd.dirt.integration.bus.StringConvertingContentTypeResolver;
import org.springframework.xd.dirt.plugins.ModuleConfigurationException;


/**
 *
 * @author David Turanski
 * @author Gunnar Hillert
 */
public class FileSourceModuleTests extends StreamTestSupport {

	private static String tmpDirName = System.getProperty("java.io.tmpdir");

	private static String sourceDirName =
			tmpDirName + (tmpDirName.endsWith(File.separator) ? "" : File.separator) + "filesourcetests";

	private static File sourceDir = new File(sourceDirName);

	ContentTypeResolver contentTypeResolver = new StringConvertingContentTypeResolver();

	@BeforeClass
	public static void createTempDir() throws IOException {
		FileUtils.forceMkdir(sourceDir);
	}

	@Before
	public void setUp() throws IOException {
		if (sourceDir.exists()) {
			FileUtils.cleanDirectory(sourceDir);
		}
	}

	@Test
	public void testFileContents() throws IOException {
		deployStream(
				"filecontents",
				"file --dir=" + sourceDirName + " --fixedDelay=0 | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				byte[] bytes = (byte[]) message.getPayload();
				assertEquals("foo", new String(bytes));
				assertEquals("foo.txt", message.getHeaders().get(FileHeaders.FILENAME, String.class));
				assertEquals(MimeType.valueOf("application/octet-stream"),
						contentTypeResolver.resolve(message.getHeaders()));
			}
		};
		StreamTestSupport.getSinkInputChannel("filecontents").subscribe(test);
		dropFile("foo.txt");
		test.waitForCompletion(1000);
		undeployStream("filecontents");
		assertTrue(test.getMessageHandled());
	}

	@Test
	public void testFileContentsAsString() throws IOException {
		deployStream(
				"filestring",
				"file --dir=" + sourceDirName + " --fixedDelay=0 --outputType='text/plain;charset=UTF-8' | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertEquals("foo", message.getPayload());
				assertEquals("foo2.txt", message.getHeaders().get(FileHeaders.FILENAME, String.class));
				assertEquals(MimeTypeUtils.APPLICATION_OCTET_STREAM, contentTypeResolver.resolve(message.getHeaders()));
			}
		};
		StreamTestSupport.getSinkInputChannel("filestring").subscribe(test);
		dropFile("foo2.txt");
		test.waitForCompletion(3000);
		StreamTestSupport.getDeployedModule("filestring", 0).stop();
		assertTrue(test.getMessageHandled());
	}

	@Test
	public void testFileReference() throws IOException {
		deployStream(
				"fileref",
				"file --mode=ref --dir=" + sourceDirName + " --fixedDelay=0 | sink");
		MessageTest test = new MessageTest() {

			@Override
			public void test(Message<?> message) throws MessagingException {
				File file = (File) message.getPayload();
				assertEquals(sourceDirName + File.separator + "foo1.txt", file.getAbsolutePath());
			}
		};
		StreamTestSupport.getSinkInputChannel("fileref").subscribe(test);
		dropFile("foo1.txt");
		test.waitForCompletion(1000);
		StreamTestSupport.getDeployedModule("fileref", 0).stop();
		assertTrue(test.getMessageHandled());
	}

	@Test
	public void testTextLineMode() throws IOException {
		deployStream(
				"textLine",
				"file --mode=textLine --dir=" + sourceDirName + " --fixedDelay=0 | sink");
		MessageTest test = new MessageTest() {

			private AtomicInteger counter = new AtomicInteger(0);

			@Override
			public void test(Message<?> message) throws MessagingException {
				assertTrue("Extected a String", message.getPayload() instanceof String);
				assertEquals("foo", message.getPayload());
				assertEquals("foo1.txt", message.getHeaders().get(FileHeaders.FILENAME, String.class));
				counter.incrementAndGet();
			}

			@Override
			public boolean getMessageHandled() {
				if (counter.get() == 10) {
					super.messageHandled = true;
				}
				return super.messageHandled;
			}

		};
		StreamTestSupport.getSinkInputChannel("textLine").subscribe(test);
		dropFile("foo1.txt", 10);
		test.waitForCompletion(3000);
		undeployStream("textLine");
		assertTrue(test.getMessageHandled());

	}

	@Test
	public void testTextLineModeWithIncorrectMode() throws IOException {
		try {
			deployStream(
					"failme",
					"file --mode=failme --dir=" + sourceDirName + " --fixedDelay=0 | sink");
		}
		catch (ModuleConfigurationException e) {
			String expectation = "mode: Property 'mode' threw exception; nested exception is java.lang.IllegalArgumentException: Not a valid mode 'failme'. Acceptable values are: ref,textLine,fileAsBytes";
			assertTrue("Expected the exception to contain: " + expectation, e.getMessage().contains(expectation));
			return;
		}

		fail("Was expecting an exception to be thrown.");

	}

	private void dropFile(String fileName) throws IOException {
		dropFile(fileName, 1);
	}

	private void dropFile(String fileName, int lines) throws IOException {
		PrintWriter writer = new PrintWriter(sourceDirName + "/" + fileName, "UTF-8");
		for (int i = 1; i <= lines; i++) {
			writer.write("foo");
			if (i < lines) {
				writer.write("\n");
			}
		}
		writer.close();
	}

	@AfterClass
	public static void deleteTempDir() throws IOException {
		if (sourceDir.exists()) {
			FileUtils.cleanDirectory(sourceDir);
			FileUtils.deleteDirectory(sourceDir);
		}
	}
}
