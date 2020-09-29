/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ScreenOcr implements IScreenPartCompare, IAssigner {

	private static final ILogger logger = LogManager.getInstance().getLogger(ScreenOcr.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private static final String XML_RECTANGLE = "Rectangle";
	private static final String XML_SCREEN_GRAFIC_REF = "ScreenGraficRef";

	private final Rectangle rectangle;
	private final String value;
	private final ScreenGraficRef graficRef;
	private final boolean right;

	private ScreenGraficDescription graficDescription = null;

	private ScreenOcr(Rectangle rectangle, String value, ScreenGraficRef graficRef, boolean right) {
		this.rectangle = rectangle;
		this.value = value;
		this.graficRef = graficRef;
		this.right = right;
	}

	@Override
	public boolean isElementOf(MyImage image, Solvis solvis) {

		SplitResult splitResult = this.split(image, solvis);

		if (!splitResult.scanHit) {
			return false;
		}

		image = splitResult.image;

		if (image == null) {
			return true;
		}

		return this.graficDescription.isElementOf(image, solvis, true);
	}

	static class Creator extends CreatorByXML<ScreenOcr> {

		private Rectangle rectangle;
		private String value;
		private ScreenGraficRef graficRef = null;
		private boolean right = false;

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "value":
					this.value = value;
					break;
				case "right":
					this.right = Boolean.parseBoolean(value);
					break;
			}

		}

		@Override
		public ScreenOcr create() throws XmlException, IOException {
			return new ScreenOcr(this.rectangle, this.value, this.graficRef, this.right);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_RECTANGLE:
					return new Rectangle.Creator(id, this.getBaseCreator());
				case XML_SCREEN_GRAFIC_REF:
					return new ScreenGraficRef.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_RECTANGLE:
					this.rectangle = (Rectangle) created;
					break;
				case XML_SCREEN_GRAFIC_REF:
					this.graficRef = (ScreenGraficRef) created;
					break;
			}
		}

	}

	private static class SplitResult {
		private final MyImage image;
		private final boolean scanHit;

		public SplitResult(MyImage image, boolean scanHit) {
			this.image = image;
			this.scanHit = scanHit;
		}
	}

	private SplitResult split(MyImage image, Solvis solvis) {

		if (this.graficDescription == null) {
			String cmp = new OcrRectangle(image, this.rectangle).getString();
			return new SplitResult(null, this.value.equals(cmp));
		}

		if (this.graficDescription.isExact()) {
			image = new MyImage(image, this.rectangle, true);
		} else {
			image = new Pattern(image, this.rectangle);
		}

		List<MyImage> images = image.split();

		int charCnt = this.value.length();

		if (images.size() <= charCnt) {
			return new SplitResult(null, false);
		}

		MyImage leftImage;
		MyImage rightImage;
		int ocrStart;

		if (this.right) {
			leftImage = images.get(0);
			ocrStart = images.size() - charCnt;
			rightImage = images.get(ocrStart - 1);
		} else {
			ocrStart = 0;
			leftImage = images.get(charCnt);
			rightImage = images.get(images.size() - 1);
		}

		Rectangle imageRectangle = new Rectangle( //
				new Coordinate( //
						leftImage.getOrigin().getX() - image.getOrigin().getX(),
						leftImage.getOrigin().getY() - image.getOrigin().getY()), //
				new Coordinate( //
						rightImage.getOrigin().getX() + rightImage.getWidth() - 1 - image.getOrigin().getX(), //
						rightImage.getOrigin().getY() + rightImage.getHeight() - 1 - image.getOrigin().getY()) //
		);

		if (this.graficDescription.isExact()) {

			image = new MyImage(image, imageRectangle, false);
		} else {
			image = new Pattern(image, imageRectangle);
		}

		Collection<MyImage> ocrImages = new ArrayList<>(charCnt);
		for (int i = 0; i < charCnt; ++i) {
			ocrImages.add(images.get(i + ocrStart));
		}
		String cmp = new OcrRectangle(image, ocrImages).getString();
		return new SplitResult(image, this.value.equals(cmp));

	}

	@Override
	public void learn(Solvis solvis) throws IOException, TerminationException {

		if (this.graficDescription == null) {
			return;
		}

		String id = this.graficDescription.getId();

		MyImage image = SolvisScreen.getImage(solvis.getCurrentScreen());

		SplitResult result = this.split(image, solvis);

		image = result.image;

		ScreenGraficData grafic = solvis.getGrafics().get(id);

		if (grafic != null) {
			if (!grafic.getImage().equals(image)) {
				String warning = "Warning: The screen grafic <" + id + "> was tried to learn again,"
						+ " but didn't match with the previous.";
				logger.log(LEARN, warning);
			}
		}
		solvis.getGrafics().put(id, image);
		logger.log(LEARN, "Screen grafic <" + id + "> learned.");
		solvis.writeLearningImage(image, "graphic__" + id);
	}

	@Override
	public void assign(SolvisDescription description) throws XmlException, AssignmentException {
		if (this.graficRef != null) {
			this.graficDescription = description.getScreenGrafics().get(this.graficRef.getRefId());
		}
	}

	@Override
	public boolean isLearned(Solvis solvis) {
		if (this.graficDescription == null) {
			return true;
		} else {
			return this.graficDescription.isLearned(solvis);
		}
	}

}
