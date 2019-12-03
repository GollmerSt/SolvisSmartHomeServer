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

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Command;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class AllDataDescriptions implements Assigner, GraficsLearnable {

	private Map<String, DataDescription> descriptions = new HashMap<>();

	public void addDescription(DataDescription description) {
		this.descriptions.put(description.getId(), description);
	}

	public DataDescription get(String descriptionString) {
		return this.descriptions.get(descriptionString);
	}

	@Override
	public void assign(SolvisDescription description) {
		for (DataDescription data : descriptions.values()) {
			data.assign(description);
		}
	}

	public static class Creator extends CreatorByXML<AllDataDescriptions> {

		private final AllDataDescriptions descriptions = new AllDataDescriptions();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {

		}

		@Override
		public AllDataDescriptions create() throws XmlError {
			return descriptions;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			if (id.equals("DataDescription")) {
				return new DataDescription.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			this.descriptions.addDescription((DataDescription) created);

		}
	}

	@Override
	public void createAndAddLearnScreen(LearnScreen learnScreen, Collection<LearnScreen> learnScreens) {
		for (DataDescription description : this.descriptions.values()) {
			((GraficsLearnable) description).createAndAddLearnScreen(null, learnScreens);
		}

	}

	@Override
	public void learn(Solvis solvis) throws IOException {
		// TODO Auto-generated method stub
		for (DataDescription description : this.descriptions.values()) {
			if (description instanceof GraficsLearnable) {
				description.learn(solvis);
			}
		}
	}

	public void measure(Solvis solvis, AllSolvisData datas) throws IOException {
		solvis.clearMeasuredData();
		for (DataDescription description : this.descriptions.values()) {
			if (description.getType() == DataSourceI.Type.MEASUREMENT) {
				solvis.getValue(description);
			}
		}
	}

	public void init(Solvis solvis, AllSolvisData datas) throws IOException {

		for (DataDescription description : this.descriptions.values()) {
			datas.get(description);
		}

		for (DataDescription description : this.descriptions.values()) {
			description.instantiate(solvis);
		}

		// this.measure(solvis, datas);
		//
		// for (DataDescription description : this.descriptions.values()) {
		// if (description.getType() == DataSourceI.Type.CALCULATION) {
		// description.instantiate(solvis);
		// }
		// }
		// List<DataDescription> descriptions = new ArrayList<>();
		// for (DataDescription description : this.descriptions.values()) {
		// if (description.getType() == DataSourceI.Type.CONTROL) {
		// descriptions.add(description);
		// }
		// }
		// Collections.sort(descriptions, new Comparator<DataDescription>() {
		//
		// @Override
		// public int compare(DataDescription o1, DataDescription o2) {
		// return o1.getScreen().compareTo(o2.getScreen());
		// }
		// });
		// for (DataDescription description : descriptions) {
		// solvis.getValue(description);
		// }
	}

	public void initControl(Solvis solvis) {
		List<DataDescription> descriptions = new ArrayList<>();
		for (DataDescription description : this.descriptions.values()) {
			if (description.getType() == DataSourceI.Type.CONTROL) {
				descriptions.add(description);
			}
		}
		Collections.sort(descriptions, new Comparator<DataDescription>() {

			@Override
			public int compare(DataDescription o1, DataDescription o2) {
				return o1.getScreen().compareTo(o2.getScreen());
			}
		});
		for (DataDescription description : descriptions) {
			solvis.execute(new Command(description));
		}

	}
}
