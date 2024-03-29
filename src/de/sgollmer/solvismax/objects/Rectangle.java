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

public class Rectangle {

	private static final String XML_TOP_LEFT = "TopLeft";
	private static final String XML_BOTTOM_RIGHT = "BottomRight";

	private final boolean invertFunction;
	private final Coordinate topLeft;
	private final Coordinate bottomRight;
	private final String name;

	public Rectangle(final Coordinate topLeft, final Coordinate bottomRight) {
		this(null, false, topLeft, bottomRight);
	}

	private Rectangle(final boolean invertFunction, final Coordinate topLeft, final Coordinate bottomRight) {
		this(null, invertFunction, topLeft, bottomRight);
	}

	private Rectangle(final String name, final boolean invertFunction, final Coordinate topLeft,
			final Coordinate bottomRight) {
		this.invertFunction = invertFunction;
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
		this.name = name;
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
		private final String name;

		public Creator(final String id, final BaseCreator<?> creator) {
			this(null, id, creator);
		}

		public Creator(final String name, final String id, final BaseCreator<?> creator) {
			super(id, creator);
			this.name = name;
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "invertFunction":
					this.invertFunction = Boolean.parseBoolean(value);
					break;
			}
		}

		@Override
		public Rectangle create() throws XmlException {
			return new Rectangle(this.name, this.invertFunction, this.topLeft, this.bottomRight);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_TOP_LEFT:
				case XML_BOTTOM_RIGHT:
					return new Coordinate.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
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

	public boolean isIn(final int x, final int y) {
		return this.topLeft.getX() <= x && x <= this.getBottomRight().getX() //
				&& this.topLeft.getY() <= y && y <= this.bottomRight.getY();
	}

	public boolean isIn(final Coordinate c) {
		return this.isIn(c.getX(), c.getY());
	}

	public Rectangle add(final Coordinate origin) {
		return new Rectangle(this.invertFunction, this.getTopLeft().add(origin), this.getBottomRight().add(origin));
	}

	public boolean isInvertFunction() {
		return this.invertFunction;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Rectangle)) {
			return false;
		}

		Rectangle cmp = (Rectangle) obj;

		if (this.invertFunction != cmp.invertFunction) {
			return false;
		}

		if (!this.topLeft.equals(cmp.topLeft)) {
			return false;
		}

		return this.bottomRight.equals(cmp.bottomRight);
	}

	@Override
	public int hashCode() {
		return (18521 + this.topLeft.hashCode() * 4567) * 37951 + this.bottomRight.hashCode() * 4567;
	}

}
