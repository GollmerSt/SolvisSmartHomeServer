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

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Ssl {

	private final boolean enable;
	private final String caFilePath;// = "/your_ssl/cacert.pem";
	private final String clientCrtFilePath;// = "/your_ssl/client.pem";
	private final String clientKeyFilePath;// = "/your_ssl/client.key";

	private Ssl(boolean enable, String caFilePath, String clientCrtFilePath, String clientKeyFilePath) {
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

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
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
		public Ssl create() throws XmlError, IOException {
			return new Ssl(this.enable, this.caFilePath, this.clientCrtFilePath, this.clientKeyFilePath);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {

		}

	}

	@SuppressWarnings("unused")
	public SSLSocketFactory getSocketFactory() {
		if (!this.enable) {
			return null;
		}
		String caFilePath = this.caFilePath;
		String clientCrtFilePath = this.clientCrtFilePath;
		String clientKeyFilePath = this.clientKeyFilePath;
		;
		// TODO not yet implemented
		return null;
	}
}
