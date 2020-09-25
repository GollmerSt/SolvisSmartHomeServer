package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.StringData;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Dependency implements IAssigner {
	private final String id;
	private final String value;

	private OfConfigs<ChannelDescription> channel = null;

	protected ChannelDescription description;

	public Dependency(String id, String value) {
		this.id = id;
		this.value = value;
	}

	public static class Creator extends CreatorByXML<Dependency> {

		private String id;
		private String value;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
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
		public Dependency create() throws XmlException, IOException, AssignmentException, ReferenceException {
			return new Dependency(this.id, this.value);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {

		}

	}

	public ChannelDescription getChannelDescription(Solvis solvis) {
		return solvis.getChannelDescription(this.id);
	}

	public SingleData<?> getData(Solvis solvis) {
		ChannelDescription description = this.getChannelDescription(solvis);
		try {
			return description.interpretSetData(new StringData(this.value, 0));
		} catch (TypeException e) {
		}
		return null;
	}

	@Override
	public void assign(SolvisDescription description) throws XmlException, AssignmentException, ReferenceException {
		this.channel = description.getChannelDescriptions().get(this.id);
		if (this.channel == null) {
			throw new ReferenceException("Channel < " + this.id + " > not found");
		}

		for (ChannelDescription cd : this.channel) {
			if (this.value != null) {
				try {
					cd.interpretSetData(new StringData(this.value, 0));
				} catch (TypeException e) {
					throw new XmlException("Error in control.xml, invalid dependency value <" + this.value
							+ "> defined for channel <" + cd.getId() + ">.");
				}
			}
		}

	}

	public static boolean equals(Dependency d1, Dependency d2, Solvis solvis) {

		Boolean result = Helper.checkNull(d1, d2);
		if (result != null) {
			return result;
		}
		if (d1.getChannelDescription(solvis) != d2.getChannelDescription(solvis)) {
			return false;
		}
		SingleData<?> dd1 = d1.getData(solvis);
		SingleData<?> dd2 = d2.getData(solvis);

		return Helper.equals(dd1, dd2);

	}

	public int compareTo(Dependency o, Solvis solvis) {
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
		return "ChannelId: " + this.id + ", value: " + this.value;
	}

}