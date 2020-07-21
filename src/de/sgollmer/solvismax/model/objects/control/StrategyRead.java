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
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.control.Control.GuiAccess;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class StrategyRead implements IStrategy {

	private static final String XML_GUI_READ = "GuiRead";

	protected final int divisor;
	private final GuiRead guiRead;

	protected StrategyRead(int divisor, GuiRead guiRead) {
		this.divisor = divisor;
		this.guiRead = guiRead;
	}

	@Override
	public boolean isWriteable() {
		return false;
	}

	@Override
	public IntegerValue getValue(SolvisScreen screen, Solvis solvis, IControlAccess controlAccess, boolean optional)
			throws IOException {
		Integer i;
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
				i = Integer.parseInt(formated);
			}
		} else {
			i = solvis.readUnsignedShortModbusData((ModbusAccess) controlAccess);
		}
		return new IntegerValue(i, System.currentTimeMillis());
	}

	@Override
	public SingleData<?> setValue(Solvis solvis, IControlAccess controlAccess, SolvisData value) throws IOException {
		return null;
	}

	@Override
	public Integer getDivisor() {
		return this.divisor;
	}

	static class Creator extends CreatorByXML<StrategyRead> {

		private int divisor = 1;
		private GuiRead guiRead = null;

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "divisor":
					this.divisor = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public StrategyRead create() throws XmlError {
			return new StrategyRead(this.divisor, this.guiRead);
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
	public Double getAccuracy() {
		return (double) 1 / (double) this.getDivisor();
	}

	@Override
	public List<? extends IMode> getModes() {
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
	public boolean learn(Solvis solvis, IControlAccess controlAccess) {
		return true;
	}

	@Override
	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeError {
		return null;
	}

	protected static class GuiRead {
		protected final Format format;

		protected GuiRead(String format) {
			this.format = new Format(format);
		}

		private Format getFormat() {
			return this.format;
		}

		private static class Creator extends CreatorByXML<GuiRead> {
			private String format;

			private Creator(String id, BaseCreator<?> creator) {
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

	@Override
	public boolean isBoolean() {
		return false;
	}

	@Override
	public SingleData<?> createSingleData(String value) {
		return null;
	}

}
