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
import java.util.ListIterator;
import java.util.Map;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.CommandControl;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Screen.ScreenTouch;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class AllChannelDescriptions implements Assigner, GraficsLearnable {

	private static final String XML_CHANNEL_DESCRIPTION = "ChannelDescription";

	private Map<String, OfConfigs<ChannelDescription>> descriptions = new HashMap<>();

	public void addDescription(ChannelDescription description) {
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
	public void assign(SolvisDescription description) {
		for (OfConfigs<ChannelDescription> channelConf : descriptions.values()) {
			channelConf.assign(description);
		}
	}

	public Collection<OfConfigs<ChannelDescription>> get() {
		return this.descriptions.values();
	}

	public static class Creator extends CreatorByXML<AllChannelDescriptions> {

		private final AllChannelDescriptions descriptions = new AllChannelDescriptions();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {

		}

		@Override
		public AllChannelDescriptions create() throws XmlError {
			return descriptions;
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
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_CHANNEL_DESCRIPTION:
					this.descriptions.addDescription((ChannelDescription) created);
					break;
			}

		}
	}

	@Override
	public void learn(Solvis solvis) throws IOException {
		for (OfConfigs<ChannelDescription> descriptions : this.descriptions.values()) {
			ChannelDescription description = descriptions.get(solvis.getConfigurationMask());
			if (description != null && description instanceof GraficsLearnable) {
				description.learn(solvis);
			}
		}
	}

	public void measure(Solvis solvis, AllSolvisData datas) throws IOException, ErrorPowerOn {
		solvis.clearMeasuredData();
		solvis.getMeasureData() ;
		solvis.getDistributor().setBurstUpdate(true);
		for (OfConfigs<ChannelDescription> descriptions : this.descriptions.values()) {
			ChannelDescription description = descriptions.get(solvis.getConfigurationMask());
			if (description != null &&description.getType() == ChannelSourceI.Type.MEASUREMENT) {
				description.getValue(solvis);
			}
		}
		solvis.getDistributor().setBurstUpdate(false);
	}

	public void init(Solvis solvis, AllSolvisData datas) throws IOException {

		for (OfConfigs<ChannelDescription> descriptions : this.descriptions.values()) {
			ChannelDescription description = descriptions.get(solvis.getConfigurationMask());
			if (description != null) {
				datas.get(description);
			}
		}

		for (OfConfigs<ChannelDescription> descriptions : this.descriptions.values()) {
			ChannelDescription description = descriptions.get(solvis.getConfigurationMask());
			if (description != null) {
				description.instantiate(solvis);
			}
		}
	}

	public void initControl(Solvis solvis) {
		final int configurationMask = solvis.getConfigurationMask();
		List<ChannelDescription> descriptions = new ArrayList<>();
		for (OfConfigs<ChannelDescription> confDescriptions : this.descriptions.values()) {
			ChannelDescription description = confDescriptions.get(solvis.getConfigurationMask());
			if (description != null && description.getType() == ChannelSourceI.Type.CONTROL
					&& description.isInConfiguration(configurationMask)) {
				descriptions.add(description);
			}
		}
		Collections.sort(descriptions, new Comparator<ChannelDescription>() {

			@Override
			public int compare(ChannelDescription o1, ChannelDescription o2) {

				List<ScreenTouch> p1 = o1.getScreen(configurationMask).getPreviosScreens(configurationMask);
				List<ScreenTouch> p2 = o2.getScreen(configurationMask).getPreviosScreens(configurationMask);

				if (p1.size() == 0 && p2.size() == 0) {
					return 0;
				}

				if (p1.size() == 0) {
					return 1;
				} else if (p2.size() == 0) {
					return -1;
				}

				ListIterator<ScreenTouch> i1 = p1.listIterator(p1.size());
				ListIterator<ScreenTouch> i2 = p2.listIterator(p2.size());

				int cmp;

				while (i1.hasPrevious()) {
					if (i2.hasPrevious()) {
						ScreenTouch s1 = i1.previous();
						ScreenTouch s2 = i2.previous();
						cmp = s1.getScreen().compareTo(s2.getScreen());
						if (cmp != 0) {
							if (i1.hasPrevious()) {
								return -1;
							} else if (i2.hasPrevious()) {
								return 1;
							}
							return cmp;
						}
					} else {
						return -1;
					}
				}
				if (i2.hasPrevious()) {
					return 1;
				}
				return 0;

				// return o1.getScreen().compareTo(o2.getScreen());
			}
		});
		for (ChannelDescription description : descriptions) {
			solvis.execute(new CommandControl(description));
		}

	}
}
