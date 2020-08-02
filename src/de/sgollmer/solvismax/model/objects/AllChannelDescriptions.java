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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.eclipse.paho.client.mqttv3.MqttException;

import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.DependencyException;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.ModbusException;
import de.sgollmer.solvismax.error.MqttConnectionLost;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.model.CommandControl;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.IGraficsLearnable;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class AllChannelDescriptions implements IAssigner, IGraficsLearnable {

	private static final String XML_CHANNEL_DESCRIPTION = "ChannelDescription";

	private Map<String, OfConfigs<ChannelDescription>> descriptions = new HashMap<>();

	private Map<Integer, Collection<ChannelDescription>> updateControlChannelsSequences = new HashMap<>();
	private Map<Integer, Collection<ChannelDescription>> updateByScreenChangeSequences = new HashMap<>();

	private void addDescription(ChannelDescription description) throws XmlException {
		OfConfigs<ChannelDescription> channelConf = this.descriptions.get(description.getId());
		if (channelConf == null) {
			channelConf = new OfConfigs<ChannelDescription>();
			this.descriptions.put(description.getId(), channelConf);
		}
		channelConf.verifyAndAdd(description);
	}

	public OfConfigs<ChannelDescription> get(String id) {
		return this.descriptions.get(id);
	}

	public ChannelDescription get(String id, int configurationMask) {
		OfConfigs<ChannelDescription> descriptions = this.descriptions.get(id);
		if (descriptions == null) {
			return null;
		} else {
			return descriptions.get(configurationMask);
		}
	}

	@Override
	public void assign(SolvisDescription description) throws XmlException, AssignmentException, ReferenceException {
		for (OfConfigs<ChannelDescription> channelConf : this.descriptions.values()) {
			channelConf.assign(description);
		}
	}

	public Collection<OfConfigs<ChannelDescription>> get() {
		return this.descriptions.values();
	}

	static class Creator extends CreatorByXML<AllChannelDescriptions> {

		private final AllChannelDescriptions descriptions = new AllChannelDescriptions();

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {

		}

		@Override
		public AllChannelDescriptions create() throws XmlException {
			return this.descriptions;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			if (id.equals(XML_CHANNEL_DESCRIPTION)) {
				return new ChannelDescription.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_CHANNEL_DESCRIPTION:
					this.descriptions.addDescription((ChannelDescription) created);
					break;
			}

		}
	}

	@Override
	public void learn(Solvis solvis) throws IOException, LearningException, TerminationException, ModbusException {
		for (OfConfigs<ChannelDescription> descriptions : this.descriptions.values()) {
			ChannelDescription description = descriptions.get(solvis);
			if (description != null && description instanceof IGraficsLearnable) {
				description.learn(solvis);
			}
		}
	}

	public void measure(Solvis solvis, AllSolvisData datas)
			throws IOException, PowerOnException, TerminationException, ModbusException {
		solvis.clearMeasuredData();
		solvis.getMeasureData();
		solvis.getDistributor().setBurstUpdate(true);
		for (OfConfigs<ChannelDescription> descriptions : this.descriptions.values()) {
			ChannelDescription description = descriptions.get(solvis);
			if (description != null) {
				if (description.getType() == IChannelSource.Type.MEASUREMENT) {
					description.getValue(solvis);
				} else if (description.isModbus(solvis)) {
					description.getValue(solvis);
				}
			}
		}
		solvis.getDistributor().setBurstUpdate(false);
	}

	public void init(Solvis solvis) throws IOException, AssignmentException, DependencyException {
		AllSolvisData datas = solvis.getAllSolvisData();
		for (OfConfigs<ChannelDescription> descriptions : this.descriptions.values()) {
			ChannelDescription description = descriptions.get(solvis);
			if (description != null) {
				SolvisData data = datas.get(description);
				if (description.isAverage()) {
					solvis.getSolvisState().register(data);
				}
			}
		}

		for (OfConfigs<ChannelDescription> descriptions : this.descriptions.values()) {
			ChannelDescription description = descriptions.get(solvis);
			if (description != null) {
				description.instantiate(solvis);
			}
		}
	}

//	private Map<ConfigurationMask, Collection<ChannelDescription>> updateByScreenChangeSequences = new HashMap<>();

	public void updateControlChannels(Solvis solvis) {
		final int configurationMask = solvis.getConfigurationMask();

		Collection<ChannelDescription> descriptions = this.updateControlChannelsSequences.get(configurationMask);

		if (descriptions == null) {
			descriptions = new ArrayList<>();
			for (OfConfigs<ChannelDescription> confDescriptions : this.descriptions.values()) {
				ChannelDescription description = confDescriptions.get(solvis);
				if (description != null && description.getType() == IChannelSource.Type.CONTROL
						&& description.isInConfiguration(configurationMask) && !description.isModbus(solvis)) {
					descriptions.add(description);
				}
			}
			this.optimize((List<ChannelDescription>) descriptions, configurationMask);

			this.updateControlChannelsSequences.put(configurationMask, descriptions);
		}

		for (ChannelDescription description : descriptions) {
			solvis.execute(new CommandControl(description, solvis));
		}

	}

	public void updateByHumanAccessFinished(Solvis solvis) {
		final int configurationMask = solvis.getConfigurationMask();

		Collection<ChannelDescription> descriptions = this.updateByScreenChangeSequences.get(configurationMask);

		if (descriptions == null) {
			descriptions = new ArrayList<>();
			for (OfConfigs<ChannelDescription> confDescriptions : this.descriptions.values()) {
				ChannelDescription description = confDescriptions.get(solvis);
				if (description != null && description.isScreenChangeDependend()
						&& description.isInConfiguration(configurationMask) && !description.isModbus(solvis)) {
					descriptions.add(description);
				}
			}
			this.optimize((List<ChannelDescription>) descriptions, configurationMask);

			this.updateByScreenChangeSequences.put(configurationMask, descriptions);
		}

		for (ChannelDescription description : descriptions) {
			solvis.execute(new CommandControl(description, solvis));
		}

	}

	private static class OptimizeComparator implements Comparator<ChannelDescription> {

		private final AbstractScreen.OptimizeComparator comp;

		public OptimizeComparator(int mask) {
			this.comp = new AbstractScreen.OptimizeComparator(mask);
		}

		@Override
		public int compare(ChannelDescription o1, ChannelDescription o2) {
			return this.comp.compare(o1.getScreen(this.comp.getMask()), o2.getScreen(this.comp.getMask()));
		}
	}

	private void optimize(List<ChannelDescription> descriptions, int mask) {

		Collections.sort(descriptions, new OptimizeComparator( mask));

	}

	public Collection<ChannelDescription> getChannelDescriptions(AbstractScreen screen, Solvis solvis) {
		Collection<ChannelDescription> result = new ArrayList<>();
		for (OfConfigs<ChannelDescription> descriptionsC : this.descriptions.values()) {
			ChannelDescription description = descriptionsC.get(solvis);
			if (description != null) {
				AbstractScreen channelScreen = descriptionsC.get(solvis).getScreen(solvis.getConfigurationMask());
				if (channelScreen != null && screen == channelScreen) {
					result.add(description);
				}
			}
		}
		return result;
	}

	void sendToMqtt(Solvis solvis, Mqtt mqtt) throws MqttException, MqttConnectionLost {
		for (OfConfigs<ChannelDescription> descriptionsC : this.descriptions.values()) {
			ChannelDescription meta = descriptionsC.get(solvis);
			if (meta != null) {
				MqttData data = meta.getMqttMeta(solvis);
				mqtt.publish(data);
			}
		}

	}
}
