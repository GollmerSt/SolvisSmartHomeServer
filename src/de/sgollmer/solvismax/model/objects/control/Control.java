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

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.ReferenceError;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.modbus.ModbusAccess;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllPreparations.PreparationRef;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.ChannelSource;
import de.sgollmer.solvismax.model.objects.IChannelSource;
import de.sgollmer.solvismax.model.objects.OfConfigs;
import de.sgollmer.solvismax.model.objects.Preparation;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.model.update.UpdateStrategies;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Control extends ChannelSource {

	private static final ILogger logger = LogManager.getInstance().getLogger(Control.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private static final String XML_CONTROL_CURRENT = "CurrentValue";
	private static final String XML_PREPARATION_REF = "PreparationRef";

	private static final String XML_GUI_ACCESS = "GuiAccess";
	private static final String XML_MODBUS_ACCESS = "ModbusAccess";
	private static final String XML_CONTROL_TYPE_READ_WRITE = "TypeReadWrite";
	private static final String XML_CONTROL_TYPE_READ = "TypeRead";
	private static final String XML_CONTROL_TYPE_MODE = "TypeMode";
	private static final String XML_CONTROL_TYPE_BUTTON = "TypeButton";
	private static final String XML_UPDATE_BY = "UpdateBy";

	private final boolean optional;
	private final GuiAccess guiAccess;
	private final ModbusAccess modbusAccess;

	private final IStrategy strategy;
	private final UpdateStrategies updateStrategies;

	private Control(boolean optional, GuiAccess guiAccess, ModbusAccess modbusAccess, IStrategy strategy,
			UpdateStrategies updateStrategies) {
		this.optional = optional;
		this.guiAccess = guiAccess;
		this.modbusAccess = modbusAccess;
		this.strategy = strategy;
		if (strategy != null) {
			this.strategy.setCurrentRectangle(guiAccess != null ? guiAccess.valueRectangle : null);
		}
		this.updateStrategies = updateStrategies;
		if (this.updateStrategies != null) {
			this.updateStrategies.setSource(this);
		}
	}

	@Override
	public boolean getValue(SolvisData destin, Solvis solvis) throws IOException, TerminationException {
		IControlAccess controlAccess = this.getControlAccess(solvis);
		if (!this.guiPrepare(solvis, controlAccess)) {
			return false;
		}
		SingleData<?> data = this.strategy.getValue(solvis.getCurrentScreen(), solvis, this.getControlAccess(solvis),
				this.optional);
		if (data == null) {
			return false;
		} else {
			destin.setSingleData(data);
			return true;
		}
	}

	@Override
	public SingleData<?> setValue(Solvis solvis, SolvisData value) throws IOException, TerminationException {

		IControlAccess controlAccess = this.getControlAccess(solvis);
		if (!this.guiPrepare(solvis, controlAccess)) {
			return null;
		}
		SingleData<?> setValue = null;
		try {
			for (int c = 0; c < Constants.SET_REPEATS + 1 && setValue == null; ++c) {
				setValue = this.strategy.setValue(solvis, this.getControlAccess(solvis), value);
				if (setValue == null && c == 1) {
					logger.error("Setting of <" + this.getDescription().getId() + "> to " + value
							+ " failed, set will be tried again.");
				}
			}
		} catch (TypeError e) {
			logger.error("Setting value <" + value.toString() + "> not defined for <" + this.getDescription().getId()
					+ ">. Setting ignored");
			return null;
		}
		if (setValue == null) {
			logger.error("Setting of <" + this.getDescription().getId() + "> not successfull");
		} else {
			logger.info("Channel <" + this.description.getId() + "> is set to " + value.toString() + ">.");
		}
		return setValue;
	}

	private boolean guiPrepare(Solvis solvis, IControlAccess controlAccess) throws IOException, TerminationException {
		if (!controlAccess.isModbus()) {
			this.guiAccess.getScreen().get(solvis).goTo(solvis);
			if (!this.guiAccess.prepare(solvis)) {
				return false;
			}
		}
		return true;
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
	public Double getAccuracy() {
		return this.strategy.getAccuracy();
	}

	@Override
	public void assign(SolvisDescription description) throws ReferenceError {

		if (this.updateStrategies != null) {
			this.updateStrategies.assign(description);
		}

		if (this.strategy != null) {
			this.strategy.assign(description);
		}

		if (this.guiAccess != null) {
			this.guiAccess.assign(description);
		}

	}

	@Override
	public void instantiate(Solvis solvis) {
		if (this.updateStrategies != null) {
			this.updateStrategies.instantiate(solvis);
		}

	}

	@Override
	public boolean isScreenChangeDependend() {
		return this.updateStrategies == null ? false : this.updateStrategies.isScreenChangeDependend();
	}

	public static class Creator extends CreatorByXML<Control> {

		private boolean optional;
		private GuiAccess guiAccess;
		private ModbusAccess modbusAccess;
		private IStrategy strategy;
		private UpdateStrategies updateStrategies = null;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "optional":
					this.optional = Boolean.parseBoolean(value);
					break;
			}
		}

		@Override
		public Control create() throws XmlError {
			if (this.modbusAccess != null && this.modbusAccess.isModbus()) {
				if (!this.strategy.isXmlValid(true)) {
					throw new XmlError("Missing modbus value definitions");
				}
			}
			if (this.guiAccess != null) {
				if (!this.strategy.isXmlValid(false)) {
					throw new XmlError("Missing gui value definitions");
				}
			}
			return new Control(this.optional, this.guiAccess, this.modbusAccess, this.strategy, this.updateStrategies);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_GUI_ACCESS:
					return new GuiAccess.Creator(id, getBaseCreator());
				case XML_MODBUS_ACCESS:
					return new ModbusAccess.Creator(id, getBaseCreator());
				case XML_CONTROL_TYPE_READ_WRITE:
					return new StrategyReadWrite.Creator(id, this.getBaseCreator());
				case XML_CONTROL_TYPE_READ:
					return new StrategyRead.Creator(id, this.getBaseCreator());
				case XML_CONTROL_TYPE_MODE:
					return new StrategyMode.Creator(id, this.getBaseCreator());
				case XML_CONTROL_TYPE_BUTTON:
					return new StrategyButton.Creator(id, this.getBaseCreator());
				case XML_UPDATE_BY:
					return new UpdateStrategies.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_GUI_ACCESS:
					this.guiAccess = (GuiAccess) created;
					break;
				case XML_MODBUS_ACCESS:
					this.modbusAccess = (ModbusAccess) created;
					break;
				case XML_CONTROL_TYPE_READ_WRITE:
				case XML_CONTROL_TYPE_READ:
				case XML_CONTROL_TYPE_MODE:
				case XML_CONTROL_TYPE_BUTTON:
					this.strategy = (IStrategy) created;
					break;
				case XML_UPDATE_BY:
					this.updateStrategies = (UpdateStrategies) created;
					break;
			}
		}

	}

	@Override
	public void learn(Solvis solvis) throws IOException, LearningError {
		if (!this.getControlAccess(solvis).isModbus() && this.strategy.mustBeLearned()) {
			SingleData<?> data = null;
			Screen screen = this.guiAccess.getScreen().get(solvis);
			if (screen == null) {
				String error = "Learning of <" + this.getDescription().getId()
						+ "> not possible, rejected. Screen undefined"
						+ " in the current configuration. Check the control.xml!";
				logger.error(error);
				throw new LearningError(error);
			}
			boolean finished = false;
			screen.goTo(solvis);
			SolvisScreen saved = solvis.getCurrentScreen();
			for (int repeat = 0; repeat < Constants.LEARNING_RETRIES && !finished; ++repeat) {
				screen.goTo(solvis);
				if (this.guiAccess.getPreparation() != null) {
					finished = this.guiAccess.getPreparation().learn(solvis);
				} else {
					finished = true;
				}
				if (finished) {
					finished = this.strategy.learn(solvis, this.getControlAccess(solvis));
				}
				if (finished) {
					data = this.strategy.getValue(saved, solvis, this.getControlAccess(solvis), false);
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
						+ "> not possible, rejected. Check the control.xml!";
				logger.error(error);
				throw new LearningError(error);
			}
			boolean success = false;
			for (int repeat = 0; repeat < Constants.SET_REPEATS; ++repeat) {
				success = this.setValue(solvis, new SolvisData(data)) != null;
				if (success) {
					break;
				} else {
					AbortHelper.getInstance()
							.sleep(solvis.getSolvisDescription().getMiscellaneous().getUnsuccessfullWaitTime_ms());
				}
			}
		}
	}

	@Override
	public Type getType() {
		return IChannelSource.Type.CONTROL;
	}

	@Override
	public Screen getScreen(int configurationMask) {
		return this.guiAccess.getScreen().get(configurationMask);
	}

	@Override
	public Collection<? extends IMode> getModes() {
		return this.strategy.getModes();
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return this.strategy.getUpperLowerStep();
	}

	@Override
	public boolean isBoolean() {
		return this.strategy.isBoolean();
	}

	@Override
	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeError {
		return this.strategy.interpretSetData(singleData);
	}

	static class GuiAccess implements IAssigner, IControlAccess {
		private final String screenId;
		private final Rectangle valueRectangle;
		private final String preparationId;
		private final String restoreChannelId;

		private OfConfigs<Screen> screen = null;
		private Preparation preparation = null;
		private OfConfigs<ChannelDescription> restoreChannel = null;

		private GuiAccess(String screenId, Rectangle valueRectangle, String preparationId, String restoreChannelId) {
			this.screenId = screenId;
			this.valueRectangle = valueRectangle;
			this.preparationId = preparationId;
			this.restoreChannelId = restoreChannelId;
		}

		Rectangle getValueRectangle() {
			return this.valueRectangle;
		}

		private OfConfigs<Screen> getScreen() {
			return this.screen;
		}

		private Preparation getPreparation() {
			return this.preparation;
		}

		private static class Creator extends CreatorByXML<GuiAccess> {

			private String screenId;
			private Rectangle valueRectangle;
			private String preparationId;
			private String restoreChannelId;

			private Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				switch (name.getLocalPart()) {
					case "screenId":
						this.screenId = value;
						break;
					case "restoreChannelId":
						this.restoreChannelId = value;
						break;
				}

			}

			@Override
			public GuiAccess create() throws XmlError, IOException {
				return new GuiAccess(this.screenId, this.valueRectangle, this.preparationId, this.restoreChannelId);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				String id = name.getLocalPart();
				switch (id) {
					case XML_CONTROL_CURRENT:
						return new Rectangle.Creator(id, this.getBaseCreator());
					case XML_PREPARATION_REF:
						return new PreparationRef.Creator(id, getBaseCreator());
				}
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
				switch (creator.getId()) {
					case XML_CONTROL_CURRENT:
						this.valueRectangle = (Rectangle) created;
						break;
					case XML_PREPARATION_REF:
						this.preparationId = ((PreparationRef) created).getPreparationId();
						break;
				}

			}

		}

		@Override
		public void assign(SolvisDescription description) throws ReferenceError {
			this.screen = description.getScreens().get(this.screenId);
			if (this.screen == null) {
				throw new ReferenceError("Screen of reference < " + this.screenId + " > not found");
			}

			if (this.preparationId != null) {
				this.preparation = description.getPreparations().get(this.preparationId);
				if (this.preparation == null) {
					throw new ReferenceError("Preparation of reference < " + this.preparationId + " > not found");
				}
			}

			if (this.restoreChannelId != null) {
				this.restoreChannel = description.getChannelDescriptions().get(this.restoreChannelId);
				if (this.restoreChannel == null) {
					throw new ReferenceError("Channel < " + this.restoreChannelId + " > not found");
				}
			}

		}

		boolean prepare(Solvis solvis) throws IOException, TerminationException {
			if (this.preparation == null) {
				return true;
			} else {
				return this.preparation.execute(solvis);
			}
		}

		@Override
		public boolean isModbus() {
			return false;
		}

	}

	private IControlAccess getControlAccess(Solvis solvis) {
		if (this.isModbus(solvis)) {
			return this.modbusAccess;
		} else {
			return this.guiAccess;
		}
	}

	@Override
	public boolean isModbus(Solvis solvis) {
		return solvis.getUnit().isModbus() && this.modbusAccess != null && this.modbusAccess.isModbus();
	}

	@Override
	public ChannelDescription getRestoreChannel(Solvis solvis) {
		if (this.isModbus(solvis)) {
			return null;
		}
		if (this.guiAccess.restoreChannel != null) {
			return this.guiAccess.restoreChannel.get(solvis);
		}
		return null;
	}

	@Override
	protected SingleData<?> createSingleData(String value) throws TypeError {
		return this.strategy.createSingleData(value);
	}

}
