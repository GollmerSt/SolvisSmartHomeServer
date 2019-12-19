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
import de.sgollmer.solvismax.model.Command;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Screen.ScreenTouch;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class AllChannelDescriptions implements Assigner, GraficsLearnable {

	private static final String XML_CHANNEL_DESCRIPTION = "ChannelDescription";

	private Map<String, ChannelDescription> descriptions = new HashMap<>();

	public void addDescription(ChannelDescription description) {
		this.descriptions.put(description.getId(), description);
	}

	public ChannelDescription get(String descriptionString) {
		return this.descriptions.get(descriptionString);
	}

	@Override
	public void assign(SolvisDescription description) {
		for (ChannelDescription data : descriptions.values()) {
			data.assign(description);
		}
	}

	public Collection<ChannelDescription> get() {
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
	public void createAndAddLearnScreen(LearnScreen learnScreen, Collection<LearnScreen> learnScreens) {
		for (ChannelDescription description : this.descriptions.values()) {
			((GraficsLearnable) description).createAndAddLearnScreen(null, learnScreens);
		}

	}

	@Override
	public void learn(Solvis solvis) throws IOException {
		for (ChannelDescription description : this.descriptions.values()) {
			if (description instanceof GraficsLearnable) {
				description.learn(solvis);
			}
		}
	}

	public void measure(Solvis solvis, AllSolvisData datas) throws IOException, ErrorPowerOn {
		solvis.clearMeasuredData();
		solvis.getDistributor().setBurstUpdate(true);
		for (ChannelDescription description : this.descriptions.values()) {
			if (description.getType() == ChannelSourceI.Type.MEASUREMENT) {
				solvis.getValue(description);
			}
		}
		solvis.getDistributor().setBurstUpdate(false);
	}

	public void init(Solvis solvis, AllSolvisData datas) throws IOException {

		for (ChannelDescription description : this.descriptions.values()) {
			datas.get(description);
		}

		for (ChannelDescription description : this.descriptions.values()) {
			description.instantiate(solvis);
		}
	}

	public void initControl(Solvis solvis) {
		List<ChannelDescription> descriptions = new ArrayList<>();
		for (ChannelDescription description : this.descriptions.values()) {
			if (description.getType() == ChannelSourceI.Type.CONTROL) {
				descriptions.add(description);
			}
		}
		Collections.sort(descriptions, new Comparator<ChannelDescription>() {

			@Override
			public int compare(ChannelDescription o1, ChannelDescription o2) {

				List<ScreenTouch> p1 = o1.getScreen().getPreviosScreens();
				List<ScreenTouch> p2 = o2.getScreen().getPreviosScreens();

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
			solvis.execute(new Command(description));
		}

	}
}
