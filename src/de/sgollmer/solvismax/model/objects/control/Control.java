package de.sgollmer.solvismax.model.objects.control;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.error.ReferenceError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllDataDescriptions;
import de.sgollmer.solvismax.model.objects.AllScreens;
import de.sgollmer.solvismax.model.objects.DataDescription;
import de.sgollmer.solvismax.model.objects.DataSource;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Rectangle;

public class Control extends DataSource {
	private final String screenId;
	private final Rectangle valueRectangle;
	private final Strategy strategy;
	private final Collection<UpdateStrategies.Strategy<?>>  updateStrategies = new ArrayList<>() ;

	private Screen screen = null;

	public Control( DataDescription description, String screenId, Rectangle current, Strategy strategy) {
		super(description) ;
		this.screenId = screenId;
		this.valueRectangle = current;
		this.strategy = strategy;
	}

	public void assign(AllScreens screens) {
		this.screen = screens.get(screenId);
		if (this.screen == null) {
			throw new ReferenceError("Screen reference < " + this.screenId + " > not found");
		}
	}

	@Override
	public boolean getValue(SolvisData destin, Solvis solvis) {
		solvis.gotoScreen(screen);
		SingleData data = this.strategy.getValue(solvis.getCurrentImage(), this.valueRectangle);
		if (data == null) {
			return false;
		} else {
			destin.setSingleData(data);
			return true;
		}
	}

	@Override
	public boolean setValue(Solvis solvis, SolvisData value) {
		solvis.gotoScreen(screen);
		boolean set = false;
		for (int c = 0; c < 100 && !set; ++c) {
			set = this.strategy.setValue(solvis, this.valueRectangle, value);
		}
		return set;
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
		return this.strategy.getDivisor();
	}

	@Override
	public String getUnit() {
		return this.strategy.getUnit();
	}


	@Override
	public void assign(AllDataDescriptions descriptions ) {

		for (UpdateStrategies.Strategy<?> strategy : this.updateStrategies) {
			strategy.assign(descriptions);
		}

	}

	@Override
	public void instantiate(Solvis solvis) {
		for (UpdateStrategies.Strategy<?> strategy : this.updateStrategies) {
			strategy.instantiate(solvis);
		}
		
	}

}
