package de.sgollmer.solvismax.model.objects.calculation;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllDataDescriptions;
import de.sgollmer.solvismax.model.objects.DataDescription;
import de.sgollmer.solvismax.model.objects.DataSource;
import de.sgollmer.solvismax.model.objects.Dependencies;
import de.sgollmer.solvismax.model.objects.Dependency;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class Calculation extends DataSource {

	private final String id ;
	private final Strategy<?> strategy;
	private final Dependencies dependencies = new Dependencies() ;

	private Calculation( DataDescription description, String id, Strategies strategies) {
		super( description ) ;
		this.id = id ;
		this.strategy = strategies.create(this);
	}
	
	public Dependency getDependency( String id ) {
		return this.dependencies.get(id) ;
	}

	@Override
	public boolean getValue(SolvisData dest, Solvis solvis) {
		return this.strategy.getValue(dest, solvis);
	}

	@Override
	public boolean setValue(Solvis solvis, SolvisData value) {
		return this.strategy.setValue(solvis, value);
	}

	@Override
	public boolean isWriteable() {
		return this.strategy.isWriteable();
	}

	@Override
	public boolean isAverage() {
		return false;
	}

	@Override
	public Integer getDivisor() {
		return 1;
	}

	@Override
	public String getUnit() {
		return this.strategy.getUnit();
	}

	@Override
	public void assign(AllDataDescriptions descriptions ) {
		this.strategy.assign(descriptions);

	}

	public String getId() {
		return id;
	}

	@Override
	public void instantiate(Solvis solvis) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @return the dependencies
	 */
	public Dependencies getDependencies() {
		return dependencies;
	}
}
