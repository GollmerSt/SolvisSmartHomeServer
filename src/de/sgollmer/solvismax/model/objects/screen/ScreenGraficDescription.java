/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class ScreenGraficDescription implements IScreenCompare, IAssigner {

	private static final ILogger logger = LogManager.getInstance().getLogger(ScreenGraficDescription.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private static final String XML_FIELD = "Field";

	private final String id;
	private final boolean exact;
	private Rectangle rectangle = null;

	private ScreenGraficDescription(String id, boolean exact, Rectangle rectangle) {
		this.id = id;
		this.exact = exact;
		this.rectangle = rectangle;
	}

	@Override
	public boolean isElementOf(MyImage image, Solvis solvis) {
		return this.isElementOf(image, solvis, false);
	}

	public boolean isElementOf(MyImage image, Solvis solvis, boolean cmpNoRectangle) {
		if (!isLearned(solvis)) {
			return false;
		}
		ScreenGraficData data = solvis.getGrafics().get(this.id);
		if (data == null) { // not learned
			return false;
		}
		MyImage cmp = data.getImage();
		if (cmp == null) { // not learned
			return false;
		}
		if (!cmpNoRectangle) {
			image = new MyImage(image, this.rectangle, true);
		}
		if (this.exact) {
			return ((MyImage) image).equals(((MyImage) cmp));
		} else {
			image = new Pattern(image);
			return image.equals(cmp);
		}
	}

	public boolean isLearned(Solvis solvis) {
		return solvis.getGrafics().get(this.getId()) != null;
	}

	/**
	 * @return the exact
	 */
	public boolean isExact() {
		return this.exact;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}

	public static class Creator extends CreatorByXML<ScreenGraficDescription> {

		private String id;
		private boolean exact;
		private Rectangle rectangle;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "exact":
					this.exact = Boolean.parseBoolean(value);
					break;
			}

		}

		@Override
		public ScreenGraficDescription create() throws XmlError {
			return new ScreenGraficDescription(this.id, this.exact, this.rectangle);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_FIELD:
					return new Rectangle.Creator(name.getLocalPart(), this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_FIELD:
					this.rectangle = (Rectangle) created;
					break;
			}
		}

	}

	@Override
	public void assign(SolvisDescription description) {
	}

	public void learn(Solvis solvis) throws IOException {
		MyImage image = solvis.getCurrentScreen().getImage();
		if (this.exact) {
			image = new MyImage(image, this.rectangle, true);
		} else {
			image = new Pattern(image, this.rectangle);
		}
		ScreenGraficData grafic = solvis.getGrafics().get(this.getId());

		if (grafic != null) {
			if (!grafic.getImage().equals(image)) {
				String warning = "Warning: The screen grafic <" + this.getId() + "> was tried to learn again,"
						+ " but didn't match with the previous.";
				logger.log(LEARN, warning);
			}
		}
		solvis.getGrafics().put(this.id, image);
		logger.log(LEARN, "Screen grafic <" + this.getId() + "> learned.");
	}

	public void setRectangle(Rectangle rectangle) {
		if (rectangle != null) {
			this.rectangle = rectangle;
		}
	}

}
