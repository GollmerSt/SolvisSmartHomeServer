package de.sgollmer.solvismax.imagepatternrecognition.image;

import de.sgollmer.solvismax.helper.ImageHelper;

public class ImageMeta {
	private final int averageBrightness;
	private final int treshold;
	private final boolean invert;

	public ImageMeta(MyImage image) {

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

	/**
	 * @return the averageBrightness
	 */
	public int getAverageBrightness() {
		return averageBrightness;
	}

	/**
	 * @return the treshold
	 */
	public int getTreshold() {
		return treshold;
	}

	public boolean isActive(int rgb) {
			int brightness = ImageHelper.getBrightness(rgb);
			return (brightness < this.treshold) != this.invert;
	}

	public boolean isLight(int rgb) {
			int brightness = ImageHelper.getBrightness(rgb);
			return brightness > this.treshold;
	}

	/**
	 * @return the invert
	 */
	public boolean isInvert() {
		return invert;
	}

}
