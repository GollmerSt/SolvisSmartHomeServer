package de.sgollmer.solvismax.model.objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AllScreenGrafics implements Assigner {
	private final Map<String, ScreenGrafic> screenGrafics = new HashMap<>();

	public void add(ScreenGrafic grafic) {
		this.screenGrafics.put(grafic.getId(), grafic);
	}

	public void addAll(Collection<ScreenGrafic> grafics) {
		for (ScreenGrafic grafic : grafics) {
			this.screenGrafics.put(grafic.getId(), grafic);
		}
	}

	public ScreenGrafic get(String id) {
		return this.screenGrafics.get(id);
	}

	@Override
	public void assign(SolvisDescription description) {
		for ( ScreenGrafic grafic : this.screenGrafics.values() ) {
			grafic.assign(description);
		}
		
	}
}
