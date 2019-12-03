package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.ReferenceError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.DataSource;
import de.sgollmer.solvismax.model.objects.DataSourceI;
import de.sgollmer.solvismax.model.objects.GraficsLearnable;
import de.sgollmer.solvismax.model.objects.Mode;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.update.UpdateStrategies;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Control extends DataSource {

	private static final String XML_CONTROL_CURRENT = "Current";
	private static final String XML_CONTROL_TYPE_VALUE = "TypeValue";
	private static final String XML_CONTROL_TYPE_READ = "TypeRead";
	private static final String XML_CONTROL_TYPE_MODE = "TypeMode";
	private static final String XML_UPDATE_BY = "UpdateBy";

	private final String screenId;
	private final Rectangle valueRectangle;
	private final Strategy strategy;
	private final UpdateStrategies updateStrategies;

	private Screen screen = null;

	public Control(String screenId, Rectangle current, Strategy strategy, UpdateStrategies updateStrategies) {
		this.screenId = screenId;
		this.valueRectangle = current;
		this.strategy = strategy;
		this.updateStrategies = updateStrategies;
		if (this.updateStrategies != null) {
			this.updateStrategies.setSource(this);
		}
	}

	@Override
	public boolean getValue(SolvisData destin, Solvis solvis) throws IOException {
		solvis.gotoScreen(screen);
		SingleData data = this.strategy.getValue(solvis.getCurrentImage(), this.valueRectangle, solvis);
		if (data == null) {
			return false;
		} else {
			destin.setSingleData(data);
			return true;
		}
	}

	@Override
	public boolean setValue(Solvis solvis, SolvisData value) throws IOException {
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

		if (updateStrategies != null) {
			this.updateStrategies.assign(description);
		}
		this.screen = description.getScreens().get(screenId);
		if (this.screen == null) {
			throw new ReferenceError("Screen reference < " + this.screenId + " > not found");
		}

		this.strategy.assign(description);

	}

	@Override
	public void instantiate(Solvis solvis) {
		if (this.updateStrategies != null) {
			this.updateStrategies.instantiate(solvis);
		}

	}

	public static class Creator extends CreatorByXML<Control> {

		private String screenId;
		private Rectangle current;
		private Strategy strategy;
		private UpdateStrategies updateStrategies = null;

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
				case XML_CONTROL_CURRENT:
					return new Rectangle.Creator(id, this.getBaseCreator());
				case XML_CONTROL_TYPE_VALUE:
					return new StrategyValue.Creator(id, this.getBaseCreator());
				case XML_CONTROL_TYPE_READ:
					return new StrategyRead.Creator(id, this.getBaseCreator());
				case XML_CONTROL_TYPE_MODE:
					return new StrategyMode.Creator(id, this.getBaseCreator());
				case XML_UPDATE_BY:
					return new UpdateStrategies.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_CONTROL_CURRENT:
					this.current = (Rectangle) created;
					break;
				case XML_CONTROL_TYPE_VALUE:
				case XML_CONTROL_TYPE_READ:
				case XML_CONTROL_TYPE_MODE:
					this.strategy = (Strategy) created;
					break;
				case XML_UPDATE_BY:
					this.updateStrategies = (UpdateStrategies) created;
					break;
			}
		}

	}

	@Override
	public void createAndAddLearnScreen(LearnScreen learnScreen, Collection<LearnScreen> learnScreens) {
		if (this.strategy instanceof GraficsLearnable) {
			LearnScreen learn = new LearnScreen();
			learn.setScreen(this.screen);
			((GraficsLearnable) this.strategy).createAndAddLearnScreen(learn, learnScreens);
		}

	}

	@Override
	public void learn(Solvis solvis) throws IOException {
		if (this.strategy instanceof StrategyMode) {
			StrategyMode strategy = (StrategyMode) this.strategy;
			solvis.gotoScreen(this.screen);
			MyImage saved = solvis.getCurrentImage();
			for (Mode mode : strategy.getModes()) {
				solvis.send(mode.getTouch());
				mode.getGrafic().learn(solvis);
			}
			SingleData data = this.strategy.getValue(saved, this.valueRectangle, solvis);
			this.setValue(solvis, new SolvisData(data));
		}
	}

	@Override
	public Type getType() {
		return DataSourceI.Type.CONTROL;
	}

	@Override
	public Screen getScreen() {
		return this.screen;
	}

}
