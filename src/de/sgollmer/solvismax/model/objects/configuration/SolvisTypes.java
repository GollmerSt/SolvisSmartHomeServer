/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class SolvisTypes {

	private static final String XML_TYPE = "Type";
	private final Collection<Type> types;

	public SolvisTypes(Collection<Type> types) {
		this.types = types;
	}

	public static class Creator extends CreatorByXML<SolvisTypes> {

		private Collection<Type> types = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public SolvisTypes create() throws XmlError, IOException {
			return new SolvisTypes(this.types);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_TYPE:
					return new Type.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_TYPE:
					this.types.add((Type) created);
					break;
			}

		}

	}

	private static class Type {
		private final String id;
		private final int configuration;

		public Type(String is, int configuration) {
			this.id = is;
			this.configuration = configuration;
		}

		public static class Creator extends CreatorByXML<Type> {

			private String id;
			private int configuration;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				switch (name.getLocalPart()) {
					case "id":
						this.id = value;
						break;
					case "configuration":
						if (value.startsWith("0x")) {
							value = value.substring(2);
						}
						this.configuration = Integer.parseInt(value, 16);
						break;
				}
			}

			@Override
			public Type create() throws XmlError, IOException {
				return new Type(this.id, this.configuration);
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

	public int getConfiguration(String typeString) {
		for (Type type : this.types) {
			if (type.id.equals(typeString)) {
				return type.configuration;
			}
		}
		return 0;
	}
}
