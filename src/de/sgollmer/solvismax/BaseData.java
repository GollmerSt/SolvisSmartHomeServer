/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.mail.ExceptionMail;
import de.sgollmer.solvismax.model.objects.unit.Units;
import de.sgollmer.solvismax.smarthome.IoBroker;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class BaseData {

	private static final String XML_UNITS = "Units";
	private static final String XML_EXECUTION_DATA = "ExecutionData";
	private static final String XML_MAIL = "ExceptionMail";
	private static final String XML_MQTT = "Mqtt";
	private static final String XML_IOBROKER = "Iobroker";

	public static boolean DEBUG = false;

	private final String timeZone;

	int getPort() {
		return this.port;
	}

	public String getWritablePath() {
		boolean windows = System.getProperty("os.name").startsWith("Windows");
		return windows ? this.writeablePathWindows : this.writablePathLinux;

	}

	private final int port;
	private final String writeablePathWindows;
	private final String writablePathLinux;
	private final int echoInhibitTime_ms;
	private final Units units;
	private final ExceptionMail exceptionMail;
	private final Mqtt mqtt;
	private final IoBroker ioBroker;

	public String getTimeZone() {
		return this.timeZone;
	}

	private BaseData(final String timeZone, final int port, final String writeablePathWindows,
			final String writablePathLinux, final int echoInhibitTime_ms, final Units units,
			final ExceptionMail exceptionMail, final Mqtt mqtt, final IoBroker ioBroker) {
		this.timeZone = timeZone;
		this.port = port;
		this.writeablePathWindows = writeablePathWindows;
		this.writablePathLinux = writablePathLinux;
		this.units = units;
		this.exceptionMail = exceptionMail;
		this.echoInhibitTime_ms = echoInhibitTime_ms;
		this.mqtt = mqtt;
		this.ioBroker = ioBroker;

	}

	public Units getUnits() {
		return this.units;
	}

	public ExceptionMail getExceptionMail() {
		return this.exceptionMail;
	}

	public int getEchoInhibitTime_ms() {
		return this.echoInhibitTime_ms;
	}

	public Mqtt getMqtt() {
		return this.mqtt;
	}

	public IoBroker getIoBroker() {
		return this.ioBroker;
	}

	public static class Creator extends BaseCreator<BaseData> {

		private Units units;
		private ExecutionData executionData;
		private ExceptionMail exceptionMail;
		private Mqtt mqtt = null;
		private IoBroker ioBroker = new IoBroker();

		public Creator(final String id) {
			super(id);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "DEBUG":
					DEBUG = Boolean.parseBoolean(value);
					break;
			}
		}

		@Override
		public BaseData create() throws XmlException, IOException {
			BaseData baseData = new BaseData(this.executionData.timeZone, this.executionData.port,
					this.executionData.writeablePathWindows, this.executionData.writablePathLinux,
					this.executionData.echoInhibitTime_ms, this.units, this.exceptionMail, this.mqtt, this.ioBroker);
			this.ioBroker.setBaseData(baseData);
			return baseData;
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_UNITS:
					return new Units.Creator(id, this.getBaseCreator());
				case XML_EXECUTION_DATA:
					return new ExecutionData.Creator(id, getBaseCreator());
				case XML_MAIL:
					return new ExceptionMail.Creator(id, getBaseCreator());
				case XML_MQTT:
					return new Mqtt.Creator(id, getBaseCreator());
				case XML_IOBROKER:
					return new IoBroker.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_UNITS:
					this.units = (Units) created;
					break;
				case XML_EXECUTION_DATA:
					this.executionData = (ExecutionData) created;
					break;
				case XML_MAIL:
					this.exceptionMail = (ExceptionMail) created;
					break;
				case XML_MQTT:
					this.mqtt = (Mqtt) created;
					break;
				case XML_IOBROKER:
					this.ioBroker = (IoBroker) created;
			}

		}

	}

	private static class ExecutionData {

		private final String timeZone;
		private final int port;
		private final String writeablePathWindows;
		private final String writablePathLinux;
		private final int echoInhibitTime_ms;

		private ExecutionData(final String timeZone, final int port, final String writeablePathWindows,
				final String writablePathLinux, final int echoInhibitTime_ms) {
			this.timeZone = timeZone;
			this.port = port;
			this.writeablePathWindows = writeablePathWindows;
			this.writablePathLinux = writablePathLinux;
			this.echoInhibitTime_ms = echoInhibitTime_ms;
		}

		private static class Creator extends CreatorByXML<ExecutionData> {

			private String timeZone;
			private int port;
			private String writeablePathWindows;
			private String writablePathLinux;
			private int echoInhibitTime_ms;

			private Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) {
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
					case "echoInhibitTime_ms":
						this.echoInhibitTime_ms = Integer.parseInt(value);
						break;
				}

			}

			@Override
			public ExecutionData create() throws XmlException, IOException {
				return new ExecutionData(this.timeZone, this.port, this.writeablePathWindows, this.writablePathLinux,
						this.echoInhibitTime_ms);
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
				return null;
			}

			@Override
			public void created(final CreatorByXML<?> creator, final Object created) {

			}
		}
	}
}
