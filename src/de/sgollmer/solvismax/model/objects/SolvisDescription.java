package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.GraficsLearnable.LearnScreen;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class SolvisDescription {

	private static final String XML_CONFIGURATIONS = "Configurations";
	private static final String XML_SCREEN_SAVER = "ScreenSaver";
	private static final String XML_SCREENS = "Screens";
	private static final String XML_FALL_BACK = "FallBack";
	private static final String XML_SCREEN_GRAFICS = "ScreenGrafics";
	private static final String XML_CHANNEL_DESCRIPTIONS = "ChannelDescriptions";
	private static final String XML_LAUNCHES = "Launches";
	private static final String XML_DURATIONS = "Durations";
	private static final String XML_MISCELLANEOUS = "Miscellaneous";

	private final String homeId;
	private final Configurations configurations;
	private final ScreenSaver saver;
	private final AllScreens screens;
	private final FallBack fallBack;
	private final AllScreenGraficDescriptions screenGrafics;
	private final AllChannelDescriptions dataDescriptions;
	private final AllLaunches allLaunches;
	private final AllDurations durations;
	private final Miscellaneous miscellaneous;

	public SolvisDescription(String homeId, Configurations configurations, ScreenSaver saver, AllScreens screens,
			FallBack fallBack, AllScreenGraficDescriptions screenGrafics, AllChannelDescriptions dataDescriptions,
			AllLaunches allLaunches, AllDurations durations, Miscellaneous miscellaneous) {
		this.homeId = homeId;
		this.configurations = configurations;
		this.saver = saver;
		this.screens = screens;
		this.fallBack = fallBack;
		this.screenGrafics = screenGrafics;
		this.dataDescriptions = dataDescriptions;
		this.allLaunches = allLaunches;
		this.durations = durations;
		this.miscellaneous = miscellaneous;

		this.process();

	}

	private void process() {
		this.saver.assign(this);
		this.screens.assign(this);
		this.screenGrafics.assign(this);
		this.dataDescriptions.assign(this);
	}

	public Collection<LearnScreen> getLearnScreens(int configurationMask) {

		Collection<LearnScreen> learnScreens = new ArrayList<>();
		this.screens.createAndAddLearnScreen(null, learnScreens, configurationMask);
		for (Iterator<LearnScreen> itOuter = learnScreens.iterator(); itOuter.hasNext();) {
			LearnScreen outer = itOuter.next();
			for (Iterator<LearnScreen> itInner = itOuter; itInner.hasNext();) {
				LearnScreen inner = itInner.next();
				if (inner.getDescription().getId().equals(outer.getDescription().getId())) {
					itOuter.remove();
					break;
				}
			}
		}
		return learnScreens;
	}

	public static class Creator extends BaseCreator<SolvisDescription> {

		private final AllScreenGraficDescriptions screenGrafics;
		private String homeId;
		private Configurations configurations;
		private ScreenSaver saver;
		private AllScreens screens;
		private FallBack fallBack;
		private AllChannelDescriptions dataDescriptions;
		private AllLaunches allLaunches;
		private AllDurations durations;
		private Miscellaneous miscellaneous;

		public Creator(String id) {
			super(id);
			this.screenGrafics = new AllScreenGraficDescriptions();
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "homeId":
					this.homeId = value;
			}

		}

		@Override
		public SolvisDescription create() throws XmlError {
			return new SolvisDescription(homeId, configurations, saver, screens, fallBack, screenGrafics,
					dataDescriptions, allLaunches, durations, miscellaneous);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_CONFIGURATIONS:
					return new Configurations.Creator(id, this);
				case XML_SCREEN_SAVER:
					return new ScreenSaver.Creator(id, this);
				case XML_SCREENS:
					return new AllScreens.Creator(id, this);
				case XML_FALL_BACK:
					return new FallBack.Creator(id, this);
				case XML_SCREEN_GRAFICS:
					return new CreatorScreenGrafics(id, this);
				case XML_CHANNEL_DESCRIPTIONS:
					return new AllChannelDescriptions.Creator(id, this);
				case XML_LAUNCHES:
					return new AllLaunches.Creator(id, this);
				case XML_DURATIONS:
					return new AllDurations.Creator(id, this);
				case XML_MISCELLANEOUS:
					return new Miscellaneous.Creator(id, this);
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_CONFIGURATIONS:
					this.configurations = (Configurations) created;
					break;
				case XML_SCREEN_SAVER:
					this.saver = (ScreenSaver) created;
					break;
				case XML_SCREENS:
					this.screens = (AllScreens) created;
					break;
				case XML_FALL_BACK:
					this.fallBack = (FallBack) created;
					break;
				case XML_SCREEN_GRAFICS: {
					@SuppressWarnings("unchecked")
					Collection<ScreenGraficDescription> collection = (Collection<ScreenGraficDescription>) created;
					this.screenGrafics.addAll(collection);
				}
					break;
				case XML_CHANNEL_DESCRIPTIONS:
					this.dataDescriptions = (AllChannelDescriptions) created;
					break;
				case XML_LAUNCHES:
					this.allLaunches = (AllLaunches) created;
					break;
				case XML_DURATIONS:
					this.durations = (AllDurations) created;
					break;
				case XML_MISCELLANEOUS:
					this.miscellaneous = (Miscellaneous) created;
			}

		}

		/**
		 * @return the screenGrafics
		 */
		public AllScreenGraficDescriptions getScreenGraficDescriptions() {
			return screenGrafics;
		}
	}

	private static class CreatorScreenGrafics extends CreatorByXML<Collection<ScreenGraficDescription>> {

		private final Collection<ScreenGraficDescription> grafics = new ArrayList<>();

		public CreatorScreenGrafics(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {

		}

		@Override
		public Collection<ScreenGraficDescription> create() throws XmlError {
			return grafics;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case "ScreenGrafic":
					return new ScreenGraficDescription.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case "ScreenGrafic":
					this.grafics.add((ScreenGraficDescription) created);
			}
		}

	}

	/**
	 * @return the homeId
	 */
	public String getHomeId() {
		return homeId;
	}

	/**
	 * @return the saver
	 */
	public ScreenSaver getSaver() {
		return saver;
	}

	/**
	 * @return the screens
	 */
	public AllScreens getScreens() {
		return screens;
	}

	/**
	 * @return the screenGrafics
	 */
	public AllScreenGraficDescriptions getScreenGrafics() {
		return screenGrafics;
	}

	/**
	 * @return the dataDescriptions
	 */
	public AllChannelDescriptions getChannelDescriptions() {
		return dataDescriptions;
	}

	/**
	 * @return the durations
	 */
	public AllDurations getDurations() {
		return durations;
	}

	public Miscellaneous getMiscellaneous() {
		return miscellaneous;
	}

	public FallBack getFallBack() {
		return fallBack;
	}

	public int getConfigurations(Solvis solvis) throws IOException {
		return this.configurations.get(solvis);
	}
}
