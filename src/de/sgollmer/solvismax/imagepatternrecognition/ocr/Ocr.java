package de.sgollmer.solvismax.imagepatternrecognition.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.imageio.ImageIO;

import de.sgollmer.solvismax.imagepatternrecognition.image.ImageMeta;
import de.sgollmer.solvismax.imagepatternrecognition.image.Maxima;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.objects.Coordinate;

public class Ocr extends MyImage {

	private Maxima[] maximaX = new Maxima[] { new Maxima(0, 0), new Maxima(0, 0) };
	private Maxima[] maximaY = new Maxima[] { new Maxima(0, 0), new Maxima(0, 0) };

	public Ocr(BufferedImage image) {
		super(image);
		this.processing(false);
	}

	public Ocr(MyImage image) {
		super(image);
		this.processing(false);
	}

	public Ocr(MyImage image, Coordinate topLeft, Coordinate bottomRight, boolean createImageMeta ) {
		super(image, topLeft, bottomRight, createImageMeta );
		
		this.processing(true);
	}

	private final void processing(boolean coordinatesChanged) {

		this.createHistograms(true);
		this.shrink();
		this.createMaxima();
	}

	private void createMaxima() {
		for (int x = 0; x < this.getHistogramX().size(); ++x) {
			int value = this.getHistogramX().get(x);
			for (int m = 0; m < 2; ++m) {
				int cmpCoord = this.maximaX[m].getCoord();
				int cmpValue = this.maximaX[m].getValue();
				if (cmpValue == value && cmpCoord == x - 1) {
					break;
				}
				if (cmpValue <= value) {
					this.maximaX[m] = new Maxima(x, value);
					break;
				}
			}
		}

		for (int y = 0; y < this.getHistogramY().size(); ++y) {
			int value = this.getHistogramY().get(y);
			for (int m = 0; m < 2; ++m) {
				int cmpCoord = this.maximaY[m].getCoord();
				int cmpValue = this.maximaY[m].getValue();
				if (cmpValue == value && cmpCoord == y - 1) {
					break;
				}
				if (cmpValue <= value) {
					this.maximaY[m] = new Maxima(y, value);
					break;
				}
			}
		}

	}

	private class BlackWhite {

		private Coordinate black;
		private Coordinate white;

		public BlackWhite(Coordinate black, Coordinate white) {
			this.black = black;
			this.white = white;
		}

		public BlackWhite nextCicle() {
			Coordinate diff = this.white.diff(this.black);
			diff = new Coordinate(-diff.getY(), diff.getX());
			BlackWhite newBlackWhite = new BlackWhite(this.black.add(diff), this.white.add(diff));
			boolean checkInactive = Ocr.this.isActive(newBlackWhite.white);
			boolean checkActive = Ocr.this.isActive(newBlackWhite.black);

			if (checkActive && !checkInactive) {
				return newBlackWhite;
			} else if (!checkActive && !checkInactive) {
				return new BlackWhite(this.black, newBlackWhite.black);
			} else {
				return new BlackWhite(newBlackWhite.white, this.white);
			}
		}

		public BlackWhite nextMonoton(boolean left) {
			Coordinate diff = this.white.diff(this.black);
			diff = new Coordinate(left ? diff.getY() : -diff.getY(), diff.getX() > 0 ? diff.getX() : -diff.getX());
			BlackWhite newBlackWhite = new BlackWhite(this.black.add(diff), this.white.add(diff));
			boolean checkInactive = Ocr.this.isActive(newBlackWhite.white);
			boolean checkActive = Ocr.this.isActive(newBlackWhite.black);

			if (checkActive) {
				if (checkInactive) {
					return new BlackWhite(newBlackWhite.white, this.white);
				} else {
					return newBlackWhite;
				}
			} else {
				if (checkInactive) {
					if (newBlackWhite.white.getY() >= this.black.getY()) {
						return new BlackWhite(newBlackWhite.white, this.white);
					} else {
						return new BlackWhite(this.black, newBlackWhite.black);
					}
				} else {
					return new BlackWhite(this.black, newBlackWhite.black);
				}
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof BlackWhite)) {
				return false;
			} else {
				BlackWhite bw = (BlackWhite) obj;
				return this.white.equals(bw.white) && this.black.equals(bw.black);
			}
		}

		@Override
		public int hashCode() {
			return 89 + 617 * this.black.hashCode() + this.white.hashCode();
		}

		@Override
		public String toString() {
			return "Black: " + this.black + ", white: " + this.white;
		}
	}

	private Coordinate getClosedStructure(Coordinate coord) {

		if (this.isActive(coord)) {
			return null;
		}

		boolean found = false;

		int startX;
		for (startX = coord.getX(); startX < this.getWidth(); ++startX) {
			if (this.isActive(startX, coord.getY())) {
				found = true;
				break;
			}
		}

		if (!found) {
			return null;
		}

		Coordinate black = new Coordinate(startX, coord.getY());
		Coordinate white = new Coordinate(startX - 1, coord.getY());

		BlackWhite start = new BlackWhite(black, white);

		Coordinate returnValue = white;
		int cnt = 0;

		boolean cont = true;

		BlackWhite current = start;

		while (cont) {
			BlackWhite next = current.nextCicle();
			if (next == null || !this.isIn(next.white)) {
				return null;
			}
			returnValue = returnValue.add(next.white);
			++cnt;
			cont = !start.equals(next);
			current = next;
		}

		return returnValue.div(cnt);
	}

	private boolean detectSlash(boolean backSlash, int startY, int endY) {

		int x = (backSlash ? 0 : this.getWidth() - 1);
		int add = backSlash ? 1 : -1;

		for (; !this.isActive(x, startY) && x >= 0 && x < this.getWidth(); x += add)
			;

		for (; this.isActive(x, startY) && x >= 0 && x < this.getWidth(); x += add)
			;

		Coordinate white = new Coordinate(x, startY);
		Coordinate black = new Coordinate(x - add, startY);

		BlackWhite current = new BlackWhite(black, white);

		while (true) {
			BlackWhite next = current.nextMonoton(!backSlash);
			if (next == null) {
				return false;
			} else if (next.white.getY() >= endY || next.black.getY() >= endY) {
				return true;
			}
			Coordinate diff = next.white.diff(current.white);
			if (diff.getY() < 0 || diff.getX() * add < 0) {
				return false;
			}
			current = next;
		}
	}

	public char toChar() {
		// - Erkennung von 4, geschlossene Struktur obere Hälfte, waagerechtes
		// Maximum 3/4 * Breite, nahe Mitte

		int y = this.maximaY[0].getCoord();
		int value = this.maximaY[0].getValue();

		if (value > this.getWidth() * 10 / 11 && this.getHeight() / 3 < y && y < this.getHeight() * 3 / 4) {

			int x = this.maximaX[0].getCoord() / 2;

			while (this.isActive(x, --y))
				;

			Coordinate coord = new Coordinate(x, y);

			if (this.isIn(coord) && this.getClosedStructure(coord) != null) {
				return '4';
			}
		}

		Coordinate upperClosedStructure = this
				.getClosedStructure(new Coordinate(this.getWidth() / 2, this.getHeight() / 4));
		Coordinate lowerClosedStructure = this
				.getClosedStructure(new Coordinate(this.getWidth() / 2, this.getHeight() * 3 / 4));

		if (upperClosedStructure != null && lowerClosedStructure != null) {
			if (upperClosedStructure.approximately(lowerClosedStructure, 2)) {
				// - Erkennung von 0, geschlossene Struktur nahe Mitte
				if (this.getHeight() > this.getWidth() * 5 / 4) {
					return '0';
				} else {
					return '°';
				}
			} else {
				// - Erkennung von 8, zwei geschlossene Strukturen
				return '8';
			}
		} else if (upperClosedStructure != null) {
			// - Erkennung von 9, geschlossene Struktur obere Hälfte
			return '9';
		} else if (lowerClosedStructure != null) {
			// - Erkennung von 6, geschlossene Struktur untere Hälfte
			return '6';
		}

		if (this.maximaY[0].getValue() == this.getWidth() && this.maximaX[0].getValue() < (this.getWidth() + 1) / 2) {
			// - Erkennung von -, waagerechtes Maximum = Breite, senkrechtes
			// Maximum < 1/2 Breite
			return '-';
		}

		if (this.getWidth() * 2 < this.getHeight() && this.getHistogramY().get(this.getHeight() / 2) == 0) {
			return ':';
		}

		if (this.maximaY[0].getValue() == this.getWidth() && this.maximaX[0].getValue() == this.getHeight()
				&& this.maximaX[0].getCoord() > this.getWidth() / 3
				&& this.maximaX[0].getCoord() < this.getWidth() * 2 / 3) {
			// - Erkennung von +, senkrechtes Maximum = Höhe, waagerechtes
			// maximum = Breite
			return '+';
		}

		if (this.getWidth() * 2 > this.getHeight() && this.getHeight() * 2 > this.getWidth()
				&& this.getHistogramY().get(this.getHeight() / 2) == this.getWidth()
				&& this.getHistogramX().get(this.getWidth() / 2) == this.getHeight()) {
			return '.';
		}

		if (this.getHistogramX().get(0) > this.getHeight() * 4 / 5
				&& this.getHistogramY().get(0) > this.getWidth() * 4 / 5) {
			return '[';
		}

		if (this.getHistogramX().get(this.getWidth() - 1) > this.getHeight() * 4 / 5
				&& this.getHistogramY().get(0) > this.getWidth() * 4 / 5) {
			return ']';
		}

		if (this.getHistogramX().get(0) < this.getHeight() / 3 && this.maximaY[0].getValue() < this.getWidth() / 2) {
			if (this.detectSlash(false, 0, this.getHeight() - 1)) {
				return '/';
			}
		}

		if (this.maximaX[0].getValue() == this.getHeight() && this.maximaX[0].getCoord() < this.getWidth() / 3) {
			return 'h';
		}

		if (this.maximaX[0].getValue() == this.getHeight()) {
			// if ( this.maximaX[0].getCoord() >= this.getWidth() - 2 &&
			// this.maximaX[0].getValue() > this.getHeight() * 3 / 4 ) {
			// - Erkennung von 1, senkrechtes Maximum = Höhe, rechts
			return '1';
		}

		if (this.maximaY[0].getCoord() >= this.getHeight() - 2
				&& this.maximaY[0].getValue() > this.getWidth() * 5 / 6) {
			// - Erkennung von 2, waagerechtes Maximum unten
			return '2';
		}

		if (this.maximaY[0].getValue() == this.getWidth() && this.maximaX[0].getValue() < this.getWidth() / 2) {
			// - Erkennung von -, waagerechtes Maximum = Breite, senkrechtes
			// Maximum < 1/2 Breite
			return '-';
		}

		if (this.maximaY[0].getValue() == this.getWidth()) {
			// - Erkennung von 7, waagerechtes Maximum = 1, oben
			return '7';
		}

		if (this.getHistogramY().get(0) > this.getWidth() * 2 / 3) {
			// - Erkennung von 5, Waagerechtes Maximum oben, 2. Maximum
			// ausgeprägt Mitte
			return '5';
		}

		if (this.detectSlash(false, 0, this.getWidth() - 1)) {
			int x = 0;
			while (!this.isActive(x++, 1))
				;
			if (x < this.getWidth() / 2) {
				return '%';
			}
		}

		if (this.maximaX[0].getCoord() > this.getWidth() / 2 && this.maximaX[1].getCoord() > this.getWidth() / 2) {
			// - Erkennung von 3, Senkrechtes Maximum rechte Hälfte, 2. Maximum
			// ebenfalls rechte Hälfte
			return '3';
		}

		if (this.maximaX[0].getValue() == this.getHeight() && this.maximaX[0].getCoord() < this.getWidth() / 3) {
			return 'h';
		}

		if (this.maximaX[0].getCoord() < 3) {
			return 'C';
		}

		return 0x00;
	}

	public static void main(String[] args) {

		File parent = new File("testFiles\\images");

		Collection<String> names = Arrays.asList("0.png", "1.png", "1 small.png", "2.png", "3.png", "4.png",
				"4 black.png", "4 small.png", "5.png", "6.png", "7.png", "8.png", "9.png", "9 grey small.png",
				"minus.png", "minus2.png", "plus.png", "doppelpunkt.png", "punkt.png", "punkt small.png", "h small.png",
				"grad.png", "C.png", "square bracket left.png", "square bracket right.png", "slash.png", "percent.png",
				"percent grey.png");

		BufferedImage image = null;

		for (Iterator<String> it = names.iterator(); it.hasNext();) {
			File file = new File(parent, it.next());
			try {
				image = ImageIO.read(file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("File: " + file.getName());
				e.printStackTrace();
			}

			Ocr ocr = new Ocr(image);

			char c = ocr.toChar();

			String out = "Character of file <" + file.getName() + "> is:  ";

			while (out.length() < 55) {
				out = " " + out;
			}

			System.out.println(out + c);
		}

	}

}
