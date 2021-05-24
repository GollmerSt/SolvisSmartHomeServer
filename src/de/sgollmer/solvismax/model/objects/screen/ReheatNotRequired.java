/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.imageio.ImageIO;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;

public class ReheatNotRequired {

	@SuppressWarnings("unused")
	private static final ILogger logger = LogManager.getInstance().getLogger(ReheatNotRequired.class);

	private ReheatNotRequired(int timeHeight, Rectangle timeDateRectangle, Coordinate maxGraficSize) {
	}

	public static boolean is(SolvisScreen screen) {
		MyImage original = SolvisScreen.getImage(screen);
		return is(original);
	}

	private static boolean is(MyImage image) {

		int fs = Constants.SCREEN_IGNORED_FRAME_SIZE;

		Rectangle scanArea = new Rectangle(new Coordinate(fs, fs),
				new Coordinate(image.getWidth() - 2 * fs - 1, image.getHeight() - 2 * fs - 1));

		MyImage scannedImage = new MyImage(image, scanArea, false);
		scannedImage.createHistograms(false);

		int areasCnt = 0;
		boolean found = false;
		int minY = -1;
		int maxY = -1;

		for (int y = 0; y < scannedImage.getHeight(); ++y) {
			boolean black = scannedImage.getHistogramY().get(y) > 0;
			if (black & !found) {
				found = true;
				++areasCnt;
			} else if (!black && found) {
				found = false;
			}
			if (found && areasCnt == 3) {
				if (minY < 0) {
					minY = y;
				}
				maxY = y;
			}
		}

		if (areasCnt != 3) {
			return false;
		}

		scanArea = new Rectangle(new Coordinate(fs, minY), new Coordinate(scannedImage.getWidth() - 1 - 2 * fs, maxY));

		scannedImage = new MyImage(scannedImage, scanArea, false);
		scannedImage.createHistograms(false);

		int minX = -1;
		int maxX = -1;
		areasCnt = 0;
		found = false;

		for (int x = 0; x < scannedImage.getWidth(); ++x) {
			boolean black = scannedImage.getHistogramX().get(x) > 0;
			if (black & !found) {
				found = true;
				++areasCnt;
			} else if (!black && found) {
				found = false;
			}
			if (found && areasCnt == 1) {
				if (minX < 0) {
					minX = x;
				}
				maxX = x;
			}
		}

		if (areasCnt < 3) {
			return false;
		}

		return maxX - minX == maxY - minY;
	}

	public static void main(String[] args) {

		File parent = new File("testFiles\\images");

		final class Test {
			private final boolean soll;
			private final String name;

			private Test(boolean soll, String name) {
				this.soll = soll;
				this.name = name;
			}
		}

		Collection<Test> names = Arrays.asList( //
				new Test(false, "Bildschirmschoner V1 Artefakte.bmp"), //
				new Test(true, "Nachheizen nicht erforderlich.png"),
				new Test(true, "Nachheizen nicht erforderlich - mit Artefakten.png")//
		);

		BufferedImage image = null;


		boolean failed = false;

		int i = 0;
		for (Iterator<Test> it = names.iterator(); it.hasNext();) {
			Test test = it.next();
			File file = new File(parent, test.name);
			try {
				image = ImageIO.read(file);
			} catch (IOException e) {
				System.err.println("File: " + file.getName());
				e.printStackTrace();
			}

			MyImage myImage = new MyImage(image);

			boolean notRequired = is(myImage);

			boolean pass = notRequired == test.soll;
			if (!pass) {
				failed = true;
			}

			System.out.println("" + ++i + ". " + file.getName() + " SolvisStatus: " + Boolean.toString(notRequired)
					+ ", Soll: " + Boolean.toString(test.soll) + ", Check: " + pass);
		}

		if (failed) {
			System.err.println("Test failed");
		} else {
			System.out.println("Test pass");
		}

	}

}
