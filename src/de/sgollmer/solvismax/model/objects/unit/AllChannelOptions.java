package de.sgollmer.solvismax.model.objects.unit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.FatalError;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.DoubleValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class AllChannelOptions {

	private static final ILogger logger = LogManager.getInstance().getLogger(AllChannelOptions.class);
	private static final String XML_CHANNEL_VALUE = "Channel";

	private final Collection<ChannelOption> values;

	private AllChannelOptions(final Collection<ChannelOption> values) {
		this.values = values;
	}

	public void initialize(Solvis solvis) {
		for (ChannelOption channelValue : this.values) {
			channelValue.initialize(solvis);
		}
	}

	public void setFixValues(Solvis solvis) {
		for (ChannelOption channelValue : this.values) {
			channelValue.setFixValues(solvis);
		}
	}

	public enum ModifyType {
		FIX, ADD, MULT;
	}

	public static class ChannelOption {

		private final String id;
		private final ModifyType type;
		private final Double value;
		private final int powerOnDelay;

		public ChannelOption(final String id, final ModifyType type, final Double value, final int powerOnDelay) {
			this.id = id;
			this.type = type;
			this.value = value;
			this.powerOnDelay = powerOnDelay;
		}

		public void initialize(Solvis solvis) {
			SolvisData data = solvis.getAllSolvisData().getByName(this.id);
			if (data == null) {
				logger.error("base.xml error: Channel <" + this.id + "> no defined.");
				return;
			}
			try {
				data.setChannelOption(this);
			} catch (TypeException e) {
				logger.error("base.xml error: Channel <" + this.id + "> can't be set by the given format.");
			}

		}

		public void setFixValues(Solvis solvis) {
			SolvisData data = solvis.getAllSolvisData().getByName(this.id);
			if (data == null) {
				logger.error("base.xml error: Channel <" + this.id + "> no defined.");
				return;
			}
			try {
				data.setFixedChannelValue(this);
			} catch (TypeException e) {
				logger.error("base.xml error: Channel <" + this.id + "> can't be set by the given format.");
			}

		}

		public SingleData<?> modify(SolvisData data) throws TypeException {
			if (this.value == null || this.type == null) {
				return data.getSingleData();
			}

			DoubleValue doubleValue = new DoubleValue(this.getValue(), -1L);
			SingleData<?> modify = data.getDescription().interpretSetData(doubleValue, true);
			switch (this.type) {
				case ADD:
					return data.getSingleData().add(modify);
				case MULT:
					return data.getSingleData().mult(modify);
				case FIX:
					return data.getSingleData();
				default:
					throw new FatalError("Channel value type <" + this.type.name() + "> not supported");
			}

		}

		private static class Creator extends CreatorByXML<ChannelOption> {

			private String id;
			private ModifyType type = null;
			private Double value = null;
			private int powerOnDelay = -1;

			public Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) throws XmlException {
				switch (name.getLocalPart()) {
					case "id":
						this.id = value;
						break;
					case "type":
						this.type = ModifyType.valueOf(value.toUpperCase());
						break;
					case "value":
						this.value = Double.parseDouble(value);
						break;
					case "powerOnDelay_s":
						this.powerOnDelay = Integer.parseInt(value) * 1000;
						break;
				}

			}

			@Override
			public ChannelOption create() throws XmlException, IOException {
				if ((this.type != null) != (this.value != null)) {
					throw new XmlException(
							"Channel option of channel <" + this.id + "> not complete. Type or value missing.");
				}
				return new ChannelOption(this.id, this.type, this.value, this.powerOnDelay);
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
				return null;
			}

			@Override
			public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {
			}

		}

		public double getValue() {
			return this.value;
		}

		public ModifyType getType() {
			return this.type;
		}

		public int getPowerOnDelay() {
			return this.powerOnDelay;
		}

	}

	public static class Creator extends CreatorByXML<AllChannelOptions> {

		private final Collection<ChannelOption> values = new ArrayList<>();

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) throws XmlException {
		}

		@Override
		public AllChannelOptions create() throws XmlException, IOException {
			return new AllChannelOptions(this.values);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_CHANNEL_VALUE:
					return new ChannelOption.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_CHANNEL_VALUE:
					this.values.add((ChannelOption) created);
					break;
			}
		}

	}
}
