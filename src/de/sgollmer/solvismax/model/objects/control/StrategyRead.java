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
import de.sgollmer.solvismax.modbus.ModbusAccess;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelSourceI.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control.GuiAccess;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class StrategyRead implements Strategy {

	private static final String XML_GUI_READ = "GuiRead";

	protected final boolean optional;
	protected final int divisor;
	private final GuiRead guiRead;

	public StrategyRead(boolean optional, int divisor, GuiRead guiRead) {
		this.optional = optional;
		this.divisor = divisor;
		this.guiRead = guiRead;
	}

	@Override
	public boolean isWriteable() {
		return false;
	}

	@Override
	public IntegerValue getValue(SolvisScreen screen, Solvis solvis, ControlAccess controlAccess) throws IOException {
		Integer i;
		if (controlAccess instanceof GuiAccess) {
			Rectangle rectangle = ((GuiAccess) controlAccess).getValueRectangle();
			OcrRectangle ocr = new OcrRectangle(screen.getImage(), rectangle);
			String s = ocr.getString();
			s = this.guiRead.format.getString(s);
			if (s == null) {
				if (this.optional) {
					i = null;
				} else {
					return null;
				}
			} else {
				i = Integer.parseInt(s);
			}
		} else {
			i = solvis.readUnsignedShortModbusData((ModbusAccess) controlAccess);
		}
		return new IntegerValue(i, System.currentTimeMillis());
	}

	@Override
	public SingleData<?> setValue(Solvis solvis, ControlAccess controlAccess, SolvisData value) throws IOException {
		return null;
	}

	@Override
	public Integer getDivisor() {
		return this.divisor;
	}

	public static class Creator extends CreatorByXML<StrategyRead> {

		private boolean optional = false;
		private int divisor = 1;
		private GuiRead guiRead = null;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "optional":
					this.optional = Boolean.parseBoolean(value);
					break;
				case "divisor":
					this.divisor = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public StrategyRead create() throws XmlError {
			return new StrategyRead(this.optional, this.divisor, this.guiRead);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_GUI_READ:
					return new GuiRead.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_GUI_READ:
					this.guiRead = (GuiRead) created;
					break;
			}
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
	public boolean learn(Solvis solvis, ControlAccess controlAccess) {
		return true;
	}

	@Override
	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeError {
		return null;
	}

	public static class GuiRead {
		protected final Format format;

		public GuiRead(String format) {
			this.format = new Format(format);
		}

		public Format getFormat() {
			return this.format;
		}

		public static class Creator extends CreatorByXML<GuiRead> {
			private String format;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				switch (name.getLocalPart()) {
					case "format":
						this.format = value;
						break;
				}

			}

			@Override
			public GuiRead create() throws XmlError, IOException {
				return new GuiRead(this.format);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
			}

		}

	}

	@Override
	public boolean isXmlValid(boolean modbus) {
		return true;
	}

}
