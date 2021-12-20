/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Version {

	private String serverVersion = "01.05.01-rc5";
	private String appendix = "3 heating circuits beta";

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
		return "03.00";
	}

	public String getMqttFormatVersion() {
		return "01.00";
	}

	private String buildDate = null;

	public String getBuildDate() {
		if (this.buildDate == null) {
			String javaCommand = System.getProperty(Constants.SUN_JAVA_COMMAND);
			int jarIdx = javaCommand.indexOf(".jar");
			if (jarIdx < 0) {
				this.buildDate = null;
			} else {
				String main = javaCommand.substring(0, jarIdx + 4);
				File file = new File(main);
				try {
					JarFile jar = new JarFile(file);
					Manifest manifest = jar.getManifest();
					Attributes attr = manifest.getMainAttributes();
					this.buildDate = attr.getValue("Built-Date");
					jar.close();
				} catch (IOException e) {
					this.buildDate = e.getMessage();
				}
			}
		}
		return this.buildDate;
	}

}
