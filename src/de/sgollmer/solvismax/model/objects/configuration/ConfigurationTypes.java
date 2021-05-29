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

import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class ConfigurationTypes {

	private static final String XML_TYPE = "Type";
	private final Collection<Type> types;

	private ConfigurationTypes(final Collection<Type> types) {
		this.types = types;
	}

	public static class Creator extends CreatorByXML<ConfigurationTypes> {

		private Collection<Type> types = new ArrayList<>();

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
		}

		@Override
		public ConfigurationTypes create() throws XmlException, IOException {
			return new ConfigurationTypes(this.types);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_TYPE:
					return new Type.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_TYPE:
					this.types.add((Type) created);
					break;
			}

		}

	}

	public static class Type {
		private final String id;
		private final long configuration;
		private final boolean dontCare;

		public Type(final String is, final long configuration, final boolean dontCare) {
			this.id = is;
			this.configuration = configuration;
			this.dontCare = dontCare;
		}

		@Override
		public String toString() {
			return Long.toHexString(this.configuration);
		}

		private static class Creator extends CreatorByXML<Type> {

			private String id;
			private long configuration;
			private boolean dontCare;

			private Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) {
				switch (name.getLocalPart()) {
					case "id":
						this.id = value;
						break;
					case "configuration":
						this.configuration = Long.decode(value);
						break;
					case "dontCare":
						this.dontCare = Boolean.parseBoolean(value);
						break;
				}
			}

			@Override
			public Type create() throws XmlException, IOException {
				return new Type(this.id, this.configuration, this.dontCare);
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
				return null;
			}

			@Override
			public void created(final CreatorByXML<?> creator, final Object created) {
			}

		}

		public String getId() {
			return this.id;
		}

		public long getConfiguration() {
			return this.configuration;
		}

		public boolean isDontCare() {
			return this.dontCare;
		}
	}

	public long getConfiguration(final String typeString) {
		for (Type type : this.types) {
			if (type.id.equals(typeString)) {
				return type.configuration;
			}
		}
		return 0;
	}

	public Collection<Type> getTypes() {
		return this.types;
	}
}
