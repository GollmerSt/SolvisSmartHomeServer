/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.IScreenLearnable;
import de.sgollmer.solvismax.model.objects.screen.IScreenPartCompare;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.ScreenSequence;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.IXmlElement;
import de.sgollmer.xmllibrary.XmlException;

public class AllScreens implements IScreenLearnable, IXmlElement<SolvisDescription> {

	private static final String XML_SCREEN = "Screen";
	private static final String XML_SCREEN_SEQUENCE = "ScreenSequence";

	private final String homeId;
	private final Map<String, OfConfigs<AbstractScreen>> screens;
	private boolean initialized = false;

	private AllScreens(final String homeId, final Map<String, OfConfigs<AbstractScreen>> screens) {
		this.homeId = homeId;
		this.screens = screens;
	}

	public OfConfigs<AbstractScreen> get(final String id) {
		return this.screens.get(id);
	}

	/**
	 * Get the Screen of all configurations ensuring the type of Screen
	 * 
	 * @param id screen Id
	 * @return Scrreen result of all configurations
	 * @throws XmlException
	 */
	public OfConfigs<AbstractScreen> getScreen(final String id) throws XmlException {
		OfConfigs<AbstractScreen> screens = this.screens.get(id);
		if (screens == null) {
			throw new XmlException("Screen <" + id + "> is unknown, check the control.xml");
		}
		for (AbstractScreen screen : screens.getElements()) {
			if (!screen.isScreen()) {
				throw new XmlException("<" + screen.getId() + "> must be type of Screen, check the control.xml");
			}
		}
		return screens;
	}

	public AbstractScreen get(final String id, final Solvis solvis) {
		OfConfigs<AbstractScreen> screens = this.screens.get(id);
		if (screens == null) {
			return null;
		} else {
			return screens.get(solvis);
		}
	}

	public Screen getScreen(final MyImage image, final Solvis solvis) {
		for (OfConfigs<AbstractScreen> screenConf : this.screens.values()) {
			AbstractScreen screen = screenConf.get(solvis);
			if (screen != null && screen.isMatchingScreen(image, solvis) && screen.isScreen()) {
				return (Screen) screen;
			}
		}
		return null;
	}

	public Collection<Screen> getScreens(final Solvis solvis) {
		Collection<Screen> screens = new ArrayList<>();
		for (OfConfigs<AbstractScreen> screenConf : this.screens.values()) {
			AbstractScreen screen = screenConf.get(solvis);
			if (screen != null && screen.isScreen()) {
				screens.add((Screen) screen);
			}
		}
		return screens;
	}

	@Override
	public void postProcess(final SolvisDescription description) throws XmlException {
		OfConfigs<AbstractScreen> homeScreenConfig = this.screens.get(this.homeId);
		this.initialized = true;
		for (AbstractScreen screen : homeScreenConfig.getElements()) {
			if (!screen.isInitialisationFinished()) {
				this.initialized = false;
			} else if (!screen.isScreen()) {
				throw new XmlException("Home screen <" + this.homeId + "must be a Screen element");
			}
		}
		this.initialized = true;
	}

	@Override
	public boolean isInitialisationFinished() {
		return this.initialized;
	}

	static class Creator extends CreatorByXML<AllScreens> {

		private String homeId;
		private final Map<String, OfConfigs<AbstractScreen>> screens = new HashMap<>();

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		private void add(final AbstractScreen screen) throws XmlException {
			OfConfigs<AbstractScreen> screenConf = this.screens.get(screen.getId());
			if (screenConf == null) {
				screenConf = new OfConfigs<AbstractScreen>();
				this.screens.put(screen.getId(), screenConf);
			}
			screenConf.verifyAndAdd(screen);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "homeId":
					this.homeId = value;
					break;
			}
		}

		@Override
		public AllScreens create() throws XmlException {
			return new AllScreens(this.homeId, this.screens);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_SCREEN:
					return new Screen.Creator(id, this.getBaseCreator());
				case XML_SCREEN_SEQUENCE:
					return new ScreenSequence.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_SCREEN:
					this.add((AbstractScreen) created);
					break;
				case XML_SCREEN_SEQUENCE:
					this.add((AbstractScreen) created);
					break;
			}
		}

	}

	@Override
	public void addLearnScreenGrafics(final Collection<IScreenPartCompare> learnScreens, final Solvis solvis) {
		for (OfConfigs<AbstractScreen> screenConf : this.screens.values()) {
			AbstractScreen screen = screenConf.get(solvis);
			if (screen != null) {
				screen.addLearnScreenGrafics(learnScreens, solvis);
			}
		}
	}

	public String getHomeId() {
		return this.homeId;
	}

}
