package de.sgollmer.solvismax.model.objects.unit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

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

	public static class ChannelOption {

		private final String id;
		private final Double fix;
		private final Double factor;
		private final Double offset;
		private final int powerOnDelay;

		public ChannelOption(final String id, final Double fix, final Double factor, final Double offset,
				final Double value, final int powerOnDelay) {
			this.id = id;
			this.fix = fix;
			this.factor = factor;
			this.offset = offset;
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

		public SingleData<?> modify(final SolvisData data) throws TypeException {
			if (this.offset == null && this.factor == null) {
				return data.getSingleData();
			}

			SingleData<?> modified = data.getSingleData();

			if (this.factor != null) {
				modified = modified.mult(getModifyValue(this.factor, data));
			}

			if (this.offset != null) {
				modified = modified.add(getModifyValue(this.offset, data));
			}

			return modified;
		}

		private SingleData<?> getModifyValue(final Double value, final SolvisData data) throws TypeException {
			if (value == null ) {
				return null;
			}
			DoubleValue doubleValue = new DoubleValue(value, -1L);
			return data.getDescription().interpretSetData(doubleValue, true);
		}

		private static class Creator extends CreatorByXML<ChannelOption> {

			private String id;
			private Double fix = null;
			private Double factor = null;
			private Double offset = null;
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
					case "fix":
						this.fix = Double.parseDouble(value);
						break;
					case "factor":
						this.factor = Double.parseDouble(value);
						break;
					case "offset":
						this.offset = Double.parseDouble(value);
						break;
					case "powerOnDelay_s":
						this.powerOnDelay = Integer.parseInt(value) * 1000;
						break;
				}

			}

			@Override
			public ChannelOption create() throws XmlException, IOException {
				return new ChannelOption(this.id, this.fix, this.factor, this.offset, this.factor, this.powerOnDelay);
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
				return null;
			}

			@Override
			public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {
			}

		}

		public SingleData<?> getFixValue(final SolvisData data) throws TypeException {
			return this.getModifyValue(this.fix, data);
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
