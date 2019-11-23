package de.sgollmer.solvismax.model.objects.control;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.ReferenceError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.DataSource;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Control extends DataSource {
	private final String screenId;
	private final Rectangle valueRectangle;
	private final Strategy strategy;
	private final Collection<UpdateStrategies.Strategy<?>> updateStrategies;

	private Screen screen = null;

	public Control(String screenId, Rectangle current, Strategy strategy, Collection<UpdateStrategies.Strategy<?>> updateStrategies) {
		this.screenId = screenId;
		this.valueRectangle = current;
		this.strategy = strategy;
		this.updateStrategies = updateStrategies ;
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
	public void assign(SolvisDescription description) {

		for (UpdateStrategies.Strategy<?> strategy : this.updateStrategies) {
			strategy.setControl(this);
			strategy.assign(description);
		}
		this.screen = description.getScreens().get(screenId);
		if (this.screen == null) {
			throw new ReferenceError("Screen reference < " + this.screenId + " > not found");
		}

		this.strategy.assign(description);

	}

	@Override
	public void instantiate(Solvis solvis) {
		for (UpdateStrategies.Strategy<?> strategy : this.updateStrategies) {
			strategy.instantiate(solvis);
		}

	}

	public static class Creator extends CreatorByXML<Control> {

		private String screenId;
		private Rectangle current;
		private Strategy strategy;
		private Collection<UpdateStrategies.Strategy<?>> updateStrategies = new ArrayList<>(2);

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "screenId":
					this.screenId = value;
					break;
			}

		}

		@Override
		public Control create() throws XmlError {
			return new Control(screenId, current, strategy, updateStrategies);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case "Current":
					return new Rectangle.Creator(id, this.getBaseCreator());
				case "TypeValue":
					return new StrategyValue.Creator(id, this.getBaseCreator());
				case "TypeRead":
					return new StrategyRead.Creator(id, this.getBaseCreator());
				case "TypeMode":
					return new StrategyMode.Creator(id, this.getBaseCreator());
				case "UpdateHeatingBurner":
					return new BurnerSometimes.Creator(id, this.getBaseCreator());
				case "UpdateScreenChange":
					return new ByScreenChange.Creator(id, this.getBaseCreator());
			}
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case "Current":
					this.current = (Rectangle) created;
					break;
				case "TypeValue":
				case "TypeRead":
				case "TypeMode":
					this.strategy = (Strategy) created;
					break;
				case "UpdateHeatingBurner":
				case "UpdateScreenChange":
					this.updateStrategies.add((UpdateStrategies.Strategy<?>) created);
					break;
			}
		}

	}

}
