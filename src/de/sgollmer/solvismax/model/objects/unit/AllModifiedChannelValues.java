package de.sgollmer.solvismax.model.objects.unit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class AllModifiedChannelValues {

	private static final ILogger logger = LogManager.getInstance().getLogger(AllModifiedChannelValues.class);
	private static final String XML_CHANNEL_VALUE = "ChannelValue";

	private final Collection<ChannelValue> values;

	private AllModifiedChannelValues(final Collection<ChannelValue> values) {
		this.values = values;
	}

	public void initialize(Solvis solvis) {
		for (ChannelValue channelValue : this.values) {
			channelValue.initialize(solvis);
		}
	}

	public enum ModifyType {
		FIX, ADD, MULT;
	}

	public static class ChannelValue {

		private final String id;
		private final ModifyType type;
		private final double value;

		public ChannelValue(final String id, final ModifyType type, final double value) {
			this.id = id;
			this.type = type;
			this.value = value;
		}

		public void initialize(Solvis solvis) {
			SolvisData data = solvis.getAllSolvisData().getByName(this.id);
			if (data == null) {
				logger.error("base.xml error: Fix channel <" + this.id + "> no defined.");
				return;
			}
			try {
				data.setMofifiedChannelValue(this);
			} catch (TypeException e) {
				logger.error("base.xml error: Fix channel <" + this.id + "> can't be set by the given format.");
			}

		}

		public void modify(SolvisData data) {
			if (this.type == ModifyType.FIX) {
				return; // Modified while init phase
			}
			if (data == null) {
				logger.error("Program or base.xml error, data of <" + this.id + "> not defined");
				return;
			}

		}

		private static class Creator extends CreatorByXML<ChannelValue> {

			private String id;
			private ModifyType type;
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
					case "type":
						this.type = ModifyType.valueOf(value.toUpperCase());
						break;
					case "value":
						this.value = Double.parseDouble(value);
						break;
				}

			}

			@Override
			public ChannelValue create() throws XmlException, IOException {
				return new ChannelValue(this.id, this.type, this.value);
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

	}

	public static class Creator extends CreatorByXML<AllModifiedChannelValues> {

		private final Collection<ChannelValue> values = new ArrayList<>();

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) throws XmlException {
		}

		@Override
		public AllModifiedChannelValues create() throws XmlException, IOException {
			return new AllModifiedChannelValues(this.values);
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
