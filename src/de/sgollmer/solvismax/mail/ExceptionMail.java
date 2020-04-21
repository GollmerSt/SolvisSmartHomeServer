package de.sgollmer.solvismax.mail;

import java.io.IOException;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.xml.namespace.QName;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.Logger2;
import de.sgollmer.solvismax.log.Logger2.DelayedMessage;
import de.sgollmer.solvismax.mail.Mail.Recipient;
import de.sgollmer.solvismax.mail.Mail.Security;
import de.sgollmer.solvismax.model.SolvisState;
import de.sgollmer.solvismax.model.SolvisState.State;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.Units.Unit;
import de.sgollmer.solvismax.xml.ArrayXml;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ExceptionMail implements ObserverI<SolvisState> {

	private static Logger logger = null;

	private static final String XML_RECIPIENT = "Recipient";
	private static final String XML_RECIPIENTS = "Recipients";

	private final String name;
	private final String from;
	private final CryptAes password;
	private final Security securityType;
	private final String provider;
	private final int port;
	private final Collection<Recipient> recipients;

	public ExceptionMail(String name, String from, CryptAes password, Security securityType, String provider, int port,
			Collection<Recipient> recipients) {
		this.name = name;
		this.from = from;
		this.password = password;
		this.securityType = securityType;
		this.provider = provider;
		this.port = port;
		this.recipients = recipients;
	}

	public static class Creator extends CreatorByXML<ExceptionMail> {

		private String name;
		private String from;
		private CryptAes password = new CryptAes();
		private Security securityType;
		private String provider;
		private int port;
		private Collection<Recipient> recipients;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			try {
				switch (name.getLocalPart()) {
					case "name":
						this.name = value;
						break;
					case "from":
						this.from = value;
						break;
					case "password":
						try {
							this.password.decrypt(value);
						} catch (Throwable e) {
							throw new Error("Decrypt error", e);
						}
						break;
					case "securityType":
						try {
							this.securityType = Security.valueOf(Security.class, value.toUpperCase());
						} catch (IllegalArgumentException e) {
							throw new Error("Security type error", e);
						}
						break;
					case "provider":
						this.provider = value;
						break;
					case "port":
						this.port = Integer.parseInt(value);
						break;
				}
			} catch (Throwable e) {
				String m = "base.vml error: " + e.getMessage();
				Logger2.addDelayedErrorMessage(
						new DelayedMessage(Level.ERROR, m, Unit.class, Constants.ExitCodes.CRYPTION_FAIL));
				System.err.println(m);
			}
		}

		@Override
		public ExceptionMail create() throws XmlError, IOException {
			return new ExceptionMail(this.name, this.from, this.password, this.securityType, this.provider, this.port,
					this.recipients);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_RECIPIENTS:
					return new ArrayXml.Creator<Recipient>(id, this.getBaseCreator(), new Recipient(), XML_RECIPIENT);
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_RECIPIENTS:
					this.recipients = ((ArrayXml<Recipient>) created).getArray();
					break;
			}
		}
	}

	public void send(String subject, String text, MyImage image) throws MessagingException, IOException {
		if (logger == null) {
			logger = LogManager.getLogger(ExceptionMail.class);
		}
		Mail.send(subject, text, this.name, this.from, this.password, this.securityType, this.provider, this.port,
				this.recipients, image);
	}

	@Override
	public void update(SolvisState data, Object source) {
		
		if (data.getState() == State.ERROR) {
			try {
				this.send(data.getLastMessage(), "", data.getErrorScreen());
			} catch (Throwable e) {
				logger.error("Error occured on sending message.", e);
			}
		}

	}
}
