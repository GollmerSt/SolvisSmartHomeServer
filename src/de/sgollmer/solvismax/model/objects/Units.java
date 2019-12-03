package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Units {
	
	private static final String XML_UNITS_UNIT = "Unit";

	private final Collection<Unit> units;
	
	public Units( Collection<Unit> units ) {
		this.units = units ;
	}
	
	public Collection<Unit> getUnits() {
		return units;
	}

	public static class  Creator extends CreatorByXML<Units> {
		
		private final Collection<Unit> units = new ArrayList<>() ;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public Units create() throws XmlError, IOException {
			return new Units(units);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart() ;
			switch (id) {
				case XML_UNITS_UNIT:
					return new Unit.Creator(id, this.getBaseCreator()) ;
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId() ) {
				case XML_UNITS_UNIT:
					this.units.add((Unit) created) ;
					break ;
			}
		}
		
	}

	public static class Unit {

		private final String url;
		private final String account;
		private final String password;

		public Unit(String url, String account, String password) {
			this.url = url;
			this.account = account;
			this.password = password;
		}

		public String getUrl() {
			return url;
		}

		public String getAccount() {
			return account;
		}

		public String getPassword() {
			return password;
		}

		public static class Creator extends CreatorByXML<Unit> {

			private String url;
			private String account;
			private String password;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				switch ( name.getLocalPart() ) {
					case "url":
						this.url = value ;
						break ;
					case "account":
						this.account = value ;
						break ;
					case "password":
						this.password = value ;
				}

			}

			@Override
			public Unit create() throws XmlError, IOException {
				return new Unit(url, account, password);
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
}
