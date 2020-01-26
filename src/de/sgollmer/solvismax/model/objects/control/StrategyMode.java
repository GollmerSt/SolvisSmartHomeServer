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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelSourceI.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class StrategyMode implements Strategy {

	private static final Logger logger = LogManager.getLogger(StrategyMode.class);
	private static final Level LEARN = Level.getLevel("LEARN");
	
	private static final String XML_MODE = "Mode" ; 

	private final List<Mode> modes;

	public StrategyMode(List<Mode> modes) {
		this.modes = modes;
	}

	@Override
	public boolean isWriteable() {
		return true;
	}

	@Override
	public ModeValue<Mode> getValue(SolvisScreen source, Rectangle rectangle) {
		MyImage image = new MyImage(source.getImage(), rectangle, true);
		Pattern pattern = null;
		for (Mode mode : this.modes) {
			ScreenGraficDescription cmp = mode.getGrafic();
			MyImage current = image;
			if (!cmp.isExact()) {
				if (pattern == null) {
					pattern = new Pattern(source.getImage(), rectangle);
				}
				current = pattern;
			}
			if (cmp.isElementOf(current, source.getSolvis(), true)) {
				return new ModeValue<Mode>(mode);
			}
		}
		return null;
	}

	@Override
	public boolean setValue(Solvis solvis, Rectangle rectangle, SolvisData value)
			throws IOException, TerminationException {
		ModeValue<Mode> cmp = this.getValue(solvis.getCurrentScreen(), rectangle);
		if (cmp != null && value.getMode().equals(cmp)) {
			return true;
		}
		Mode mode = null;
		for (Mode m : modes) {
			if (value.getMode().equals(new ModeValue<Mode>(m))) {
				mode = m;
				break;
			}
		}
		if (mode == null) {
			return false;
		}
		solvis.send(mode.getTouch());
		return false;
	}

	@Override
	public List<Mode> getModes() {
		return this.modes;
	}

	@Override
	public Integer getDivisor() {
		return null;
	}

	@Override
	public String getUnit() {
		return null;
	}

	public static class Creator extends CreatorByXML<StrategyMode> {

		private final List<Mode> modes = new ArrayList<>(5);

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {

		}

		@Override
		public StrategyMode create() throws XmlError {
			return new StrategyMode(this.modes);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_MODE:
					return new Mode.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_MODE:
					this.modes.add((Mode) created);
					break;
			}

		}

	}

	@Override
	public void assign(SolvisDescription description) {
		for (Mode mode : this.modes) {
			mode.assign(description);
		}
	}

	@Override
	public Float getAccuracy() {
		return null;
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return null;
	}

	@Override
	public void setCurrentRectangle(Rectangle rectangle) {
		for (Mode mode : this.modes) {
			mode.getGrafic().setRectangle(rectangle);
		}
	}

	@Override
	public boolean mustBeLearned() {
		return true;
	}

	@Override
	public boolean learn(Solvis solvis) throws IOException {
		boolean successfull = true;
		for (Mode mode : this.getModes()) {
			solvis.send(mode.getTouch());
			ScreenGraficDescription grafic = mode.getGrafic();
			grafic.learn(solvis);
			solvis.clearCurrentScreen();
			SingleData<Mode> data = this.getValue(solvis.getCurrentScreen(), grafic.getRectangle());
			if (data == null || !mode.equals(data.get())) {
				logger.log(LEARN, "Learning of <" + mode.getId() + "> not successfull, will be retried");
				successfull = false;
				break;
			}
		}
		for (ListIterator<Mode> itO = this.getModes().listIterator(); itO.hasNext();) {
			Mode modeO = itO.next();
			for (ListIterator<Mode> itI = this.getModes().listIterator(itO.nextIndex()); itI.hasNext();) {
				Mode modeI = itI.next();
				if (modeO.getGrafic().equals(modeI.getGrafic())) {
					logger.log(LEARN,
							"Learning of <" + modeI.getId() + "> not successfull (not unique), will be retried");
					break;
				}
			}
		}
		if ( !successfull ) {
			for ( Mode mode: this.getModes() ) {
				solvis.getGrafics().remove(mode.getGrafic().getId()) ;
			}
		}
		solvis.clearCurrentScreen();
		return successfull;
	}
}
