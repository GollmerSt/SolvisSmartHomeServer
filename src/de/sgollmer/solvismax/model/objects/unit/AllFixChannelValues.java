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
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class AllFixChannelValues {

	private static final ILogger logger = LogManager.getInstance().getLogger(AllFixChannelValues.class);
	private static final String XML_CHANNEL_VALUE = "ChannelValue";

	private final Collection<ChannelValue> values;

	private AllFixChannelValues(final Collection<ChannelValue> values) {
		this.values = values;
	}

	public void initialize(Solvis solvis) {
		for (ChannelValue channelValue : this.values) {
			channelValue.initialize(solvis);
		}
	}

	public static class ChannelValue {

		private final String id;
		private final double value;

		public ChannelValue(final String id, final double value) {
			this.id = id;
			this.value = value;
		}

		public void initialize(Solvis solvis) {
			SolvisData data = solvis.getAllSolvisData().getByName(this.id);
			if (data == null) {
				logger.error("base.xml error: Fix channel <" + this.id + "> no defined.");
				return;
			}
			DoubleValue doubleValue = new DoubleValue(this.value, -1L);
			try {
				data.setFixData(doubleValue);
				logger.info("The channel <" + this.id + "> was set to the fix value \"" + data.getSingleData().toString()
						+ "\".");
			} catch (TypeException e) {
				logger.error("base.xml error: Fix channel <" + this.id + "> can't be set by the given format.");
			}

		}

		private static class Creator extends CreatorByXML<ChannelValue> {

			private String id;
			private double value;

			public Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) throws XmlException {
				switch (name.getLocalPart()) {
					case "id":
						this.id = value;
						break;
					case "value":
						this.value = Double.parseDouble(value);
						break;
				}

			}

			@Override
			public ChannelValue create() throws XmlException, IOException {
				return new ChannelValue(this.id, this.value);
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

	public static class Creator extends CreatorByXML<AllFixChannelValues> {

		private final Collection<ChannelValue> values = new ArrayList<>();

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) throws XmlException {
		}

		@Override
		public AllFixChannelValues create() throws XmlException, IOException {
			return new AllFixChannelValues(this.values);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_CHANNEL_VALUE:
					return new ChannelValue.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_CHANNEL_VALUE:
					this.values.add((ChannelValue) created);
					break;
			}
		}

	}
}
