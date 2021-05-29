/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.imagepatternrecognition.image;

import de.sgollmer.solvismax.helper.ImageHelper;

public class ImageMeta {
	private final int averageBrightness;
	private final int treshold;
	private final boolean invert;

	ImageMeta(final MyImage image) {

		int minBrightness = 256 * 3;
		int maxBrightness = 0;
		long sum = 0;

		for (int x = 0; x < image.getWidth(); ++x) {
			for (int y = 0; y < image.getHeight(); ++y) {
				int rgb = image.getRGB(x, y);
				int brightness = ImageHelper.getBrightness(rgb);
				if (minBrightness > brightness) {
					minBrightness = brightness;
				}
				if (maxBrightness < brightness) {
					maxBrightness = brightness;
				}
				sum += brightness;
			}
		}

		this.treshold = (minBrightness + maxBrightness) / 2;
		this.averageBrightness = (int) (sum / (image.getWidth() * image.getHeight()));

		this.invert = this.treshold > this.averageBrightness;

	}

	boolean isActive(final int rgb) {
		int brightness = ImageHelper.getBrightness(rgb);
		return (brightness < this.treshold) != this.invert;
	}

	boolean isLight(final int rgb) {
		int brightness = ImageHelper.getBrightness(rgb);
		return brightness > this.treshold;
	}

	/**
	 * @return the invert
	 */
	boolean isInvert() {
		return this.invert;
	}

}
