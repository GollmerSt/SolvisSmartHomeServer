/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.imagepatternrecognition.image;

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
				int brightness = (rgb & 0xff) + (rgb >> 8 & 0xff) + (rgb >> 16 & 0xff);
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
		int brightness = (rgb & 0xff) + (rgb >> 8 & 0xff) + (rgb >> 16 & 0xff);
		return (brightness < this.treshold) != this.invert;
	}

	boolean isLight(final int rgb) {
		int brightness = (rgb & 0xff) + (rgb >> 8 & 0xff) + (rgb >> 16 & 0xff);
		return brightness > this.treshold;
	}

	/**
	 * @return the invert
	 */
	boolean isInvert() {
		return this.invert;
	}

}
