/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.modbus;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.control.IControlAccess;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ModbusAccess implements IControlAccess {
	private final RegisterType type;
	private final int address;

	private ModbusAccess(RegisterType type, int address) {
		this.type = type;
		this.address = address;
	}

	public RegisterType getType() {
		return this.type;
	}

	public int getAddress() {
		return this.address;
	}

	public static class Creator extends CreatorByXML<ModbusAccess> {

		private RegisterType type = null;
		private int address;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "address":
					this.address = Integer.decode(value);
					break;
				case "type":
					this.type = RegisterType.get(value);
					break;
			}

		}

		@Override
		public ModbusAccess create() throws XmlError, IOException {
			return new ModbusAccess(this.type, this.address);
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
	public boolean isModbus() {
		return this.type != null;
	}

}
