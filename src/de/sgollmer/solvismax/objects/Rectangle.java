package de.sgollmer.solvismax.objects;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class Rectangle {
	private final Coordinate topLeft;
	private final Coordinate bottomRight;

	public Rectangle(Coordinate topLeft, Coordinate bottomRight) {
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
	}

	/**
	 * @return the topLeft
	 */
	public Coordinate getTopLeft() {
		return topLeft;
	}

	/**
	 * @return the bottomRight
	 */
	public Coordinate getBottomRight() {
		return bottomRight;
	}

	public static class Creator extends CreatorByXML<Rectangle> {
		private Coordinate topLeft;
		private Coordinate bottomRight;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public Rectangle create() throws XmlError {
			return new Rectangle(topLeft, bottomRight);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case "TopLeft":
				case "BottomRight":
					return new Coordinate.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case "TopLeft":
					this.topLeft = (Coordinate) created;
					break;
				case "BottomRight":
					this.bottomRight = (Coordinate) created;
			}

		}

	}

	public boolean isIn(int x, int y) {
		return this.topLeft.getX() <= x && x <= this.getBottomRight().getX() //
				&& this.topLeft.getY() <= y && y <= this.bottomRight.getY();
	}
	
	public boolean isIn( Coordinate c) {
		return this.isIn(c.getX(), c.getY()) ;
	}
	
	public Rectangle add( Coordinate origin ) {
		return new Rectangle( this.getTopLeft().add(origin), this.getBottomRight().add(origin)) ;
	}

}
