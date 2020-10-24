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

import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.IScreenPartCompare;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Configurations {

	private static final String XML_HEATER_LOOPS = "HeaterLoops";
	private static final String XML_SOLAR = "Solar";

	private final Collection<IConfiguration> configurations;

	private Configurations(Collection<IConfiguration> configurations) {
		this.configurations = configurations;
	}

	public int get(Solvis solvis) throws IOException, TerminationException, LearningException {

		solvis.gotoHome(true);
		AbstractScreen current = solvis.getHomeScreen();

		int configurationMask = 0;
		Screen home = solvis.getHomeScreen();
		IConfiguration homeConfiguration = null;
		Collection<IScreenPartCompare> learnConfigurationScreens = new ArrayList<>();
		for (Iterator<IConfiguration> it = this.configurations.iterator(); it.hasNext();) {
			IConfiguration configuration = it.next();
			AbstractScreen screen = configuration.getScreen(solvis);
			if (screen == home) {
				homeConfiguration = configuration;
			} else {
				screen.addLearnScreenGrafics(learnConfigurationScreens, solvis);
			}
		}
		if (homeConfiguration != null) {
			Collection<IScreenPartCompare> learnHomeScreen = new ArrayList<>();
			home.addLearnScreenGrafics(learnHomeScreen, solvis);
			home.gotoLearning(solvis, current, learnHomeScreen);
			home.learn(solvis, learnHomeScreen);
			configurationMask |= homeConfiguration.getConfiguration(solvis);
			current = home;
		}
		for (IConfiguration configuration : this.configurations) {
			AbstractScreen screen = configuration.getScreen(solvis);
			if (screen != home) {
				screen.gotoLearning(solvis, current, learnConfigurationScreens);
				screen.learn(solvis, learnConfigurationScreens);
				configurationMask |= configuration.getConfiguration(solvis);
				current = screen;
			}
		}
		home.gotoLearning(solvis, current, learnConfigurationScreens);

		return configurationMask;
	}

	interface IConfiguration {
		int getConfiguration(Solvis solvis) throws IOException, TerminationException;

		AbstractScreen getScreen(Solvis solvis);
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
		public Configurations create() throws XmlException, IOException {
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
