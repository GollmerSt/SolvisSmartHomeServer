/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.IXmlElement;
import de.sgollmer.xmllibrary.XmlException;

/**
 * A reference to the screens of all configurations, ensuring that they are of
 * the screen type.
 * 
 * @author stefa_000
 *
 */

public class ScreenRef implements IXmlElement<SolvisDescription> {
	private final String id;
	private OfConfigs<AbstractScreen> screen = null;
	private boolean initialized = false;

	protected ScreenRef(String id) {
		this.id = id;
	}

	public ScreenRef() {
		this.id = null;
	}

	protected String getId() {
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
		public ScreenRef create() throws XmlException, IOException {
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
	public void postProcess(SolvisDescription description) throws XmlException {
		this.screen = description.getScreens().getScreen(this.id);
		this.initialized = true;
	}

	@Override
	public boolean isInitialisationFinished() {
		return this.initialized;
	}

	public OfConfigs<AbstractScreen> getScreen() {
		return this.screen;
	}

	public AbstractScreen getScreen(Solvis solvis) {
		if (this.screen == null) {
			return null;
		}
		return this.screen.get(solvis);
	}

}
