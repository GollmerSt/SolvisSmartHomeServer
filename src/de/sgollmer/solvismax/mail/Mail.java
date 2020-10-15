/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.mail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.xml.ArrayXml;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Mail {

	private static final ILogger logger = LogManager.getInstance().getLogger(Mail.class);

	private static boolean DEBUG = false; // kein Mailversand

	enum Security {
		TLS, SSL, NONE
	};

	private static Map<String, RecipientType> recipientTypeMap = new HashMap<>(5);

	static {
		recipientTypeMap.put("TO", RecipientType.TO);
		recipientTypeMap.put("CC", RecipientType.CC);
		recipientTypeMap.put("BCC", RecipientType.BCC);
	}

	static class Recipient implements ArrayXml.IElement<Recipient> {
		private final String name;
		private final String address;
		private final RecipientType type;

		private Recipient(String name, String address, RecipientType type) {
			this.name = name;
			this.address = address;
			this.type = type;
		}

		Recipient() {
			this(null, null, null);
		}

		private static class Creator extends CreatorByXML<Recipient> {
			private String name;
			private String address;
			private RecipientType type;

			private Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				switch (name.getLocalPart()) {
					case "name":
						this.name = value;
						break;
					case "address":
						this.address = value;
						break;
					case "type":
						this.type = recipientTypeMap.get(value);
						break;
				}
			}

			@Override
			public Recipient create() throws XmlException, IOException {
				return new Recipient(this.name, this.address, this.type);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
			}

		}

		@Override
		public CreatorByXML<Recipient> getCreator(String name, BaseCreator<?> creator) {
			return new Creator(name, creator);
		}
	}

	static void send(String subject, String text, String name, String from, CryptAes password, Security security,
			String provider, int port, Collection<Recipient> recipients, MyImage image, Proxy proxy)
			throws MessagingException, IOException {

		String portString = Integer.toString(port);

		logger.info(security.name() + "Email Start");
		Properties props = new Properties();
		props.put("mail.smtp.host", provider); // SMTP Host
		if (proxy != null) {
			props.put("mail.smtp.proxy.host", proxy.getHost());
			props.put("mail.smtp.proxy.port", Integer.toString(proxy.getPort()));
			if (proxy.getUser() != null) {
				props.put("mail.smtp.proxy.user", proxy.getUser());
			}
			if (proxy.getPassword() != null) {
				props.put("mail.smtp.proxy.password", new String(proxy.getPassword().cP()));
			}
		}
		switch (security) {
			case SSL:
				props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // SSL Factory Class
				props.put("mail.smtp.socketFactory.port", portString); // SSL Port
				props.put("mail.smtp.ssl.trust", "*");
				break;
			case TLS:
				props.put("mail.smtp.starttls.enable", "true");
//				props.put("mail.smtp.EnableSSL.enable", "true");
				props.put("mail.smtp.ssl.trust", "*");
				break;
			case NONE:
				break;
			default:
				throw new MessagingException("Mail security type \"" + security.name() + "\" unknown.");
		}
		props.put("mail.smtp.auth", "true"); // Enabling SMTP Authentication
		props.put("mail.smtp.port", portString); // SMTP Port

		Authenticator auth = new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				char[] p = password.cP();
				PasswordAuthentication pA = new PasswordAuthentication(from, new String(p));
				Arrays.fill(p, '\0');
				return pA;
			}
		};

		Session session = Session.getDefaultInstance(props, auth);

		System.out.println("Session created");
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from, name));
		for (Recipient recipient : recipients) {
			message.addRecipient(recipient.type, new InternetAddress(recipient.address, recipient.name));
		}
		message.setSubject(subject);

		Multipart multipart = new MimeMultipart();

		MimeBodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setText(text);
		multipart.addBodyPart(messageBodyPart);

		if (image != null) {
			messageBodyPart = new MimeBodyPart();
			ByteArrayDataSource bds = image.getByteArrayDataSource();
			messageBodyPart.setDataHandler(new DataHandler(bds));
			messageBodyPart.setFileName(Constants.Files.SOLVIS_SCREEN);
			messageBodyPart.setHeader("Content-ID", "<image>");
			multipart.addBodyPart(messageBodyPart);
		}
		message.setContent(multipart);

		logger.info("Send email...");
		if (!DEBUG) {
			Transport.send(message);
		} else {
			logger.info("Text of mail: " + subject);
		}
		logger.info("Email was sent.");
	}

}
