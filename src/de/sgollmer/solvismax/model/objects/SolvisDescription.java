package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class SolvisDescription {
	private final String homeId;
	private final ScreenSaver saver;
	private final AllScreens screens;
	private final AllScreenGrafics screenGrafics ;
	private final AllDataDescriptions dataDescriptions;
	private final AllDurations durations;

	public SolvisDescription(String homeId, ScreenSaver saver, AllScreens screens, AllScreenGrafics screenGrafics, AllDataDescriptions dataDescriptions,
			AllDurations durations) {
		this.homeId = homeId;
		this.saver = saver;
		this.screens = screens;
		this.screenGrafics = screenGrafics ;
		this.dataDescriptions = dataDescriptions;
		this.durations = durations;
		
		this.process() ;
		
	}
	
	private void process() {
		this.saver.assign(this);
		this.screens.assign(this);
		this.screenGrafics.assign(this);
		this.dataDescriptions.assign(this);
	}

	public static class Creator extends CreatorByXML<SolvisDescription> implements BaseCreator<Creator>{

		private final AllScreenGrafics screenGrafics ;
		private String homeId;
		private ScreenSaver saver;
		private AllScreens screens;
		private AllDataDescriptions dataDescriptions;
		private AllDurations durations;

		public Creator(String id) {
			super(id);
			this.screenGrafics = new AllScreenGrafics();
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
			return new SolvisDescription(homeId, saver, screens, screenGrafics, dataDescriptions, durations);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case "ScreenSaver":
					return new ScreenSaver.Creator(id, this);
				case "Screens":
					return new AllScreens.Creator(id, this);
				case "ScreenGrafics":
					return new CreatorScreenGrafics(id, this);
				case "DataDescriptions":
					return new AllDataDescriptions.Creator(id, this);
				case "Durations":
					return new AllDurations.Creator(id, this);
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
				case "ScreenGrafics": {
					@SuppressWarnings("unchecked")
					Collection<ScreenGrafic> collection = (Collection<ScreenGrafic>) created;
					this.screenGrafics.addAll(collection);
				}
					break;
				case "DataDescriptions":
					this.dataDescriptions = (AllDataDescriptions) created;
					break;
				case "Durations":
					this.durations = (AllDurations) created;
					break;
			}

		}

		@Override
		public Creator getCreator() {
			return this;
		}

		/**
		 * @return the screenGrafics
		 */
		public AllScreenGrafics getScreenGrafics() {
			return screenGrafics;
		}
	}

	private static class CreatorScreenGrafics extends CreatorByXML<Collection<ScreenGrafic>> {

		private final Collection<ScreenGrafic> grafics = new ArrayList<>();

		public CreatorScreenGrafics(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {

		}

		@Override
		public Collection<ScreenGrafic> create() throws XmlError {
			return grafics;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case "ScreenGrafic":
					return new ScreenGrafic.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case "ScreenGrafic":
					this.grafics.add((ScreenGrafic) created);
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
	public AllScreenGrafics getScreenGrafics() {
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
}
