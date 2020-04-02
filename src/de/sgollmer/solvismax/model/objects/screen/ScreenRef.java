package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.Assigner;
import de.sgollmer.solvismax.model.objects.OfConfigs;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ScreenRef implements Assigner {
	private final String id;
	private OfConfigs<Screen> screen = null;

	public ScreenRef(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public static class Creator extends CreatorByXML<ScreenRef> {

		protected String id;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
			}

		}

		@Override
		public ScreenRef create() throws XmlError, IOException {
			return new ScreenRef(this.id);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
		}

	}

	@Override
	public void assign(SolvisDescription description) {
		this.screen = description.getScreens().get(this.id);

	}

	public OfConfigs<Screen> getScreen() {
		return this.screen;
	}

}
