package de.sgollmer.solvismax.model.objects;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class DataDescription implements DataSourceI {
	private final String id;
	private final DataSource dataSource;

	public DataDescription(String id, DataSource dataSource) {
		this.id = id;
		this.dataSource = dataSource;
	}

	public String getId() {
		return this.id;
	}

	@Override
	public boolean getValue(SolvisData dest, Solvis solvis) {
		return this.dataSource.getValue(dest, solvis);
	}

	@Override
	public boolean setValue(Solvis solvis, SolvisData value) {
		return this.dataSource.setValue(solvis, value);
	}

	@Override
	public boolean isWriteable() {
		return this.dataSource.isWriteable();
	}

	@Override
	public boolean isAverage() {
		return this.dataSource.isAverage();
	}

	@Override
	public Integer getDivisor() {
		return this.dataSource.getDivisor();
	}

	@Override
	public String getUnit() {
		return this.dataSource.getUnit();
	}

	@Override
	public void assign(AllDataDescriptions descriptions ) {
		this.dataSource.assign(descriptions);
		
	}

	@Override
	public void instantiate(Solvis solvis) {
		this.dataSource.instantiate(solvis);
		
	}
	

}
