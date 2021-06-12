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
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.configuration.ConfigurationTypes.Type;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.IScreenPartCompare;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.smarthome.MaskIterator;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Configurations {

	private static final String XML_SOLVIS_TYPES = "SolvisTypes";
	private static final String XML_MAIN_HEATINGS = "MainHeatings";
	private static final String XML_EXTENSIONS = "Extensions";
	private static final String XML_HEATER_CIRCUITS = "HeaterCircuits";
	private static final String XML_SOLAR_TYPES = "SolarTypes";
	private static final String XML_HEATER_LOOPS = "HeaterLoops";
	private static final String XML_SOLAR = "Solar";
	private static final String XML_NOT_VALID_CONFIGURATIONS = "NotValid";

	private final ConfigurationTypes solvisTypes;
	private final ConfigurationTypes mainHeatings;
	private final ConfigurationTypes heaterCircuits;
	private final ConfigurationTypes solarTypes;
	private final ConfigurationTypes extensions;
	private final NotValidConfigurations notValidConfigurations;

	private final Collection<IConfiguration> configurations;

	private Configurations(final ConfigurationTypes types, final ConfigurationTypes mainHeatings,
			final ConfigurationTypes heaterCircuits, final ConfigurationTypes solarTypes,
			final ConfigurationTypes extensions, final Collection<IConfiguration> configurations,
			final NotValidConfigurations notValidConfiguartions) {
		this.solvisTypes = types;
		this.mainHeatings = mainHeatings;
		this.heaterCircuits = heaterCircuits;
		this.solarTypes = solarTypes;
		this.extensions = extensions;
		this.notValidConfigurations = notValidConfiguartions;

		this.configurations = configurations;
	}

	public int get(final Solvis solvis) throws IOException, TerminationException, LearningException {

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

		private ConfigurationTypes solvisTypes;
		private ConfigurationTypes mainHeatings;
		private ConfigurationTypes heaterCircuits;
		private ConfigurationTypes solarTypes;
		private ConfigurationTypes extensions;
		private NotValidConfigurations notValidConfiguartions = null;

		private final Collection<IConfiguration> configurations = new ArrayList<>();

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
		}

		@Override
		public Configurations create() throws XmlException, IOException {
			return new Configurations(this.solvisTypes, this.mainHeatings, this.heaterCircuits, this.solarTypes,
					this.extensions, this.configurations, this.notValidConfiguartions);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_SOLVIS_TYPES:
				case XML_MAIN_HEATINGS:
				case XML_HEATER_CIRCUITS:
				case XML_SOLAR_TYPES:
				case XML_EXTENSIONS:
					return new ConfigurationTypes.Creator(id, getBaseCreator());
				case XML_HEATER_LOOPS:
					return new HeaterLoops.Creator(id, getBaseCreator());
				case XML_SOLAR:
					return new Solar.Creator(id, getBaseCreator());
				case XML_NOT_VALID_CONFIGURATIONS:
					return new NotValidConfigurations.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_SOLVIS_TYPES:
					this.solvisTypes = (ConfigurationTypes) created;
					break;
				case XML_MAIN_HEATINGS:
					this.mainHeatings = (ConfigurationTypes) created;
					break;
				case XML_HEATER_CIRCUITS:
					this.heaterCircuits = (ConfigurationTypes) created;
					break;
				case XML_SOLAR_TYPES:
					this.solarTypes = (ConfigurationTypes) created;
					break;
				case XML_EXTENSIONS:
					this.extensions = (ConfigurationTypes) created;
					break;
				case XML_HEATER_LOOPS:
					this.configurations.add((IConfiguration) created);
					break;
				case XML_SOLAR:
					this.configurations.add((IConfiguration) created);
					break;
				case XML_NOT_VALID_CONFIGURATIONS:
					this.notValidConfiguartions = (NotValidConfigurations) created;
			}
		}

	}

	public ConfigurationTypes getSolvisTypes() {
		return this.solvisTypes;
	}

	public ConfigurationTypes getMainHeatings() {
		return this.mainHeatings;
	}

	public ConfigurationTypes getHeaterCircuits() {
		return this.heaterCircuits;
	}

	public ConfigurationTypes getSolarTypes() {
		return this.solarTypes;
	}

	public ConfigurationTypes getExtensions() {
		return this.extensions;
	}

	public MaskIterator getConfigurationIterator() {

		Type emptyType = new Type(null, 0L, false);
		MaskIterator iterator = new MaskIterator(this.solvisTypes.getTypes(), null);
		iterator = new MaskIterator(this.mainHeatings.getTypes(), iterator);
		iterator = new MaskIterator(this.heaterCircuits.getTypes(), iterator);
		iterator = new MaskIterator(this.solarTypes.getTypes(), iterator);

		for (Type type : this.extensions.getTypes()) {
			if (type.isDontCare()) {
				continue;
			}
			Collection<Type> types = new ArrayList<>();
			types.add(emptyType);
			types.add(type);
			iterator = new MaskIterator(types, iterator);
		}
		return iterator;
	}

	public boolean isValid(final Long mask, final SolvisDescription description) {
		return this.notValidConfigurations.isValid(mask, description);
	}

}
