/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.model.objects.screen.IScreen;
import de.sgollmer.solvismax.model.objects.screen.IScreenLearnable;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class AllScreens implements IScreenLearnable {

	private static final String XML_SCREEN = "Screen";

	private final String homeId;
	private final Map<String, OfConfigs<IScreen>> screens;

	private AllScreens(String homeId, Map<String, OfConfigs<IScreen>> screens) {
		this.homeId = homeId;
		this.screens = screens;
	}

	public OfConfigs<IScreen> get(String id) {
		return this.screens.get(id);
	}

	/**
	 * Get the Screen of all configurations ensuring the type of Screen 
	 * 
	 * @param id	screen Id
	 * @return		Scrreen result of all configurations
	 * @throws XmlException
	 */
	public OfConfigs<IScreen> getScreen(String id) throws XmlException {
		OfConfigs<IScreen> screens = this.screens.get(id);
		for (IScreen screen : screens.getElements()) {
			if (!screen.isScreen()) {
				throw new XmlException("<" + screen.getId() + "> mut be type of Screen");
			}
		}
		return screens;
	}

	public IScreen get(String id, int configurationMask) {
		OfConfigs<IScreen> screens = this.screens.get(id);
		if (screens == null) {
			return null;
		} else {
			return screens.get(configurationMask);
		}
	}

	public IScreen getScreen(MyImage image, Solvis solvis) {
		int configurationMask = solvis.getConfigurationMask();
		for (OfConfigs<IScreen> screenConf : this.screens.values()) {
			IScreen screen = screenConf.get(configurationMask);
			if (screen != null && screen.isScreen(image, solvis) && screen.isScreen()) {
				return (Screen) screen;
			}
		}
		return null;
	}

	void assign(SolvisDescription description) throws XmlException, AssignmentException, ReferenceException {
		for (OfConfigs<IScreen> screenConf : this.screens.values()) {
			screenConf.assign(description);
		}

		OfConfigs<IScreen> homeScreenConfig = this.screens.get(this.homeId);
		for (IScreen screen : homeScreenConfig.getElements()) {
			if (!screen.isScreen()) {
				throw new XmlException("Home screen <" + this.homeId + "must be a Screen element");
			}
		}
	}

	static class Creator extends CreatorByXML<AllScreens> {

		private String homeId;
		private final Map<String, OfConfigs<IScreen>> screens = new HashMap<>();

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		private void add(Screen screen) throws XmlException {
			OfConfigs<IScreen> screenConf = this.screens.get(screen.getId());
			if (screenConf == null) {
				screenConf = new OfConfigs<IScreen>();
				this.screens.put(screen.getId(), screenConf);
			}
			screenConf.verifyAndAdd(screen);
		}

		@Override
		public void setAttribute(QName name, String value) {
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
		public CreatorByXML<Screen> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_SCREEN:
					return new Screen.Creator(name.getLocalPart(), this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_SCREEN:
					this.add((Screen) created);
					break;
			}
		}

	}

	@Override
	public void createAndAddLearnScreen(LearnScreen learnScreen, Collection<LearnScreen> learnScreens, Solvis solvis) {
		int configurationMask = solvis.getConfigurationMask();
		for (OfConfigs<IScreen> screenConf : this.screens.values()) {
			IScreen screen = screenConf.get(configurationMask);
			if (screen != null) {
				screen.createAndAddLearnScreen(null, learnScreens, solvis);
			}
		}
	}

	@Override
	public void learn(Solvis solvis) {

	}

	public String getHomeId() {
		return this.homeId;
	}

}
