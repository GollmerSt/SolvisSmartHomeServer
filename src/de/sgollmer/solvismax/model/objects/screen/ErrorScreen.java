package de.sgollmer.solvismax.model.objects.screen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;

public class ErrorScreen {

	public boolean is(MyImage image) {

		image.createHistograms(false);

		int tresholdX = image.getHeight() * 7 / 10;
		int tresholdY = image.getWidth() * 85 / 100;

		List<Integer> maxX = new ArrayList<>();

		boolean found = false;
		for (int x = 0; x < image.getWidth(); ++x) {
			if (image.getHistogramX().get(x) > tresholdX) {
				if (!found) {
					maxX.add(x);
					found = true;
				}
			} else {
				found = false;
			}
		}

		List<Integer> maxY = new ArrayList<>();

		found = false;
		for (int y = 0; y < image.getHeight(); ++y) {
			if (image.getHistogramY().get(y) > tresholdY) {
				if (!found) {
					maxY.add(y);
					found = true;
				}
			} else {
				found = false;
			}
		}

		int result = 0;

		for (int x : maxX) {
			if (20 * image.getWidth() / 385 < x && x < 32 * image.getWidth() / 385)
				result |= 0x01;
			if (349 * image.getWidth() / 385 < x && x < 361 * image.getWidth() / 385)
				result |= 0x02;
		}

		for (int y : maxY) {
			if (19 * image.getHeight() / 206 < y && y < 31 * image.getHeight() / 206)
				result |= 0x04;
			if (46 * image.getHeight() / 206 < y && y < 58 * image.getHeight() / 206)
				result |= 0x08;
			if (167 * image.getHeight() / 206 < y && y < 179 * image.getWidth() / 206)
				result |= 0x10 ;
		}

		return result == 0x1f;
	}

	public static void main(String[] args) {

		ErrorScreen errorScreen = new ErrorScreen();

		File parent = new File("testFiles\\images");

		String[] files = { "Stoerung 1.png", "Stoerung 2.png", "Stoerung 3.png", "Stoerung 4.png", "Home.png", "bildschirmschoner.png", "Night Temperatur selected.png" };

		for (String name : files) {

			File file = new File(parent, name);

			BufferedImage bufferedImage = null;
			try {
				bufferedImage = ImageIO.read(file);
			} catch (IOException e) {
				e.printStackTrace();
			}

			MyImage myImage = new MyImage(bufferedImage);

			if (errorScreen.is(myImage)) {
				System.out.println("Error screen detected");
			} else {
				System.out.println("Detection failed");
			}
		}
	}

}
