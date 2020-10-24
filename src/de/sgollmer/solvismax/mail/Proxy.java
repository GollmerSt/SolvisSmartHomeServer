package de.sgollmer.solvismax.mail;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.error.CryptDefaultValueException;
import de.sgollmer.solvismax.error.CryptExeception;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.DelayedMessage;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Proxy {

	private final String host;
	private final int port;
	private final String user;
	private final CryptAes password;

	public Proxy(String host, int port, String user, CryptAes password) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
	}

	public String getHost() {
		return this.host;
	}

	public int getPort() {
		return this.port;
	}

	public String getUser() {
		return this.user;
	}

	public CryptAes getPassword() {
		return this.password;
	}

	public static class Creator extends CreatorByXML<Proxy> {

		private String host;
		private int port;
		private String user = null;
		private CryptAes password = null;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "host":
					this.host = value;
					break;
				case "port":
					this.port = Integer.parseInt(value);
					break;
				case "user":
					this.user = value;
					break;
				case "passwordCrypt":
					this.password = new CryptAes();
					try {
						this.password.decrypt(value);
					} catch (CryptDefaultValueException | CryptExeception e) {
						this.password = null;
						String m = "base.xml error of passwordCrypt in proxy tag, mail password not used";
						Level level = Level.ERROR;
						if (e instanceof CryptDefaultValueException) {
							level = Level.WARN;
						}
						LogManager.getInstance()
								.addDelayedErrorMessage(new DelayedMessage(level, m, ExceptionMail.class, null));
					}
					break;
			}
		}

		@Override
		public Proxy create() throws XmlException, IOException {
			return new Proxy(this.host, this.port, this.user, this.password);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {

		}

	}
}
