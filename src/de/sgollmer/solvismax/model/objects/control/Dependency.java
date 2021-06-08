package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Dependency implements IAssigner {
	private final String id;
	private final String value;
	private final Integer priority;
	private final String standbyId;

	private OfConfigs<ChannelDescription> channel = null;

	protected ChannelDescription description;

	private Dependency(final String id, final String value, final Integer priority, final String standbyId) {
		this.id = id;
		this.value = value;
		this.priority = priority;
		this.standbyId = standbyId;
	}

	public static class Creator extends CreatorByXML<Dependency> {

		private String id;
		private String value;
		private Integer priority = null;
		private String standbyId = null;

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "value":
					this.value = value;
					break;
				case "priority":
					this.priority = Integer.parseInt(value);
					break;
				case "standby":
					this.standbyId = value;
					break;
			}

		}

		@Override
		public Dependency create() throws XmlException, IOException {
			return new Dependency(this.id, this.value, this.priority, this.standbyId);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {

		}

	}

	public ChannelDescription getChannelDescription(final Solvis solvis) {
		return solvis.getChannelDescription(this.id);
	}

	public SingleData<?> getData(final Solvis solvis) {
		ChannelDescription description = this.getChannelDescription(solvis);
		try {
			return description.interpretSetData(new StringData(this.value, -1L), false);
		} catch (TypeException e) {
		}
		return null;
	}

	@Override
	public void assign(final SolvisDescription description)
			throws XmlException, AssignmentException, ReferenceException {
		this.channel = description.getChannelDescriptions().get(this.id);
		if (this.channel == null) {
			throw new ReferenceException("Channel < " + this.id + " > not found");
		}

		for (ChannelDescription cd : this.channel) {
			if (this.value != null) {
				try {
					if (!cd.isWriteable()) {
						throw new XmlException("Error in control.xml, invalid dependency channel <" + cd.getId()
								+ ">. Channel isn' writable.");
					}
					cd.interpretSetData(new StringData(this.value, -1L), false);
				} catch (TypeException e) {
					throw new XmlException("Error in control.xml, invalid dependency value <" + this.value
							+ "> defined for channel <" + cd.getId() + ">.");
				}
			}
		}

	}

	public static boolean equals(final Dependency d1, final Dependency d2, final Solvis solvis) {

		Helper.Boolean equals = Helper.checkNull(d1, d2);
		if (equals != Helper.Boolean.UNDEFINED) {
			return equals.result();
		}
		if (d1.getChannelDescription(solvis) != d2.getChannelDescription(solvis)) {
			return false;
		}
		SingleData<?> dd1 = d1.getData(solvis);
		SingleData<?> dd2 = d2.getData(solvis);

		return Helper.equals(dd1, dd2);

	}

	public int compareTo(final Dependency o, final Solvis solvis) {
		if (o == null) {
			return 1;
		}
		String id1 = this.getChannelDescription(solvis).getId();
		String id2 = o.getChannelDescription(solvis).getId();

		if (!this.getChannelDescription(solvis).getId().equals(o.getChannelDescription(solvis).getId())) {
			return id1.compareTo(id2);
		}
		SingleData<?> d1 = this.getData(solvis);
		SingleData<?> d2 = o.getData(solvis);

		return Helper.compareTo(d1, d2);
	}

	@Override
	public String toString() {
		return "ChannelId: " + this.id + ", value: " + this.value
				+ (this.priority == null ? "" : ", priority: " + this.priority);
	}

	public Integer getPriority() {
		return this.priority;
	}

	public String getId() {
		return this.id;
	}

	public String getStandbyId() {
		return this.standbyId;
	}

}
