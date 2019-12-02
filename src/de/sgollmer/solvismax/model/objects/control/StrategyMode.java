package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Mode;
import de.sgollmer.solvismax.model.objects.ScreenGraficDescription;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class StrategyMode implements Strategy {

	private final Collection<Mode> modes;

	public StrategyMode(Collection<Mode> modes) {
		this.modes = modes;
	}

	@Override
	public boolean isWriteable() {
		return true;
	}

	@Override
	public SingleData getValue(MyImage source, Rectangle rectangle, Solvis solvis) {
		MyImage image = new MyImage(source, rectangle, true);
		Pattern pattern = null;
		for (Mode mode : this.modes) {
			ScreenGraficDescription cmp = mode.getGrafic();
			MyImage current = image;
			if (!cmp.isExact()) {
				if (pattern == null) {
					pattern = new Pattern(source, rectangle);
				}
				current = pattern;
			}
			if (cmp.isElementOf(current, solvis,true)) {
				return new ModeValue<Mode>(mode);
			}
		}
		return null;
	}

	@Override
	public Boolean setValue(Solvis solvis, Rectangle rectangle, SolvisData value) throws IOException {
		SingleData cmp = this.getValue(solvis.getCurrentImage(), rectangle, solvis);
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
			return null;
		}
		solvis.send(mode.getTouch());
		return false;
	}

	public Collection<Mode> getModes() {
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

		private final Collection<Mode> modes = new ArrayList<>(5);

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
				case "Mode":
					return new Mode.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case "Mode":
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
}
