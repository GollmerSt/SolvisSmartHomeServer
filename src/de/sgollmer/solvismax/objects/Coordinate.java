/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.objects;

import javax.xml.namespace.QName;

import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Coordinate implements Cloneable {
	private final int x;
	private final int y;

	public Coordinate(final int x, final int y) {
		this.x = x;
		this.y = y;
	}

	public static class Creator extends CreatorByXML<Coordinate> {

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		private Integer x = null;
		private Integer y = null;

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "X":
					this.x = Integer.parseInt(value);
					break;
				case "Y":
					this.y = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public Coordinate create() throws XmlException {
			if (this.x == null || this.y == null) {
				throw new XmlException("Coordinates missing");
			}
			return new Coordinate(this.x, this.y);
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {

		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			return null;
		}

	}

	@Override
	public Coordinate clone() {
		try {
			return (Coordinate) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return the x
	 */
	public int getX() {
		return this.x;
	}

	/**
	 * @return the y
	 */
	public int getY() {
		return this.y;
	}

	public Coordinate add(final Coordinate coord) {
		return new Coordinate(this.getX() + coord.getX(), this.getY() + coord.getY());
	}

	public Coordinate diff(final Coordinate coord) {
		return new Coordinate(this.getX() - coord.getX(), this.getY() - coord.getY());
	}

	public Coordinate div(final int value) {
		return new Coordinate(this.getX() / value, this.getY() / value);
	}

	public Coordinate increment() {
		return new Coordinate(this.getX() + 1, this.getY() + 1);
	}

	public Coordinate decrement() {
		return new Coordinate(this.getX() - 1, this.getY() - 1);
	}

	@Override
	public boolean equals(final Object coord) {
		if (!(coord instanceof Coordinate)) {
			return false;
		}
		Coordinate cmp = (Coordinate) coord;
		return this.getX() == cmp.getX() && this.getY() == cmp.getY();
	}

	@Override
	public int hashCode() {
		return 409 + 761 * Integer.hashCode(this.x) + Integer.hashCode(this.y);
	}

	public boolean approximately(final Coordinate coord, int n) {
		if (coord == null) {
			return false;
		}

		int diffX = this.getX() - coord.getX();
		int diffY = this.getY() - coord.getY();
		diffX = diffX > 0 ? diffX : -diffX;
		diffY = diffY > 0 ? diffY : -diffY;

		return diffX <= n && diffY <= n;
	}

	@Override
	public String toString() {
		return "(" + this.x + "|" + this.y + ")";
	}

}
