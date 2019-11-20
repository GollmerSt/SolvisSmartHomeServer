package de.sgollmer.solvismax.model.objects;

import java.util.HashMap;
import java.util.Map;

public class AllDataDescriptions {

	private Map<String, DataDescription> descriptions = new HashMap<>();

	public void addDescription(DataDescription description) {
		this.descriptions.put(description.getId(), description);
	}

	public DataDescription get(String descriptionString) {
		return this.descriptions.get(descriptionString);
	}

	public void assign(AllSolvisData allData) {

	}
}
