package de.sgollmer.solvismax.mail;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.crypt.CryptAes;
import de.sgollmer.solvismax.error.CryptDefaultValueException;
import de.sgollmer.solvismax.error.CryptExeception;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Proxy {

	private static final ILogger logger = LogManager.getInstance().getLogger(Proxy.class);;

	private final String host;
	private final int port;
	private final String user;
	private final CryptAes password;

	private Proxy(final String host, int port, final String user, final CryptAes password) {
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

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
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
						logger.log(level, m);
					}
					break;
			}
		}

		@Override
		public Proxy create() throws XmlException, IOException {
			return new Proxy(this.host, this.port, this.user, this.password);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {

		}

	}
}
