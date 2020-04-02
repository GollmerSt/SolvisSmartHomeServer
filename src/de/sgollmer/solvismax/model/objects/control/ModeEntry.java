/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.Assigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.Helper;

public class ModeEntry implements Assigner, ModeI {

	private static final String XML_GUI_SET = "GuiSet";
	private static final String XML_MODBUS_VALUE = "ModbusValue";
	private static final String XML_TOUCH = "Touch";
	private static final String XML_SCREEN_GRAFIC = "ScreenGrafic";

	private final String id;
	private final GuiSet guiSet;
	private final Integer modbusValue;

	public ModeEntry(String id, GuiSet guiSet, int modbusValue) {
		this.id = id;
		this.guiSet = guiSet;
		this.modbusValue = modbusValue;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}

	public GuiSet getGuiSet() {
		return this.guiSet;
	}

	@Override
	public void assign(SolvisDescription description) {
		if (this.guiSet != null) {
			this.guiSet.assign(description);
		}

	}

	public static class Creator extends CreatorByXML<ModeEntry> {

		private String id;
		private GuiSet guiSet;
		private int modbusValue;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
			}

		}

		@Override
		public ModeEntry create() throws XmlError, IOException {
			return new ModeEntry(this.id, this.guiSet, this.modbusValue);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_GUI_SET:
					return new GuiSet.Creator(id, this.getBaseCreator());
				case XML_MODBUS_VALUE:
					return new Helper.IntegerValue.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_GUI_SET:
					this.guiSet = (GuiSet) created;
					break;
				case XML_MODBUS_VALUE:
					this.modbusValue = ((Helper.IntegerValue) created).toInteger();
					break;
			}
		}

	}

	@Override
	public String getName() {
		return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ModeEntry) {
			return this.id.equals(((ModeEntry) obj).id);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	public int getModbusValue() {
		return this.modbusValue;
	}

	public static class GuiSet implements Assigner {
		private final TouchPoint touch;
		private final ScreenGraficDescription grafic;

		public GuiSet(TouchPoint touch, ScreenGraficDescription grafic) {
			this.touch = touch;
			this.grafic = grafic;
		}

		@Override
		public void assign(SolvisDescription description) {
			if (this.touch != null) {
				this.touch.assign(description);
			}
		}

		public TouchPoint getTouch() {
			return this.touch;
		}

		public ScreenGraficDescription getGrafic() {
			return this.grafic;
		}

		public static class Creator extends CreatorByXML<GuiSet> {

			private TouchPoint touch;
			private ScreenGraficDescription grafic;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
			}

			@Override
			public GuiSet create() throws XmlError, IOException {
				return new GuiSet(this.touch, this.grafic);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				String id = name.getLocalPart();
				switch (id) {
					case XML_TOUCH:
						return new TouchPoint.Creator(id, this.getBaseCreator());
					case XML_SCREEN_GRAFIC:
						return new ScreenGraficDescription.Creator(id, this.getBaseCreator());
				}
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
				switch (creator.getId()) {
					case XML_TOUCH:
						this.touch = (TouchPoint) created;
						break;
					case XML_SCREEN_GRAFIC:
						this.grafic = (ScreenGraficDescription) created;
						break;
				}
			}

		}

	}

	public boolean isXmlValid(boolean modbus) {
		return !modbus && this.guiSet != null || modbus & this.modbusValue != null;
	}

}
