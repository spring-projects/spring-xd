/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.shell.command.fixtures;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;


/**
 * Represents a mail source. Will create a disposable {@link GreenMail} test server.
 * 
 * @author Eric Bottard
 */
public class MailSource extends DisposableMailSupport<MailSource> {

	private String protocol = "imap";

	private int port = AvailableSocketPorts.nextAvailablePort();

	private String folder = "INBOX";

	private int smtpPort = AvailableSocketPorts.nextAvailablePort();

	public DisposableMailSupport protocol(String protocol) {
		ensureNotStarted();
		this.protocol = protocol;
		return this;
	}

	public DisposableMailSupport port(int port) {
		ensureNotStarted();
		this.port = port;
		return this;
	}

	public void sendEmail(String from, String subject, String msg) {
		ensureStarted();
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

		mailSender.setHost("localhost");
		mailSender.setPort(smtpPort);
		mailSender.setProtocol("smtp");


		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		try {
			helper.setTo(ADMIN_USER + "@localhost");
			helper.setFrom(from);
			helper.setSubject(subject);
			helper.setText(msg);
			mailSender.send(message);
		}
		catch (MessagingException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	protected ServerSetup setupReceiveServer() {
		return new ServerSetup(port, "localhost", protocol);
	}

	@Override
	protected ServerSetup setupSendServer() {
		return new ServerSetup(smtpPort, "localhost", "smtp");
	}

	@Override
	protected String toDSL() {
		return String.format("mail --port=%d --protocol=%s --folder=%s --username=%s --password=%s --fixedDelay=1",
				port, protocol,
				folder, ADMIN_USER, ADMIN_PASSWORD);
	}
}
