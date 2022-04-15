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

import de.sgollmer.solvismax.Constants.Csv;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.Helper.Format;
import de.sgollmer.solvismax.helper.SolvisDataHelper;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.control.Control.GuiAccess;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class StrategyRead implements IStrategy {

	private static final String XML_GUI_READ = "GuiRead";

	protected final int divisor;
	private final GuiRead guiRead;

	protected StrategyRead(final int divisor, final GuiRead guiRead) {
		this.divisor = divisor;
		this.guiRead = guiRead;
	}

	@Override
	public boolean isWriteable() {
		return false;
	}

	@Override
	public IntegerValue getValue(final SolvisScreen screen, final Solvis solvis, final IControlAccess controlAccess,
			final boolean optional) throws IOException {
		Integer i = null;
		if (controlAccess instanceof GuiAccess) {
			Rectangle rectangle = ((GuiAccess) controlAccess).getValueRectangle();
			OcrRectangle ocr = new OcrRectangle(screen.getImage(), rectangle);
			String s = ocr.getString();
			String formated = this.guiRead.getFormat().getString(s);
			if (formated == null) {
				if (optional) {
					i = null;
				} else {
					return null;
				}
			} else {
				i = (int) Math.round(Double.parseDouble(formated) * this.getDivisor());
			}
		}
		return new IntegerValue(i, System.currentTimeMillis());
	}

	@Override
	public SetResult setValue(final Solvis solvis, final IControlAccess controlAccess, final SolvisData value)
			throws IOException, TerminationException, TypeException {
		return null;
	}

	@Override
	public SetResult setValueFast(final Solvis solvis, final SolvisData value) {
		return null;
	}

	@Override
	public Integer getDivisor() {
		return this.divisor;
	}

	static class Creator extends CreatorByXML<StrategyRead> {

		private int divisor = 1;
		private GuiRead guiRead = null;

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "divisor":
					this.divisor = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public StrategyRead create() throws XmlException {
			return new StrategyRead(this.divisor, this.guiRead);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_GUI_READ:
					return new GuiRead.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_GUI_READ:
					this.guiRead = (GuiRead) created;
					break;
			}
		}

	}

	@Override
	public Double getAccuracy() {
		return (double) 1 / (double) this.getDivisor();
	}

	@Override
	public List<? extends IMode<?>> getModes() {
		return null;
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return null;
	}

	@Override
	public boolean mustBeLearned() {
		return false;
	}

	@Override
	public boolean learn(final Solvis solvis, final IControlAccess controlAccess) {
		return true;
	}

	@Override
	public SingleData<?> interpretSetData(final SingleData<?> singleData, final boolean debug) throws TypeException {

		SingleData<?> value = SolvisDataHelper.toValue(singleData);

		if (value != null) {
			return new IntegerValue((int) Math.round(value.getDouble() * this.divisor), -1l);
		} else {
			return null;
		}
	}

	protected static class GuiRead {
		protected final Format format;

		protected GuiRead(final String format) {
			this.format = new Format(format);
		}

		private Format getFormat() {
			return this.format;
		}

		private static class Creator extends CreatorByXML<GuiRead> {
			private String format;

			private Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) {
				switch (name.getLocalPart()) {
					case "format":
						this.format = value;
						break;
				}

			}

			@Override
			public GuiRead create() throws XmlException, IOException {
				return new GuiRead(this.format);
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
				return null;
			}

			@Override
			public void created(final CreatorByXML<?> creator, final Object created) {
			}

		}

	}

	@Override
	public boolean isXmlValid() {
		return true;
	}

	@Override
	public boolean isBoolean() {
		return false;
	}

	@Override
	public String getCsvMeta(final String column, final boolean semicolon) {
		switch (column) {
			case Csv.DIVISOR:
				return Integer.toString(this.divisor);
		}
		return null;
	}

	@Override
	public void setControl(final Control control) {

	}

	@Override
	public SetResult setDebugValue(Solvis solvis, SingleData<?> value) throws TypeException {
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
