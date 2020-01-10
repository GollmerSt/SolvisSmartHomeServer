/************************************************************************
 * 
 * $Id: BaseData.java 81 2020-01-04 21:05:15Z stefa $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.Units;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class BaseData {

	private static final String XML_UNITS = "Units";
	private static final String XML_EXECUTION_DATA = "ExecutionData";

	private final String timeZone;

	public int getPort() {
		return port;
	}

	public String getWritablePath() {
		boolean windows = System.getProperty("os.name").startsWith("Windows");
		return windows ? this.writeablePathWindows : this.writablePathLinux;

	}

	private final int port;
	private final String writeablePathWindows;
	private final String writablePathLinux;

	public String getTimeZone() {
		return timeZone;
	}

	private final Units units;

	public BaseData(String timeZone, int port, String writeablePathWindows, String writablePathLinux, Units units) {
		this.timeZone = timeZone;
		this.port = port;
		this.writeablePathWindows = writeablePathWindows;
		this.writablePathLinux = writablePathLinux;
		this.units = units;
	}

	public Units getUnits() {
		return units;
	}

	public static class Creator extends BaseCreator<BaseData> {

		private Units units;
		private ExecutionData executionData;

		public Creator(String id) {
			super(id);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public BaseData create() throws XmlError, IOException {
			return new BaseData(executionData.timeZone, executionData.port, executionData.writeablePathWindows,
					executionData.writablePathLinux, units);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_UNITS:
					return new Units.Creator(id, this.getBaseCreator());
				case XML_EXECUTION_DATA:
					return new ExecutionData.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_UNITS:
					this.units = (Units) created;
					break;
				case XML_EXECUTION_DATA:
					this.executionData = (ExecutionData) created;
					break;

			}

		}

	}

	private static class ExecutionData {

		private final String timeZone;
		private final int port;
		private final String writeablePathWindows;
		private final String writablePathLinux;

		public ExecutionData(String timeZone, int port, String writeablePathWindows, String writablePathLinux) {
			this.timeZone = timeZone;
			this.port = port;
			this.writeablePathWindows = writeablePathWindows;
			this.writablePathLinux = writablePathLinux;
		}

		public static class Creator extends CreatorByXML<ExecutionData> {

			private String timeZone;
			private int port;
			private String writeablePathWindows;
			private String writablePathLinux;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				switch (name.getLocalPart()) {
					case "timeZone":
						this.timeZone = value;
						break;
					case "port":
						this.port = Integer.parseInt(value);
						break;
					case "writeablePathWindows":
						this.writeablePathWindows = value;
						break;
					case "writablePathLinux":
						this.writablePathLinux = value;
						break;
				}

			}

			@Override
			public ExecutionData create() throws XmlError, IOException {
				return new ExecutionData(timeZone, port, writeablePathWindows, writablePathLinux);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {

			}
		}
	}
}
