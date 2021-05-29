/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.imagepatternrecognition.pattern;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;

public class Pattern extends MyImage {

	private Integer hashCode = null;

	private Pattern(final BufferedImage image) {
		super(image);
		processing();
	}

	public Pattern(final MyImage image) {
		super(image);
		processing();
	}

	private Pattern(final MyImage image, final Coordinate topLeft, final Coordinate bottomRight) {
		super(image, topLeft, bottomRight, false);
		processing();
	}

	public Pattern(final MyImage image, final Rectangle rectangle) {
		super(image, rectangle, false);
		processing();
	}

	private void processing() {
		this.createHistograms(false);
		this.shrink();
	}

	@Override
	public int hashCode() {
		if (this.hashCode == null) {
			this.hashCode = 569;
			for (int x = 0; x < this.getWidth(); ++x) {
				for (int y = 0; y < this.getHeight(); ++y) {
					boolean light = this.isLight(x, y);
					this.hashCode = this.hashCode * 251 + (light ? 379 : 0);
				}
			}
		}
		return this.hashCode;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Pattern)) {
			return false;
		}
		Pattern p = (Pattern) obj;

		if (this.getWidth() != p.getWidth() || this.getHeight() != p.getHeight()) {
			return false;
		}
		if (this.hashCode() != p.hashCode()) {
			return false;
		}
		for (int x = 0; x < this.getWidth(); ++x) {
			for (int y = 0; y < this.getHeight(); ++y) {
				boolean light1 = this.isLight(x, y);
				boolean light2 = p.isLight(x, y);
				if (light1 != light2) {
					return false;
				}
			}
		}
		return true;
	}

	public static void main(final String[] args) {

		File parent = new File("testFiles\\images");

		File file = new File(parent, "Home.png");

		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		MyImage myImage = new MyImage(bufferedImage);

		Pattern curentSolvisMode = new Pattern(myImage, new Coordinate(106, 48), new Coordinate(137, 72));

		file = new File(parent, "standby.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Pattern standby = new Pattern(bufferedImage);

		file = new File(parent, "day.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Pattern day = new Pattern(bufferedImage);

		file = new File(parent, "night.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Pattern night = new Pattern(bufferedImage);

		Pattern[] modes = new Pattern[] { day, night, standby };

		for (int m = 0; m < modes.length; ++m) {
			Pattern c = modes[m];
			if (curentSolvisMode.equals(c)) {
				System.out.println("Position of pattern: " + m);
			}
		}
	}
}
