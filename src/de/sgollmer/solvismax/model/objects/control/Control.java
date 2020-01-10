/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.ReferenceError;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllPreparations.PreparationRef;
import de.sgollmer.solvismax.model.objects.ChannelSource;
import de.sgollmer.solvismax.model.objects.ChannelSourceI;
import de.sgollmer.solvismax.model.objects.OfConfigs;
import de.sgollmer.solvismax.model.objects.Preparation;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.update.UpdateStrategies;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Control extends ChannelSource {

	private static final Logger logger = LogManager.getLogger(Control.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private static final String XML_CONTROL_CURRENT = "Current";
	private static final String XML_CONTROL_TYPE_VALUE = "TypeValue";
	private static final String XML_CONTROL_TYPE_READ = "TypeRead";
	private static final String XML_CONTROL_TYPE_MODE = "TypeMode";
	private static final String XML_UPDATE_BY = "UpdateBy";
	private static final String XML_PREPARATION_REF = "PreparationRef";

	private final String screenId;
	private final Rectangle valueRectangle;
	private final Strategy strategy;
	private final UpdateStrategies updateStrategies;
	private final String preparationId;
	private Preparation preparation = null;

	private OfConfigs<Screen> screen = null;

	public Control(String screenId, Rectangle current, Strategy strategy, UpdateStrategies updateStrategies,
			String preparationId) {
		this.screenId = screenId;
		this.valueRectangle = current;
		this.strategy = strategy;
		this.strategy.setCurrentRectangle(current);
		this.updateStrategies = updateStrategies;
		if (this.updateStrategies != null) {
			this.updateStrategies.setSource(this);
		}
		this.preparationId = preparationId;
	}

	private boolean prepare(Solvis solvis) throws IOException, TerminationException {
		if (this.preparation == null) {
			return true;
		} else {
			return this.preparation.execute(solvis);
		}
	}

	@Override
	public boolean getValue(SolvisData destin, Solvis solvis, int timeAfterLastSwitchingOn)
			throws IOException, TerminationException {
		solvis.gotoScreen(screen.get(solvis.getConfigurationMask()));
		if (!this.prepare(solvis)) {
			return false;
		}
		SingleData<?> data = this.strategy.getValue(solvis.getCurrentImage(), this.valueRectangle, solvis);
		if (data == null) {
			return false;
		} else {
			destin.setSingleData(data);
			return true;
		}
	}

	@Override
	public boolean setValue(Solvis solvis, SolvisData value) throws IOException, TerminationException {
		solvis.gotoScreen(screen.get(solvis.getConfigurationMask()));
		if (!this.prepare(solvis)) {
			return false;
		}
		boolean set = false;
		for (int c = 0; c < Constants.SET_REPEATS + 1 && !set; ++c) {
			set = this.strategy.setValue(solvis, this.valueRectangle, value);
			if (!set && c == 1) {
				logger.error("Setting of <" + this.getDescription().getId() + "> to " + value
						+ " failed, set will be tried again.");
			}
		}
		if (!set) {
			logger.error("Setting of <" + this.getDescription().getId() + "> not successfull");
		} else {
			logger.info("Channel <" + description.getId() + "> is set to " + value.toString() + ">.");
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
	public Float getAccuracy() {
		return this.strategy.getAccuracy();
	}

	@Override
	public void assign(SolvisDescription description) throws ReferenceError {

		if (updateStrategies != null) {
			this.updateStrategies.assign(description);
		}
		this.screen = description.getScreens().get(screenId);
		if (this.screen == null) {
			throw new ReferenceError("Screen of reference < " + this.screenId + " > not found");
		}

		this.strategy.assign(description);

		if (preparationId != null) {
			this.preparation = description.getPreparations().get(preparationId);
			if (this.preparation == null) {
				throw new ReferenceError("Preparation of reference < " + this.preparationId + " > not found");
			}
			this.preparation.assign(description);
		}

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
		private String preparationId = null;

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
			return new Control(screenId, current, strategy, updateStrategies, preparationId);
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
				case XML_PREPARATION_REF:
					return new PreparationRef.Creator(id, getBaseCreator());
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
				case XML_PREPARATION_REF:
					this.preparationId = ((PreparationRef) created).getPreparationId();
					break;
			}
		}

	}

	@Override
	public void learn(Solvis solvis) throws IOException, LearningError {
		if (this.preparation != null) {
			this.preparation.learn(solvis, this.getScreen(solvis.getConfigurationMask()));
		}

		if (strategy.mustBeLearned()) {
			SingleData<?> data = null;
			solvis.gotoScreen(this.screen.get(solvis.getConfigurationMask()));
			MyImage saved = solvis.getCurrentImage();
			boolean finished = false;
			for (int repeat = 0; repeat < Constants.LEARNING_RETRIES && !finished; ++repeat) {
				finished = this.strategy.learn(solvis);
				if (finished) {
					data = this.strategy.getValue(saved, this.valueRectangle, solvis);
					if (data == null) {
						finished = false;
					}
				}
				if (!finished) {
					logger.log(LEARN,
							"Learning of <" + this.getDescription().getId() + "> not successfull, will be retried");
				}
			}
			if (!finished) {
				String error = "Learning of <" + this.getDescription().getId()
						+ "> not possible, rekjected. Check the Xml!";
				logger.error(error);
				throw new LearningError(error);
			}
			for (int repeat = 0; repeat < Constants.SET_REPEATS; ++repeat) {
				boolean success = this.setValue(solvis, new SolvisData(data));
				if (success) {
					break;
				} else {
					AbortHelper.getInstance()
							.sleep(solvis.getSolvisDescription().getMiscellaneous().getUnsuccessfullWaitTime_ms());
				}
			}
		}
	}

//	@Override
//	public void learn(Solvis solvis) throws IOException, TerminationException {
//		if (this.preparation != null) {
//			this.preparation.learn(solvis, this.getScreen(solvis.getConfigurationMask()));
//		}
//		if (this.strategy instanceof StrategyMode) {
//			StrategyMode strategy = (StrategyMode) this.strategy;
//			solvis.gotoScreen(this.screen.get(solvis.getConfigurationMask()));
//			MyImage saved = solvis.getCurrentImage();
//			boolean finished = false;
//			SingleData<?> data = null;
//			for (int repeat = 0; repeat < Constants.LEARNING_RETRIES && !finished; ++repeat) {
//				for (Mode mode : strategy.getModes()) {
//					solvis.send(mode.getTouch());
//					mode.getGrafic().learn(solvis);
//				}
//				data = this.strategy.getValue(saved, this.valueRectangle, solvis);
//				if (data == null) {
//					logger.log(LEARN,
//							"Learning of <" + this.getDescription().getId() + "> not successfull, will be retried");
//				} else {
//					finished = true;
//				}
//			}
//			if (!finished) {
//				String error = "Learning of <" + this.getDescription().getId() + "> not possible, rekjected.";
//				logger.error(error);
//				throw new LearningError(error);
//			}
//			for (int repeat = 0; repeat < Constants.SET_REPEATS; ++repeat) {
//				boolean success = this.setValue(solvis, new SolvisData(data));
//				if (success) {
//					break;
//				} else {
//					AbortHelper.getInstance()
//							.sleep(solvis.getSolvisDescription().getMiscellaneous().getUnsuccessfullWaitTime_ms());
//				}
//			}
//		}
//	}

	@Override
	public Type getType() {
		return ChannelSourceI.Type.CONTROL;
	}

	@Override
	public Screen getScreen(int configurationMask) {
		return this.screen.get(configurationMask);
	}

	@Override
	public Collection<? extends ModeI> getModes() {
		return strategy.getModes();
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return strategy.getUpperLowerStep();
	}

	@Override
	public boolean isBoolean() {
		return false;
	}

}
