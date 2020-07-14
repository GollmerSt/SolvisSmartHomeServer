/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.Reference;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.IScreenLearnable.LearnScreen;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Configurations {

	private static final String XML_HEATER_LOOPS = "HeaterLoops";
	private static final String XML_SOLAR = "Solar";

	private final Collection<IConfiguration> configurations;

	public Configurations(Collection<IConfiguration> configurations) {
		this.configurations = configurations;
	}

	public int get(Solvis solvis, Reference<Screen> currentRef) throws IOException {

		solvis.gotoHome(true);
		Screen current = solvis.getHomeScreen();

		int configurationMask = 0;
		Screen home = solvis.getHomeScreen();
		IConfiguration homeConfiguration = null;
		Collection<LearnScreen> learnConfigurationScreens = new ArrayList<>();
		for (Iterator<IConfiguration> it = this.configurations.iterator(); it.hasNext();) {
			IConfiguration configuration = it.next();
			Screen screen = configuration.getScreen(solvis);
			if (screen == home) {
				homeConfiguration = configuration;
			} else {
				screen.createAndAddLearnScreen(null, learnConfigurationScreens, solvis);
			}
		}
		if (homeConfiguration != null) {
			Collection<LearnScreen> learnHomeScreen = new ArrayList<>();
			home.createAndAddLearnScreen(null, learnHomeScreen, solvis);
			home.gotoLearning(solvis, current, learnHomeScreen);
			home.learn(solvis, learnHomeScreen, 0);
			configurationMask |= homeConfiguration.getConfiguration(solvis);
			current = home;
		}
		for (IConfiguration configuration : this.configurations) {
			Screen screen = configuration.getScreen(solvis);
			if (screen != home) {
				screen.gotoLearning(solvis, current, learnConfigurationScreens);
				screen.learn(solvis, learnConfigurationScreens, 0);
				configurationMask |= configuration.getConfiguration(solvis);
				current = screen;
			}
		}
		home.gotoLearning(solvis, current, learnConfigurationScreens);
		currentRef.set(current);

		return configurationMask;
	}

	public interface IConfiguration {
		public int getConfiguration(Solvis solvis) throws IOException;

		public Screen getScreen(Solvis solvis);
	}

	public static class Creator extends CreatorByXML<Configurations> {

		private final Collection<IConfiguration> configurations = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public Configurations create() throws XmlError, IOException {
			return new Configurations(this.configurations);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_HEATER_LOOPS:
					return new HeaterLoops.Creator(id, getBaseCreator());
				case XML_SOLAR:
					return new Solar.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_HEATER_LOOPS:
					this.configurations.add((IConfiguration) created);
					break;
				case XML_SOLAR:
					this.configurations.add((IConfiguration) created);
					break;
			}
		}

	}
}
