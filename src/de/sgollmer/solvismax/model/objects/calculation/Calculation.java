package de.sgollmer.solvismax.model.objects.calculation;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.DataSource;
import de.sgollmer.solvismax.model.objects.Dependencies;
import de.sgollmer.solvismax.model.objects.Dependency;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Calculation extends DataSource {

	private final Strategy<?> strategy;
	private final Dependencies dependencies;

	private Calculation(Strategies strategies, Dependencies dependencies) {
		this.strategy = strategies.create(this);
		this.dependencies = dependencies;
	}

	public Dependency getDependency(String id) {
		return this.dependencies.get(id);
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
	public void instantiate(Solvis solvis) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return the dependencies
	 */
	public Dependencies getDependencies() {
		return dependencies;
	}

	public static class Creator extends CreatorByXML<Calculation> {

		private Strategies strategy;
		private final Dependencies dependencies = new Dependencies();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			String id = name.getLocalPart();
			switch (id) {
				case "strategy":
					this.strategy = Strategies.getByName(value);
					break;
			}
		}

		@Override
		public Calculation create() throws XmlError {
			return new Calculation(strategy, dependencies);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case "Dependency":
					return new Dependency.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			if (creator.getId().equals("Dependency")) {
				this.dependencies.add((Dependency) created);
			}

		}

	}

	@Override
	public void assign(SolvisDescription description) {
		this.dependencies.assign(description);
		this.strategy.assign(description);

	}
}
