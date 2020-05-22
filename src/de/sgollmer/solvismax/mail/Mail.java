/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.mail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.imageio.ImageIO;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.xml.ArrayXml;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Mail {

	private static Logger logger;
	private static boolean DEBUG = false; // kein Mailversand

	public enum Security {
		TLS, SSL
	};

	private static Map<String, RecipientType> recipientTypeMap = new HashMap<>(5);

	static {
		recipientTypeMap.put("TO", RecipientType.TO);
		recipientTypeMap.put("CC", RecipientType.CC);
		recipientTypeMap.put("BCC", RecipientType.BCC);
	}

	public static class Recipient implements ArrayXml.Element<Recipient> {
		private final String name;
		private final String address;
		private final RecipientType type;

		public Recipient(String name, String address, RecipientType type) {
			this.name = name;
			this.address = address;
			this.type = type;
		}

		public Recipient() {
			this(null, null, null);
		}

		public static class Creator extends CreatorByXML<Recipient> {
			private String name;
			private String address;
			private RecipientType type;

			public Creator(String id, BaseCreator<?> creator) {
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
			public Recipient create() throws XmlError, IOException {
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

	public static void send(String subject, String text, String name, String from, CryptAes password, Security security,
			String provider, int port, Collection<Recipient> recipients, MyImage image)
			throws MessagingException, IOException {
		if (logger == null) {
			logger = LogManager.getLogger(Mail.class);
		}

		String portString = Integer.toString(port);

		logger.info(security.name() + "Email Start");
		Properties props = new Properties();
		props.put("mail.smtp.host", provider); // SMTP Host
		if (security == Security.SSL) {
			props.put("mail.smtp.socketFactory.port", portString); // SSL Port
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // SSL Factory Class
		} else {
			props.put("mail.smtp.starttls.enable", "true");
//			props.put("mail.smtp.EnableSSL.enable", "true");
		}
		props.put("mail.smtp.auth", "true"); // Enabling SMTP Authentication
		props.put("mail.smtp.port", portString); // SMTP Port
		props.put("mail.smtp.ssl.trust", "*");

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
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image.getImage(), "png", baos);
			baos.flush();
			byte[] imageBytes = baos.toByteArray();
			baos.close();
			ByteArrayDataSource bds = new ByteArrayDataSource(imageBytes, "image/png");
			messageBodyPart.setDataHandler(new DataHandler(bds));
			messageBodyPart.setFileName("SolvisScreen.png");
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

	public static class ImageDataSource implements DataSource {

		private BufferedImage image;

		@Override
		public String getContentType() {
			return "image/png";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(this.image, "png", os);
			os.flush();
			InputStream is = new ByteArrayInputStream(os.toByteArray());
			return is;
		}

		@Override
		public String getName() {
			return "SolvisScreen.png";
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return null;
		}
	}

}
