/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.Helper.Format;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelSourceI.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class StrategyRead implements Strategy {
	private final boolean optional;
	private final Format format;
	private final int divisor;
	private final String unit;

	public StrategyRead(boolean optional, String format, int divisor, String unit) {
		this.optional = optional;
		this.format = new Format(format);
		this.divisor = divisor;
		this.unit = unit;
	}

	@Override
	public boolean isWriteable() {
		return false;
	}

	@Override
	public IntegerValue getValue(SolvisScreen screen, Rectangle rectangle) {
		OcrRectangle ocr = new OcrRectangle(screen.getImage(), rectangle);
		String s = ocr.getString();
		s = format.getString(s);
		Integer i;
		if (s == null) {
			if (this.optional) {
				i = null;
			} else {
				return null;
			}
		} else {
			i = Integer.parseInt(s);
		}
		return new IntegerValue(i, System.currentTimeMillis());
	}

	@Override
	public SingleData<?> setValue(Solvis solvis, Rectangle rectangle, SolvisData value) throws IOException {
		return null;
	}

	@Override
	public Integer getDivisor() {
		return this.divisor;
	}

	@Override
	public String getUnit() {
		return this.unit;
	}

	public static class Creator extends CreatorByXML<StrategyRead> {

		private boolean optional = false;
		private String format;
		private int divisor = 1;
		private String unit;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "optional":
					this.optional = Boolean.parseBoolean(value);
					break;
				case "format":
					this.format = value;
					break;
				case "unit":
					this.unit = value;
					break;
				case "divisor":
					this.divisor = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public StrategyRead create() throws XmlError {
			return new StrategyRead(optional, format, divisor, unit);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {

		}

	}

	@Override
	public void assign(SolvisDescription description) {

	}

	@Override
	public Float getAccuracy() {
		return (float) 1 / (float) this.getDivisor();
	}

	@Override
	public List<? extends ModeI> getModes() {
		return null;
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return null;
	}

	@Override
	public void setCurrentRectangle(Rectangle rectangle) {
	}

	@Override
	public boolean mustBeLearned() {
		return false;
	}

	@Override
	public boolean learn(Solvis solvis) {
		return true;
	}

	@Override
	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeError {
		return null;
	}

}
