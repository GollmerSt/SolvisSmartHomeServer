package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.GraficsLearnable.LearnScreen;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class SolvisDescription {
	private final String homeId;
	private final ScreenSaver saver;
	private final AllScreens screens;
	private final FallBack fallBack;
	private final AllScreenGraficDescriptions screenGrafics;
	private final AllDataDescriptions dataDescriptions;
	private final AllDurations durations;
	private final Miscellaneous miscellaneous;

	public SolvisDescription(String homeId, ScreenSaver saver, AllScreens screens, FallBack fallBack,
			AllScreenGraficDescriptions screenGrafics, AllDataDescriptions dataDescriptions, AllDurations durations,
			Miscellaneous miscellaneous) {
		this.homeId = homeId;
		this.saver = saver;
		this.screens = screens;
		this.fallBack = fallBack;
		this.screenGrafics = screenGrafics;
		this.dataDescriptions = dataDescriptions;
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

	public Collection<LearnScreen> getLearnScreens() {

		Collection<LearnScreen> learnScreens = new ArrayList<>();
		this.screens.createAndAddLearnScreen(null, learnScreens);
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
		private ScreenSaver saver;
		private AllScreens screens;
		private FallBack fallBack;
		private AllDataDescriptions dataDescriptions;
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
			return new SolvisDescription(homeId, saver, screens, fallBack, screenGrafics, dataDescriptions, durations,
					miscellaneous);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case "ScreenSaver":
					return new ScreenSaver.Creator(id, this);
				case "Screens":
					return new AllScreens.Creator(id, this);
				case "FallBack":
					return new FallBack.Creator(id, this);
				case "ScreenGrafics":
					return new CreatorScreenGrafics(id, this);
				case "DataDescriptions":
					return new AllDataDescriptions.Creator(id, this);
				case "Durations":
					return new AllDurations.Creator(id, this);
				case "Miscellaneous":
					return new Miscellaneous.Creator(id, this);
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case "ScreenSaver":
					this.saver = (ScreenSaver) created;
					break;
				case "Screens":
					this.screens = (AllScreens) created;
					break;
				case "FallBack":
					this.fallBack = (FallBack) created;
					break;
				case "ScreenGrafics": {
					@SuppressWarnings("unchecked")
					Collection<ScreenGraficDescription> collection = (Collection<ScreenGraficDescription>) created;
					this.screenGrafics.addAll(collection);
				}
					break;
				case "DataDescriptions":
					this.dataDescriptions = (AllDataDescriptions) created;
					break;
				case "Durations":
					this.durations = (AllDurations) created;
					break;
				case "Miscellaneous":
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
	public AllDataDescriptions getDataDescriptions() {
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
}
