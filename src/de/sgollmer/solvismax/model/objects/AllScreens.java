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

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.ScreenLearnable;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class AllScreens implements ScreenLearnable {

	private static final String XML_SCREEN = "Screen";

	private final String homeId;
	private final Map<String, OfConfigs<Screen>> screens;

	public AllScreens(String homeId, Map<String, OfConfigs<Screen>> screens) {
		this.homeId = homeId;
		this.screens = screens;
	}

	public OfConfigs<Screen> get(String id) {
		return this.screens.get(id);
	}

	public Screen get(String id, int configurationMask) {
		OfConfigs<Screen> screens = this.screens.get(id);
		if (screens == null) {
			return null;
		} else {
			return screens.get(configurationMask);
		}
	}

	public Screen getScreen(MyImage image, Solvis solvis) {
		int configurationMask = solvis.getConfigurationMask();
		for (OfConfigs<Screen> screenConf : screens.values()) {
			Screen screen = screenConf.get(configurationMask);
			if (screen != null && screen.isScreen(image, solvis)) {
				return screen;
			}
		}
		return null;
	}

	public void assign(SolvisDescription description) {
		for (OfConfigs<Screen> screenConf : screens.values()) {
			screenConf.assign(description);
		}
	}

	public static class Creator extends CreatorByXML<AllScreens> {

		private String homeId;
		private final Map<String, OfConfigs<Screen>> screens = new HashMap<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		private void add(Screen screen) throws XmlError {
			OfConfigs<Screen> screenConf = this.screens.get(screen.getId());
			if (screenConf == null) {
				screenConf = new OfConfigs<Screen>();
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
		public AllScreens create() throws XmlError {
			return new AllScreens(homeId, screens);
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
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_SCREEN:
					this.add((Screen) created);
					break;
			}
		}

	}

	@Override
	public void createAndAddLearnScreen(LearnScreen learnScreen, Collection<LearnScreen> learnScreens,
			int configurationMask) {
		for (OfConfigs<Screen> screenConf : screens.values()) {
			Screen screen = screenConf.get(configurationMask);
			if (screen != null) {
				screen.createAndAddLearnScreen(null, learnScreens, configurationMask);
			}
		}
	}

	@Override
	public void learn(Solvis solvis) {

	}

	public String getHomeId() {
		return homeId;
	}

}
