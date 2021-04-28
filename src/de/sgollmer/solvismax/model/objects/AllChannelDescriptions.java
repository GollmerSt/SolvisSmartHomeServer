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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.command.CommandControl;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.model.objects.control.Dependency;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.IGraficsLearnable;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class AllChannelDescriptions implements IAssigner, IGraficsLearnable {

	private static final String XML_CHANNEL_DESCRIPTION = "ChannelDescription";

	private Map<String, OfConfigs<ChannelDescription>> descriptions = new HashMap<>(3);

	private Map<ChannelConfig, Collection<ChannelDescription>> updateControlChannelsSequences = new HashMap<>(3);
	private Map<ChannelConfig, Collection<ChannelDescription>> updateByScreenChangeSequences = new HashMap<>(3);
	private Map<ChannelConfig, Collection<ChannelDescription>> updateReadOnlyControlChannelsSequences = new HashMap<>(
			3);

	private AllChannelDescriptions() {
	}

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

	public ChannelDescription get(String id, Solvis solvis) {
		OfConfigs<ChannelDescription> descriptions = this.descriptions.get(id);
		if (descriptions == null) {
			return null;
		} else {
			return descriptions.get(solvis);
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
	public void learn(Solvis solvis) throws IOException, LearningException, TerminationException {
		for (OfConfigs<ChannelDescription> descriptions : this.descriptions.values()) {
			ChannelDescription description = descriptions.get(solvis);
			if (description != null && description instanceof IGraficsLearnable) {
				description.learn(solvis);
			}
		}
	}

	public enum MeasureMode {
		ALL, FAST, STANDARD
	}

	public void measure(Solvis solvis, AllSolvisData datas, MeasureMode mode)
			throws IOException, PowerOnException, TerminationException, NumberFormatException {
		solvis.clearMeasuredData();
		solvis.getMeasureData();
		solvis.getDistributor().setBurstUpdate(true);
		try {
			for (OfConfigs<ChannelDescription> descriptions : this.descriptions.values()) {
				ChannelDescription description = descriptions.get(solvis);
				if (description != null && description.getType() == IChannelSource.Type.MEASUREMENT) {
					if (mode == MeasureMode.ALL || mode == MeasureMode.FAST && description.isFast()
							|| mode == MeasureMode.STANDARD && !description.isFast()) {
						description.getValue(solvis);
					}
				}
			}
		} catch (IOException e) {
			throw e;
		} catch (PowerOnException e) {
			throw e;
		} catch (TerminationException e) {
			throw e;
		} finally {
			solvis.getDistributor().setBurstUpdate(false);
		}
	}

	public void init(Solvis solvis) throws IOException, AssignmentException, AliasException {
		AllSolvisData datas = solvis.getAllSolvisData();
		for (OfConfigs<ChannelDescription> descriptions : this.descriptions.values()) {
			ChannelDescription description = descriptions.get(solvis);
			if (description != null) {
				boolean ignore = solvis.getUnit().isChannelIgnored(description.getId());
				SolvisData data = new SolvisData(description, solvis.getAllSolvisData(), ignore);
				data.setAsSmartHomedata();
				if (description.isAverage()) {
					solvis.getSolvisState().register(data);
				}
				datas.add(data);
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
		this.updateChannels(solvis, Type.ALL_CONTROL, this.updateControlChannelsSequences);
	}

	public void updateReadOnlyControlChannels(Solvis solvis) {
		this.updateChannels(solvis, Type.READONLY, this.updateReadOnlyControlChannelsSequences);
	}

	public void updateByHumanAccessFinished(Solvis solvis) {
		this.updateChannels(solvis, Type.HUMAN_ACCESS_DEPENDEND, this.updateByScreenChangeSequences);
	}

	private enum Type {
		ALL_CONTROL, READONLY, WRITEABLE, HUMAN_ACCESS_DEPENDEND
	}

	private static class ChannelConfig {
		private final boolean admin;
		private final int configurationMask;

		public ChannelConfig(Solvis solvis) {
			this.admin = solvis.isAdmin();
			this.configurationMask = solvis.getConfigurationMask();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ChannelConfig)) {
				return false;
			} else {
				ChannelConfig cmp = (ChannelConfig) obj;
				return this.admin == cmp.admin && this.configurationMask == cmp.configurationMask;
			}
		}

		@Override
		public int hashCode() {
			return (Integer.hashCode(this.configurationMask) * 449) + Boolean.hashCode(this.admin) * 53 + 751;
		}
	}

	private void updateChannels(Solvis solvis, Type type, Map<ChannelConfig, Collection<ChannelDescription>> destin) {

		ChannelConfig config = new ChannelConfig(solvis);
		Collection<ChannelDescription> descriptions = destin.get(config);

		if (descriptions == null) {

			descriptions = new ArrayList<>();

			for (OfConfigs<ChannelDescription> confDescriptions : this.descriptions.values()) {
				ChannelDescription description = confDescriptions.get(solvis);
				if (description != null && description.isInConfiguration(solvis)) {
					boolean add = false;
					switch (type) {
						case ALL_CONTROL:
							add = description.getType() == IChannelSource.Type.CONTROL;
							break;
						case READONLY:
							add = !description.isWriteable() && description.getType() == IChannelSource.Type.CONTROL;
							break;
						case WRITEABLE:
							add = description.isWriteable() && description.getType() == IChannelSource.Type.CONTROL;
							break;
						case HUMAN_ACCESS_DEPENDEND:
							add = description.isHumanAccessDependend();
					}
					if (add) {
						descriptions.add(description);
					}
				}
			}
			descriptions = this.optimize((List<ChannelDescription>) descriptions, solvis);
			destin.put(config, descriptions);
		}

		for (ChannelDescription description : descriptions) {
			solvis.execute(new CommandControl(description, solvis));
		}
		return;
	}

	private Collection<ChannelDescription> optimize(List<ChannelDescription> descriptions, Solvis solvis) {

		Collection<ChannelDescription> toFind = new ArrayList<>(descriptions);

		Collection<ChannelDescription> optimized = new ArrayList<>(descriptions.size());

		this.buildOptimized(solvis.getHomeScreen(), toFind, optimized, solvis);

		return optimized;
	}

	private void buildOptimized(Screen start, Collection<ChannelDescription> toFind,
			Collection<ChannelDescription> destin, final Solvis solvis) {

		List<ChannelDescription> channelsOfScreenChannelDescriptions = new ArrayList<>();

		for (Iterator<ChannelDescription> it = toFind.iterator(); it.hasNext();) {
			ChannelDescription oneToFind = it.next();
			if (oneToFind.getScreen(solvis) == start) {
				channelsOfScreenChannelDescriptions.add(oneToFind);
				it.remove();
			}
		}

		channelsOfScreenChannelDescriptions.sort(new Comparator<ChannelDescription>() {

			@Override
			public int compare(ChannelDescription o1, ChannelDescription o2) {
				Collection<Dependency> ds1 = Helper.copy(o1.getDependencyGroup().get());
				Collection<Dependency> ds2 = Helper.copy(o2.getDependencyGroup().get());
				for (Iterator<Dependency> it1 = ds1.iterator(); it1.hasNext();) {
					Dependency d1 = it1.next();
					for (Iterator<Dependency> it2 = ds2.iterator(); it2.hasNext();) {
						Dependency d2 = it2.next();
						if (Dependency.equals(d1, d2, solvis)) {
							it1.remove();
							it2.remove();
							break;
						}
					}
				}

				if (ds1.isEmpty() && ds2.isEmpty()) {
					return 0;
				}

				if (ds1.isEmpty() && !ds2.isEmpty()) {
					return -1;
				}

				if (!ds1.isEmpty() && ds2.isEmpty()) {
					return 1;
				}
				return ds1.iterator().next().compareTo(ds2.iterator().next(), solvis);
			}
		});

		destin.addAll(channelsOfScreenChannelDescriptions);

		for (AbstractScreen next : start.getNextScreens(solvis)) {
			if (next instanceof Screen) {
				this.buildOptimized((Screen) next, toFind, destin, solvis);
			}
		}

	}

	public Collection<ChannelDescription> getChannelDescriptions(AbstractScreen screen, Solvis solvis)
			throws TypeException {
		Collection<ChannelDescription> result = new ArrayList<>();
		for (OfConfigs<ChannelDescription> descriptionsC : this.descriptions.values()) {
			ChannelDescription description = descriptionsC.get(solvis);
			if (description != null) {
				AbstractScreen channelScreen = descriptionsC.get(solvis).getScreen(solvis);
				if (channelScreen != null && screen == channelScreen) {
					result.add(description);
				}
			}
		}
		return result;
	}

}
