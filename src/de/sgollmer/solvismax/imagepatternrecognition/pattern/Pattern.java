package de.sgollmer.solvismax.imagepatternrecognition.pattern;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.sgollmer.solvismax.imagepatternrecognition.image.Coordinate;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;

public class Pattern extends MyImage {

	private Integer hashCode = null;

	public Pattern(BufferedImage image) {
		super(image);
		processing();
	}

	public Pattern(MyImage image) {
		super(image);
		processing();
	}

	public Pattern(MyImage image, Coordinate topLeft, Coordinate bottomRight) {
		super(image, topLeft, bottomRight);
		processing();
	}

	private void processing() {
		this.convertToBlackWhite(false);
		this.shrink();
	}

	@Override
	public int hashCode() {
		if (this.hashCode == null) {
			this.hashCode = 569;
			for (int x = 0; x < this.getWidth(); ++x) {
				for (int y = 0; y < this.getHeight(); ++y) {
					int rgb = this.getRGB(x, y);
					this.hashCode = this.hashCode * 251 + rgb * 379;
				}
			}
		}
		return this.hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (!(obj instanceof Pattern)) {
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
				int rgb1 = this.getRGB(x, y);
				int rgb2 = this.getRGB(x, y);
				if (rgb1 != rgb2) {
					return false;
				}
			}
		}
		return true;
	}

	public static void main(String[] args) {
		
		File parent = new File( "src\\de\\sgollmer\\solvismax\\dokus\\images");

		File file = new File(parent, "Home.png");

		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		MyImage myImage = new MyImage(bufferedImage);

		Pattern curentSolvisMode = new Pattern(myImage, new Coordinate(106, 48), new Coordinate(137, 72));

		file = new File(parent, "standby.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Pattern standby = new Pattern(bufferedImage);

		file = new File(parent, "day.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Pattern day = new Pattern(bufferedImage);

		file = new File(parent, "night.png");

		try {
			bufferedImage = ImageIO.read(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
