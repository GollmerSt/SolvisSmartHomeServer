/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.eclipse.paho.client.mqttv3.MqttException;

import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.MqttConnectionLost;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.clock.ClockMonitor;
import de.sgollmer.solvismax.model.objects.configuration.Configurations;
import de.sgollmer.solvismax.model.objects.configuration.SolvisTypes;
import de.sgollmer.solvismax.model.objects.screen.IScreenPartCompare;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class SolvisDescription {

	private static final String XML_SOLVIS_TYPES = "SolvisTypes";
	private static final String XML_CONFIGURATIONS = "Configurations";
	private static final String XML_SCREEN_SAVER = "ScreenSaver";
	private static final String XML_STANDBY = "Standby";
	private static final String XML_SCREENS = "Screens";
	private static final String XML_FALL_BACK = "FallBack";
	private static final String XML_SCREEN_GRAFICS = "ScreenGrafics";
	private static final String XML_CHANNEL_DESCRIPTIONS = "ChannelDescriptions";
	private static final String XML_PREPARATIONS = "Preparations";
	private static final String XML_CLOCK = "Clock";
	private static final String XML_DURATIONS = "Durations";
	private static final String XML_MISCELLANEOUS = "Miscellaneous";
	private static final String XML_SCREEN_GRAFIC = "ScreenGrafic";
	private static final String XML_ERROR_DETECTION = "ErrorDetection";
	private static final String XML_SERVICE = "Service";

	private final SolvisTypes types;
	private final Configurations configurations;
	private final Standby standby;
	private final ScreenSaver saver;
	private final AllScreens screens;
	private final FallBack fallBack;
	private final AllScreenGraficDescriptions screenGrafics;
	private final AllChannelDescriptions dataDescriptions;
	private final AllPreparations allPreparations;
	private final ClockMonitor clock;
	private final ErrorDetection errorDetection;
	private final Service service;

	public ClockMonitor getClock() {
		return this.clock;
	}

	private final AllDurations durations;
	private final Miscellaneous miscellaneous;

	private SolvisDescription(SolvisTypes types, Configurations configurations, Standby standby, ScreenSaver saver,
			AllScreens screens, FallBack fallBack, AllScreenGraficDescriptions screenGrafics,
			AllChannelDescriptions dataDescriptions, AllPreparations allPreparations, ClockMonitor clock,
			AllDurations durations, Miscellaneous miscellaneous, ErrorDetection errorDetection, Service service)
			throws XmlException {
		this.types = types;
		this.configurations = configurations;
		this.standby = standby;
		this.saver = saver;
		this.screens = screens;
		this.fallBack = fallBack;
		this.screenGrafics = screenGrafics;
		this.dataDescriptions = dataDescriptions;
		this.allPreparations = allPreparations;
		this.clock = clock;
		this.durations = durations;
		this.miscellaneous = miscellaneous;
		this.errorDetection = errorDetection;
		this.service = service;

	}

	public void assign() throws XmlException, AssignmentException, ReferenceException {
		if (this.saver != null) {
			this.saver.assign(this);
		}
		if (this.screens != null) {
			this.screens.assign(this);
		}
		if (this.screenGrafics != null) {
			this.screenGrafics.assign(this);
		}
		if (this.dataDescriptions != null) {
			this.dataDescriptions.assign(this);
		}
		if (this.clock != null) {
			this.clock.assign(this);
		}
		if (this.fallBack != null) {
			this.fallBack.assign(this);
		}
		if (this.service != null) {
			this.service.assign(this);
		}
	}

	public Collection<IScreenPartCompare> getLearnGrafics(Solvis solvis) {
		Collection<IScreenPartCompare> learnGrafics = new ArrayList<>();
		this.screens.addLearnScreenGrafics(learnGrafics, solvis);
		return learnGrafics;
	}

	public static class Creator extends BaseCreator<SolvisDescription> {

		private final AllScreenGraficDescriptions screenGrafics;
		private SolvisTypes types;
		private Configurations configurations;
		private Standby standby;
		private ScreenSaver saver;
		private AllScreens screens;
		private FallBack fallBack;
		private AllChannelDescriptions dataDescriptions;
		private AllPreparations allPreparations;
		private ClockMonitor clock;
		private ErrorDetection errorDetection;
		private Service service;

		private AllDurations durations;
		private Miscellaneous miscellaneous;

		public Creator(String id) {
			super(id);
			this.screenGrafics = new AllScreenGraficDescriptions();
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public SolvisDescription create() throws XmlException {
			return new SolvisDescription(this.types, this.configurations, this.standby, this.saver, this.screens,
					this.fallBack, this.screenGrafics, this.dataDescriptions, this.allPreparations, this.clock,
					this.durations, this.miscellaneous, this.errorDetection, this.service);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_SOLVIS_TYPES:
					return new SolvisTypes.Creator(id, getBaseCreator());
				case XML_CONFIGURATIONS:
					return new Configurations.Creator(id, this);
				case XML_STANDBY:
					return new Standby.Creator(id, this);
				case XML_SCREEN_SAVER:
					return new ScreenSaver.Creator(id, this);
				case XML_SCREENS:
					return new AllScreens.Creator(id, this);
				case XML_FALL_BACK:
					return new FallBack.Creator(id, this, false);
				case XML_SCREEN_GRAFICS:
					return new CreatorScreenGrafics(id, this);
				case XML_CHANNEL_DESCRIPTIONS:
					return new AllChannelDescriptions.Creator(id, this);
				case XML_PREPARATIONS:
					return new AllPreparations.Creator(id, this);
				case XML_CLOCK:
					return new ClockMonitor.Creator(id, this);
				case XML_DURATIONS:
					return new AllDurations.Creator(id, this);
				case XML_MISCELLANEOUS:
					return new Miscellaneous.Creator(id, this);
				case XML_ERROR_DETECTION:
					return new ErrorDetection.Creator(id, this);
				case XML_SERVICE:
					return new Service.Creator(id, this);
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_SOLVIS_TYPES:
					this.types = (SolvisTypes) created;
					break;
				case XML_CONFIGURATIONS:
					this.configurations = (Configurations) created;
					break;
				case XML_STANDBY:
					this.standby = (Standby) created;
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
				case XML_PREPARATIONS:
					this.allPreparations = (AllPreparations) created;
					break;
				case XML_CLOCK:
					this.clock = (ClockMonitor) created;
					break;
				case XML_DURATIONS:
					this.durations = (AllDurations) created;
					break;
				case XML_MISCELLANEOUS:
					this.miscellaneous = (Miscellaneous) created;
					break;
				case XML_ERROR_DETECTION:
					this.errorDetection = (ErrorDetection) created;
					break;
				case XML_SERVICE:
					this.service = (Service) created;
			}

		}

		/**
		 * @return the screenGrafics
		 */
		public AllScreenGraficDescriptions getScreenGraficDescriptions() {
			return this.screenGrafics;
		}
	}

	private static class CreatorScreenGrafics extends CreatorByXML<Collection<ScreenGraficDescription>> {

		private final Collection<ScreenGraficDescription> grafics = new ArrayList<>();

		private CreatorScreenGrafics(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {

		}

		@Override
		public Collection<ScreenGraficDescription> create() throws XmlException {
			return this.grafics;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_SCREEN_GRAFIC:
					return new ScreenGraficDescription.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_SCREEN_GRAFIC:
					this.grafics.add((ScreenGraficDescription) created);
					break;
			}
		}

	}

	/**
	 * @return the saver
	 */
	public ScreenSaver getSaver() {
		return this.saver;
	}

	/**
	 * @return the screens
	 */
	public AllScreens getScreens() {
		return this.screens;
	}

	/**
	 * @return the screenGrafics
	 */
	public AllScreenGraficDescriptions getScreenGrafics() {
		return this.screenGrafics;
	}

	/**
	 * @return the dataDescriptions
	 */
	public AllChannelDescriptions getChannelDescriptions() {
		return this.dataDescriptions;
	}

	public Duration getDuration(String id) {
		return this.durations.get(id);
	}

	public Miscellaneous getMiscellaneous() {
		return this.miscellaneous;
	}

	public FallBack getFallBack() {
		return this.fallBack;
	}

	public int getConfigurations(Solvis solvis) throws IOException, TerminationException, LearningException {
		int configurationMask = this.types.getConfiguration(solvis.getType());
		return configurationMask | this.configurations.get(solvis);
	}

	public AllPreparations getPreparations() {
		return this.allPreparations;
	}

	public ErrorDetection getErrorDetection() {
		return this.errorDetection;
	}

	public Service getService() {
		return this.service;
	}

	public void instantiate(Solvis solvis) {
		this.clock.instantiate(solvis);
		this.errorDetection.instantiate(solvis);
	}

	public void sendToMqtt(Solvis solvis, Mqtt mqtt, boolean deleteRetained) throws MqttException, MqttConnectionLost {
		this.getChannelDescriptions().sendToMqtt(solvis, mqtt, deleteRetained);
	}

	public Standby getStandby() {
		return this.standby;
	}
}
