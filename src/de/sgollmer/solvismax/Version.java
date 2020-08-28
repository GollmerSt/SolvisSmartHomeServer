/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Version {

	private String serverVersion = "01.02.03.rc5";
	private String appendix = "MQTT beta, 3 heating circuits beta";

	public static Version getInstance() {
		Version version = VersionHolder.INSTANCE;
		return version;
	}

	private static class VersionHolder {

		private static final Version INSTANCE = new Version();
	}

	public String getVersion() {
		if (this.appendix != null) {
			return this.serverVersion + ", " + this.appendix;
		} else {
			return this.serverVersion;
		}
	}

	public String getServerFormatVersion() {
		return "01.02";
	}

	public String getMqttFormatVersion() {
		return "01.00";
	}

	private String buildDate = null;

	public String getBuildDate() {
		if (this.buildDate == null) {
			try {
				InputStream inputStream = Main.class.getResourceAsStream("/META-INF/MANIFEST.MF");
				Manifest manifest = new Manifest(inputStream);
				Attributes attr = manifest.getMainAttributes();
				this.buildDate = attr.getValue("Built-Date");
			} catch (IOException e) {
				this.buildDate = null;
			}
		}
		return this.buildDate;
	}

}
