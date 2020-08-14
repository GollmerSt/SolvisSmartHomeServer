/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ModbusException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.modbus.ModbusAccess;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.Status;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control.GuiAccess;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class StrategyMode implements IStrategy {

	private static final ILogger logger = LogManager.getInstance().getLogger(StrategyMode.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private static final String XML_MODE_ENTRY = "ModeEntry";

	private final List<ModeEntry> modes;

	private StrategyMode(List<ModeEntry> modes) {
		this.modes = modes;
	}

	@Override
	public boolean isWriteable() {
		return true;
	}

	@Override
	public ModeValue<ModeEntry> getValue(SolvisScreen source, Solvis solvis, IControlAccess controlAccess,
			boolean optional) throws IOException, ModbusException {
		if (controlAccess instanceof GuiAccess) {
			Rectangle rectangle = ((GuiAccess) controlAccess).getValueRectangle();
			MyImage image = new MyImage(source.getImage(), rectangle, true);
			Pattern pattern = null;
			for (ModeEntry mode : this.modes) {
				ScreenGraficDescription cmp = mode.getGuiSet().getGrafic();
				MyImage current = image;
				if (!cmp.isExact()) {
					if (pattern == null) {
						pattern = new Pattern(source.getImage(), rectangle);
					}
					current = pattern;
				}
				if (cmp.isElementOf(current, source.getSolvis(), true)) {
					return new ModeValue<ModeEntry>(mode, System.currentTimeMillis());
				}
			}
			return null;
		} else {
			int num = solvis.readUnsignedShortModbusData((ModbusAccess) controlAccess);
			for (ModeEntry mode : this.modes) {
				if (mode.getModbusValue() == num) {
					return new ModeValue<ModeEntry>(mode, System.currentTimeMillis());
				}
			}
			logger.error("Error: Unknown modbus mode value <" + num + ">. Check the control.xml");
			return null;
		}
	}

	@Override
	public SetResult setValue(Solvis solvis, IControlAccess controlAccess, SolvisData value)
			throws IOException, TerminationException, TypeException, ModbusException {
		if (value.getMode() == null) {
			throw new TypeException("Wrong value type");
		}
		IMode valueMode = value.getMode().get();
		ModeValue<ModeEntry> cmp = this.getValue(solvis.getCurrentScreen(), solvis, controlAccess, false);
		if (cmp != null && value.getMode().equals(cmp)) {
			return new SetResult(Status.SUCCESS, cmp);
		}
		ModeEntry mode = null;
		for (ModeEntry m : this.modes) {
			if (valueMode.equals(m)) {
				mode = m;
				break;
			}
		}
		if (mode == null) {
			throw new TypeException("Unknown value");
		}
		if (!controlAccess.isModbus()) {
			solvis.send(mode.getGuiSet().getTouch());
		} else {
			solvis.writeUnsignedShortModbusData((ModbusAccess) controlAccess, mode.getModbusValue());
		}
		return null;
	}

	@Override
	public List<ModeEntry> getModes() {
		return this.modes;
	}

	@Override
	public Integer getDivisor() {
		return null;
	}

	static class Creator extends CreatorByXML<StrategyMode> {

		private final List<ModeEntry> modes = new ArrayList<>(5);

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {

		}

		@Override
		public StrategyMode create() throws XmlException {
			return new StrategyMode(this.modes);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_MODE_ENTRY:
					return new ModeEntry.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_MODE_ENTRY:
					this.modes.add((ModeEntry) created);
					break;
			}

		}

	}

	@Override
	public void assign(SolvisDescription description) throws AssignmentException {
		for (ModeEntry mode : this.modes) {
			mode.assign(description);
		}
	}

	@Override
	public Double getAccuracy() {
		return null;
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return null;
	}

	@Override
	public void setCurrentRectangle(Rectangle rectangle) {
		for (ModeEntry mode : this.modes) {
			if (mode.getGuiSet().getGrafic() != null)
				mode.getGuiSet().getGrafic().setRectangle(rectangle);
		}
	}

	@Override
	public boolean mustBeLearned() {
		return true;
	}

	@Override
	public boolean learn(Solvis solvis, IControlAccess controlAccess) throws ModbusException, TerminationException {
		boolean successfull = true;
		for (ModeEntry mode : this.getModes()) {
			try {
				solvis.send(mode.getGuiSet().getTouch());
				ScreenGraficDescription grafic = mode.getGuiSet().getGrafic();
				SolvisScreen currentScreen = solvis.getCurrentScreen();
				currentScreen.writeLearningImage(currentScreen.get().getId() + "__" + grafic.getId());
				grafic.learn(solvis);
				grafic.writeLearningImage(solvis);
				solvis.clearCurrentScreen();
				SingleData<ModeEntry> data = this.getValue(solvis.getCurrentScreen(), solvis, controlAccess, false);
				if (data == null || !mode.equals(data.get())) {
					logger.log(LEARN, "Learning of <" + mode.getId() + "> not successfull, will be retried");
					successfull = false;
					break;
				}
			} catch (IOException e) {
				successfull = false;
				logger.log(LEARN, "Learning of <" + mode.getId() + "> not successfull (IOError), will be retried");
			}
		}
		if (successfull) {
			for (ListIterator<ModeEntry> itO = this.getModes().listIterator(); itO.hasNext();) {
				ModeEntry modeO = itO.next();
				for (ListIterator<ModeEntry> itI = this.getModes().listIterator(itO.nextIndex()); itI.hasNext();) {
					ModeEntry modeI = itI.next();
					if (modeO.getGuiSet().getGrafic().equals(modeI.getGuiSet().getGrafic())) {
						logger.log(LEARN,
								"Learning of <" + modeI.getId() + "> not successfull (not unique), will be retried");
						successfull = false;
						break;
					}
				}
			}
		}
		if (!successfull) {
			for (ModeEntry mode : this.getModes()) {
				solvis.getGrafics().remove(mode.getGuiSet().getGrafic().getId());
			}
		}
		solvis.clearCurrentScreen();
		return successfull;
	}

	@Override
	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeException {
		return this.interpretSetData(singleData.toString());
	}

	private SingleData<?> interpretSetData(String value) throws TypeException {
		IMode setMode = null;
		for (IMode mode : this.getModes()) {
			if (mode.getName().equals(value)) {
				setMode = mode;
				break;
			}
		}
		if (setMode == null) {
			throw new TypeException("Mode <" + value + "> is unknown");
		}
		return new ModeValue<IMode>(setMode, -1);
	}

	@Override
	public boolean isXmlValid(boolean modbus) {
		boolean result = true;
		for (ModeEntry entry : this.modes) {
			result &= entry.isXmlValid(modbus);
		}
		return result;
	}

	@Override
	public boolean isBoolean() {
		return false;
	}

	@Override
	public SingleData<?> createSingleData(String value) throws TypeException {
		return this.interpretSetData(value);
	}
}
