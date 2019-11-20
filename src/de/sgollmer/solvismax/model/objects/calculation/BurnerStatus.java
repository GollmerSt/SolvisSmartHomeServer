package de.sgollmer.solvismax.model.objects.calculation;

import de.sgollmer.solvismax.error.AssignmentError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllDataDescriptions;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.Dependencies;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class BurnerStatus extends Strategy<BurnerStatus> {

	public BurnerStatus(Calculation calculation) {
		super(calculation);
	}

	public BurnerStatus() {
		super(null);
	}

	public enum Status implements ModeI {
		OFF("off"), LEVEL1("Stufe1"), LEVEL2("Stufe2");

		private String name;

		private Status(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	@Override
	public BurnerStatus create(Calculation calculation) {
		return new BurnerStatus(calculation);
	}

	@Override
	public String getUnit() {
		return null;
	}

	@Override
	public boolean isWriteable() {
		return true;
	}

	@Override
	public void instantiate(Solvis solvis) {
		AllSolvisData allData = solvis.getAllSolvisData();
		Dependencies dependencies = this.calculation.getDependencies();

		SolvisData result = allData.get(this.calculation.getId());

		SolvisData burnerLevel1On = dependencies.get(allData, "burnerLevel1On");
		SolvisData burnerLevel2On = dependencies.get(allData, "burnerLevel2On");

		Executable executable = new Executable(result, burnerLevel1On, burnerLevel2On);

		executable.update(burnerLevel1On);

	}

	private class Executable implements ObserverI<SolvisData> {

		private final SolvisData result;
		private final SolvisData burnerLevel1On;
		private final SolvisData burnerLevel2On;

		public Executable(SolvisData result, SolvisData burnerLevel1On, SolvisData burnerLevel2On) {
			this.result = result;
			this.burnerLevel1On = burnerLevel1On;
			this.burnerLevel2On = burnerLevel2On;
			this.burnerLevel1On.register(this);
			this.burnerLevel2On.register(this);
		}

		@Override
		public void update(SolvisData data) {
			if (result == null || burnerLevel1On == null || burnerLevel2On == null) {
				throw new AssignmentError("Assignment error: Dependencies not assigned");
			}

			Status result = null;

			boolean level1 = burnerLevel1On.getBool();
			boolean level2 = burnerLevel1On.getBool();

			if (level2) {
				result = Status.LEVEL2;
			} else if (level1) {
				result = Status.LEVEL1;
			} else {
				result = Status.OFF;
			}
			this.result.setMode(result);

		}
	}

	@Override
	public void assign(AllDataDescriptions descriptions) {

	}

}