/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.objects;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class Rectangle {

	private static final String XML_TOP_LEFT = "TopLeft";
	private static final String XML_BOTTOM_RIGHT = "BottomRight";

	private final boolean invertFunction;
	private final Coordinate topLeft;
	private final Coordinate bottomRight;

	public Rectangle(Coordinate topLeft, Coordinate bottomRight) {
		this(false, topLeft, bottomRight);
	}

	public Rectangle(boolean invertFunction, Coordinate topLeft, Coordinate bottomRight) {
		this.invertFunction = invertFunction;
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
	}

	/**
	 * @return the topLeft
	 */
	public Coordinate getTopLeft() {
		return this.topLeft;
	}

	/**
	 * @return the bottomRight
	 */
	public Coordinate getBottomRight() {
		return this.bottomRight;
	}

	public static class Creator extends CreatorByXML<Rectangle> {
		private boolean invertFunction = false;
		private Coordinate topLeft;
		private Coordinate bottomRight;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "invertFunction":
					this.invertFunction = Boolean.parseBoolean(value);
					break;
			}
		}

		@Override
		public Rectangle create() throws XmlError {
			return new Rectangle(this.invertFunction, this.topLeft, this.bottomRight);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_TOP_LEFT:
				case XML_BOTTOM_RIGHT:
					return new Coordinate.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_TOP_LEFT:
					this.topLeft = (Coordinate) created;
					break;
				case XML_BOTTOM_RIGHT:
					this.bottomRight = (Coordinate) created;
					break;
			}

		}

	}

	public boolean isIn(int x, int y) {
		return this.topLeft.getX() <= x && x <= this.getBottomRight().getX() //
				&& this.topLeft.getY() <= y && y <= this.bottomRight.getY();
	}

	public boolean isIn(Coordinate c) {
		return this.isIn(c.getX(), c.getY());
	}

	public Rectangle add(Coordinate origin) {
		return new Rectangle(this.invertFunction, this.getTopLeft().add(origin), this.getBottomRight().add(origin));
	}

	public boolean isInvertFunction() {
		return this.invertFunction;
	}

}
