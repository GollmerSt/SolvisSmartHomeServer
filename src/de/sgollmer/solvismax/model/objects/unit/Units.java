/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.unit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Units {

	private static final String XML_UNITS_UNIT = "Unit";

	private final Collection<Unit> units;
	private boolean mailEnabled = false;

	private Units(final Collection<Unit> units) {
		this.units = units;
		for (Unit unit : units) {
			this.mailEnabled |= unit.isMailEnabled();
		}
	}

	public Collection<Unit> getUnits() {
		return this.units;
	}

	public boolean isMailEnabled() {
		return this.mailEnabled;
	}

	public static class Creator extends CreatorByXML<Units> {

		private final Collection<Unit> units = new ArrayList<>();

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
		}

		@Override
		public Units create() throws XmlException, IOException {
			return new Units(this.units);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_UNITS_UNIT:
					return new Unit.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_UNITS_UNIT:
					this.units.add((Unit) created);
					break;
			}
		}

	}
}
