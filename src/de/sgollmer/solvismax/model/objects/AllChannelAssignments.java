package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class AllChannelAssignments {

	private static final String XML_CHANNEL_ASSIGNMENT = "Assignment";

	private final Map<String, OfConfigs<ChannelAssignment>> configurations;

	private AllChannelAssignments(Map<String, OfConfigs<ChannelAssignment>> configurations) {
		this.configurations = configurations;
	}

	public static class Creator extends CreatorByXML<AllChannelAssignments> {

		private final Map<String, OfConfigs<ChannelAssignment>> configurations = new HashMap<>();

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) throws XmlException {

		}

		@Override
		public AllChannelAssignments create() throws XmlException, IOException {
			return new AllChannelAssignments(this.configurations);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();

			switch (id) {
				case XML_CHANNEL_ASSIGNMENT:
					return new ChannelAssignment.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_CHANNEL_ASSIGNMENT:
					ChannelAssignment assignment = (ChannelAssignment) created;
					String name = assignment.getName();
					OfConfigs<ChannelAssignment> config = this.configurations.get(name);

					if (config == null) {
						config = new OfConfigs<>();
						this.configurations.put(name, config);
					}

					config.verifyAndAdd(assignment);
			}

		}

	}

	public ChannelAssignment get(final String id, final Solvis solvis) {
		ChannelAssignment result = null;
		OfConfigs<ChannelAssignment> assignments = this.configurations.get(id);
		if (assignments != null) {
			result = assignments.get(solvis);
		}
		return result == null ? new ChannelAssignment(id, id, null, null, null, null) : result;
	}
}
