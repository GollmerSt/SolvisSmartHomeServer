package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.data.StringData;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Standby {

	private static final ILogger logger = LogManager.getInstance().getLogger(Standby.class);

	private static final String XML_STANDBY_CHANNEL = "Channel";

	private final Map<String, String> standbyChannels;

	private Standby(Map<String, String> standbyChannel) {
		this.standbyChannels = standbyChannel;
	}

	public Executable instantiate(Solvis solvis) throws TypeException {
		return new Executable(solvis);
	}

	public static class Creator extends CreatorByXML<Standby> {

		private final Map<String, String> standbyChannels = new HashMap<>(4);

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) throws XmlException {

		}

		@Override
		public Standby create() throws XmlException, IOException {
			return new Standby(this.standbyChannels);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {

			String id = name.getLocalPart();
			switch (id) {
				case XML_STANDBY_CHANNEL:
					return new StandbyChannelCreator(id, getBaseCreator(), this.standbyChannels);
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
		}

	}

	public static class StandbyChannel {
		private final Solvis solvis;
		private final SolvisData solvisData;
		private final SingleData<?> standbyState;
		private SingleData<?> savedState = null;

		public StandbyChannel(Solvis solvis, SolvisData solvisData, SingleData<?> standbyState) {
			this.solvis = solvis;
			this.solvisData = solvisData;
			this.standbyState = standbyState;
		}

		public boolean set(boolean standby)
				throws NumberFormatException, IOException, PowerOnException, TerminationException {
			ChannelDescription description = this.solvisData.getDescription();
			SingleData<?> state = null;
			if (standby) {
				SingleData<?> currentState = this.solvisData.getSingleData();
				if (this.savedState != null || this.standbyState.equals(currentState) && this.solvisData.isValid()) {
					return false;
				}
				description.getValue(this.solvis);
				this.savedState = this.solvisData.getSingleData();
				state = this.standbyState;
			} else {
				if (this.savedState == null) {
					return false;
				} else {
					state = this.savedState;
					this.savedState = null;
				}
			}
			ResultStatus status = ResultStatus.CONTINUE;

			while (state != null && status == ResultStatus.CONTINUE) {
				status = description.setValue(this.solvis, new SolvisData(state)).getStatus();
			}

			if (!standby && status != ResultStatus.NO_SUCCESS) {
				this.savedState = null;
			}

			return true;

		}

	}

	public class Executable {

		private final Map<ChannelDescription, StandbyChannel> standbyChannels = new HashMap<>(4);

		private Executable(Solvis solvis) throws TypeException {
			for (Map.Entry<String, String> entry : Standby.this.standbyChannels.entrySet()) {
				ChannelDescription description = solvis.getChannelDescription(entry.getKey());
				if (description != null) {
					SingleData<?> data = description.interpretSetData(new StringData(entry.getValue(), -1));
					SolvisData solvisData = solvis.getAllSolvisData().get(description);
					StandbyChannel channel = new StandbyChannel(solvis, solvisData, data);
					this.standbyChannels.put(description, channel);
				}
			}
		}

		public boolean set(SolvisData data)
				throws NumberFormatException, IOException, PowerOnException, TerminationException {
			StandbyChannel channel = this.standbyChannels.get(data.getDescription());
			if (channel == null) {
				logger.error("Standby channel <" + data.getDescription() + "> not defined. Setting ignored.");
				return false;
			} else {
				return channel.set(true);
			}
		}

		public void reset() throws NumberFormatException, IOException, PowerOnException, TerminationException {
			for (StandbyChannel channel : this.standbyChannels.values()) {
				channel.set(false);
			}
		}

	}

	private static class StandbyChannelCreator extends CreatorByXML<Object> {

		private final Map<String, String> standbyChannels;
		private String id = null;
		private String value = null;

		public StandbyChannelCreator(String id, BaseCreator<?> creator, Map<String, String> standbyChannel) {
			super(id, creator);
			this.standbyChannels = standbyChannel;
		}

		@Override
		public void setAttribute(QName name, String value) throws XmlException {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "value":
					this.value = value;
					break;
			}

		}

		@Override
		public Object create() throws XmlException, IOException {
			String former = this.standbyChannels.put(this.id, this.value);
			if (former != null) {
				logger.warn("Standby channel <" + this.id + "> is not unique.");
			}
			return null;
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
