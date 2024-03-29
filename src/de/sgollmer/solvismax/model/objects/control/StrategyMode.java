/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants.Csv;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.SolvisDataHelper;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.control.Control.GuiAccess;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class StrategyMode extends AbstractStrategy {

	private static final ILogger logger = LogManager.getInstance().getLogger(StrategyMode.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private static final String XML_MODE_ENTRY = "ModeEntry";

	private final List<ModeEntry> modes;
	private final boolean sequence;
	private int learnWaitFactor = 1;

	private StrategyMode(final List<ModeEntry> modes, final boolean sequence) {
		this.modes = modes;
		this.sequence = sequence;
	}

	@Override
	public boolean isWriteable() {
		return true;
	}

	@Override
	public ModeValue<ModeEntry> getValue(final SolvisScreen source, final Solvis solvis, boolean optional)
			throws IOException {
		GuiAccess access = this.getControl().getGuiAccess();
		Rectangle rectangle = access.getValueRectangle();
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

	}

	@Override
	public SetResult setValue(final Solvis solvis, final SolvisData value)
			throws IOException, TerminationException, TypeException {
		if (value.getMode() == null) {
			throw new TypeException("Wrong value type");
		}
		ModeValue<ModeEntry> cmp = this.getValue(solvis.getCurrentScreen(), solvis, false);
		if (cmp != null && value.getMode().equals(cmp)) {
			return new SetResult(ResultStatus.SUCCESS, cmp, true);
		}
		IMode<?> valueMode = value.getMode().get();
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

		int touches = this.sequence ? this.modes.size() : 1;

		for (int i = 0; i < touches; ++i) {
			solvis.send(mode.getGuiSet().getTouch());
			cmp = this.getValue(solvis.getCurrentScreen(), solvis, false);
			if (cmp != null && value.getMode().equals(cmp)) {
				return new SetResult(ResultStatus.SUCCESS, cmp, true);
			}
		}

		return null;
	}

	@Override
	public SetResult setValueFast(final Solvis solvis, final SolvisData value) {
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
		private boolean sequence = false;

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {

		}

		@Override
		public StrategyMode create() throws XmlException {
			return new StrategyMode(this.modes, this.sequence);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_MODE_ENTRY:
					return new ModeEntry.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_MODE_ENTRY:
					ModeEntry entry = (ModeEntry) created;
					boolean byWhite = !entry.getWhiteGraficRectangles().isEmpty();
					if (!this.modes.isEmpty() && this.sequence != byWhite) {
						logger.error("MustBeWhite definitions of mode <" + this.getId()
								+ "> not complete. White space detection ignored.");
						this.sequence = false;
					} else {
						this.sequence = byWhite;
					}
					this.modes.add(entry);

					break;
			}

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
	public boolean mustBeLearned() {
		return true;
	}

	private boolean learnByButton(final Solvis solvis) throws TerminationException {
		boolean successfull = true;
		ModeEntry mode = this.getModes().get(this.getModes().size() - 1);
		TouchPoint preTouch = mode.getGuiSet().getTouch();
		try {
			solvis.send(preTouch);

			for (Iterator<ModeEntry> it = this.getModes().iterator(); it.hasNext();) {
				mode = it.next();
				SolvisScreen currentScreen = solvis.getCurrentScreen();
				ScreenGraficDescription grafic = mode.getGuiSet().getGrafic();
				MyImage former = grafic.getImage(solvis);
				TouchPoint touch = mode.getGuiSet().getTouch();
				solvis.send(touch);
				AbortHelper.getInstance().sleep(touch.getReleaseTime(solvis) * (this.learnWaitFactor - 1));
				boolean changed = !former.equals(grafic.getImage(solvis));
				if (!changed) {
					logger.learn("Mode symbol not changed after touch, it will be waited a little longer.");
					AbortHelper.getInstance().sleep(touch.getReleaseTime(solvis));
					++this.learnWaitFactor;
					solvis.clearCurrentScreen();
				}
				currentScreen = solvis.getCurrentScreen();
				solvis.writeLearningImage(currentScreen, currentScreen.get().getId() + "__" + grafic.getId());
				grafic.learn(solvis);
				solvis.clearCurrentScreen();
				SingleData<ModeEntry> data = this.getValue(solvis.getCurrentScreen(), solvis, false);
				if (data == null || !mode.equals(data.get())) {
					logger.log(LEARN, "Learning of <" + mode.getId() + "> not successfull, will be retried");
					successfull = false;
					break;
				}
			}
		} catch (IOException e) {
			successfull = false;
			logger.log(LEARN, "Learning of <" + mode.getId() + "> not successfull (IOError), will be retried");
		}
		return successfull;
	}

	private boolean learnByWhiteRectangles(final Solvis solvis) throws TerminationException {
		boolean successfull = true;
		List<ModeEntry> toLearn = new ArrayList<>(this.getModes());
		try {
			boolean abort = false;
			while (!toLearn.isEmpty() && !abort) {
				solvis.send(toLearn.get(0).getGuiSet().getTouch());
				boolean found = false;
				SolvisScreen currentScreen = solvis.getCurrentScreen();
				for (Iterator<ModeEntry> it = toLearn.iterator(); it.hasNext();) {
					ModeEntry mode = it.next();
					if (mode.isMatchingWhite(currentScreen.getImage(), solvis)) {
						ScreenGraficDescription grafic = mode.getGuiSet().getGrafic();
						solvis.writeLearningImage(currentScreen, currentScreen.get().getId() + "__" + grafic.getId());
						grafic.learn(solvis);
						solvis.clearCurrentScreen();
						SingleData<ModeEntry> data = this.getValue(solvis.getCurrentScreen(), solvis, false);
						if (data == null || !mode.equals(data.get())) {
							logger.log(LEARN, "Learning of <" + mode.getId() + "> not successfull, will be retried");
							successfull = false;
							abort = true;
						} else {
							found = true;
							it.remove();
						}
						break;
					}
				}
				if (!found) {
					logger.log(LEARN, "Learning of <" + toLearn.get(0).getId() + "> not successfull, will be retried");
					successfull = false;
					abort = true;
				}
			}
		} catch (IOException e) {
			successfull = false;
			logger.log(LEARN,
					"Learning of <" + toLearn.get(0).getId() + "> not successfull (IOError), will be retried");

		}
		return successfull;
	}

	@Override
	public boolean learn(final Solvis solvis) throws TerminationException {
		boolean successfull = true;
		if (this.sequence) {
			successfull = learnByWhiteRectangles(solvis);
		} else {
			successfull = learnByButton(solvis);
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
	public SingleData<?> interpretSetData(final SingleData<?> singleData, final boolean debug) throws TypeException {
		return this.interpretSetData(singleData.toString(), singleData.getTimeStamp(), debug);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private SingleData<?> interpretSetData(final String value, final long timeStamp, final boolean debug)
			throws TypeException {
		return SolvisDataHelper.toMode(value, timeStamp, (Collection) this.getModes());
	}

	@Override
	public boolean isXmlValid() {
		boolean result = true;
		for (ModeEntry entry : this.modes) {
			result &= entry.isXmlValid();
		}
		return result;
	}

	@Override
	public boolean isBoolean() {
		return false;
	}

	@Override
	public String getCsvMeta(final String column, final boolean semicolon) {
		switch (column) {
			case Csv.MODES:
				StringBuilder builder = new StringBuilder();
				for (ModeEntry entry : this.modes) {
					if (builder.length() != 0) {
						builder.append('|');
					}
					builder.append(entry.getCvsMeta());
				}
				return builder.toString();
			case Csv.WRITE:
				return "true";
		}
		return null;
	}

	@Override
	public void setControl(final Control control) {
		super.setControl(control);
		Rectangle rectangle = control.getGuiAccess().getValueRectangle();
		for (ModeEntry mode : this.modes) {
			if (mode.getGuiSet().getGrafic() != null)
				mode.getGuiSet().getGrafic().setRectangle(rectangle);
		}

	}

	@Override
	public SetResult setDebugValue(Solvis solvis, SingleData<?> value) {
		return new SetResult(ResultStatus.SUCCESS, value, false);
	}

	@Override
	public void instantiate(Solvis solvis) {

	}

	@Override
	public boolean inhibitGuiReadAfterWrite() {
		return false;
	}

}
