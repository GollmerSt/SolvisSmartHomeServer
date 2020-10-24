/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.mail;

import java.io.IOException;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.xml.namespace.QName;

import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.error.CryptDefaultValueException;
import de.sgollmer.solvismax.error.CryptExeception;
import de.sgollmer.solvismax.error.ObserverException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.DelayedMessage;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.mail.Mail.Recipient;
import de.sgollmer.solvismax.mail.Mail.Security;
import de.sgollmer.solvismax.model.SolvisState.SolvisErrorInfo;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.xmllibrary.ArrayXml;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;

public class ExceptionMail implements IObserver< SolvisErrorInfo> {

	private static final ILogger logger = LogManager.getInstance().getLogger(ExceptionMail.class);;

	private static final String XML_RECIPIENT = "Recipient";
	private static final String XML_RECIPIENTS = "Recipients";
	private static final String XMl_PROXY = "Proxy";

	private final String name;
	private final String from;
	private final CryptAes password;
	private final Security securityType;
	private final String provider;
	private final int port;
	private final Collection<Recipient> recipients;
	private final Proxy proxy;

	private ExceptionMail(String name, String from, CryptAes password, Security securityType, String provider, int port,
			Collection<Recipient> recipients, Proxy proxy) {
		this.name = name;
		this.from = from;
		this.password = password;
		this.securityType = securityType;
		this.provider = provider;
		this.port = port;
		this.recipients = recipients;
		this.proxy = proxy;
	}

	public static class Creator extends CreatorByXML<ExceptionMail> {

		private String name;
		private String from;
		private CryptAes password = new CryptAes();
		private Security securityType;
		private String provider;
		private int port;
		private Collection<Recipient> recipients;
		private Proxy proxy = null ;
		private boolean valid = true;

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
					case "passwordCrypt":
						this.password.decrypt(value);
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
			} catch (CryptDefaultValueException | CryptExeception e) {
				this.valid = false;
				String m = "base.xml error of passwordCrypt in Mail tag, mail disabled: " + e.getMessage();
				Level level = Level.ERROR;
				if ( e instanceof CryptDefaultValueException ) {
					level = Level.WARN;
				}
				LogManager.getInstance()
						.addDelayedErrorMessage(new DelayedMessage(level, m, ExceptionMail.class, null));
			}
		}

		@Override
		public ExceptionMail create() throws IOException {
			if (this.valid) {
				return new ExceptionMail(this.name, this.from, this.password, this.securityType, this.provider,
						this.port, this.recipients, this.proxy);
			} else {
				return null;
			}
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_RECIPIENTS:
					return new ArrayXml.Creator<Recipient>(id, this.getBaseCreator(), new Recipient(), XML_RECIPIENT);
				case XMl_PROXY:
					return new Proxy.Creator(id, this.getBaseCreator());
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
				case XMl_PROXY:
					this.proxy = (Proxy) created;
					break;
			}
		}
	}

	public void send(String subject, String text, MyImage image) throws MessagingException, IOException {
		Mail.send(subject, text, this.name, this.from, this.password, this.securityType, this.provider, this.port,
				this.recipients, image, this.proxy);
	}

//	public void send(SolvisErrorInfo info) throws MessagingException, IOException {
//		this.send(info.getMessage(), "", info.getImage());
//	}
//
	@Override
	public void update(SolvisErrorInfo info, Object source) {
		try {
			this.send(info.getMessage(), "", info.getImage());
		} catch (MessagingException | IOException e) {
			logger.error("Mail <" + info.getMessage() + "> couldn't be sent: ", e);
			throw new ObserverException();
		}
		
	}
}
