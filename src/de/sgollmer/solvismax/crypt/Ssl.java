/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.crypt;

import java.io.IOException;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.namespace.QName;

import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Ssl {

	private final boolean enable;
	private final String caFilePath;// = "/your_ssl/cacert.pem";
	private final String clientCrtFilePath;// = "/your_ssl/client.pem";
	private final String clientKeyFilePath;// = "/your_ssl/client.key";

	private Ssl(final boolean enable, final String caFilePath, final String clientCrtFilePath,
			final String clientKeyFilePath) {
		this.enable = enable;
		this.caFilePath = caFilePath;
		this.clientCrtFilePath = clientCrtFilePath;
		this.clientKeyFilePath = clientKeyFilePath;
	}

	public static class Creator extends CreatorByXML<Ssl> {

		private boolean enable;
		private String caFilePath;// = "/your_ssl/cacert.pem";
		private String clientCrtFilePath;// = "/your_ssl/client.pem";
		private String clientKeyFilePath;// = "/your_ssl/client.key";

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "enable":
					this.enable = Boolean.parseBoolean(value);
					break;
				case "caFilePath":
					this.caFilePath = value;
					break;
				case "clientCrtFilePath":
					this.clientCrtFilePath = value;
					break;
				case "clientKeyFilePath":
					this.clientKeyFilePath = value;
					break;
			}

		}

		@Override
		public Ssl create() throws XmlException, IOException {
			return new Ssl(this.enable, this.caFilePath, this.clientCrtFilePath, this.clientKeyFilePath);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {

		}

	}

	@SuppressWarnings("unused")
	public SSLSocketFactory getSocketFactory() {
		if (!this.enable) {
			return null;
		}
		String temp = this.caFilePath;
		temp = this.clientCrtFilePath;
		temp = this.clientKeyFilePath;
		// TODO not yet implemented
		return null;
	}
}
