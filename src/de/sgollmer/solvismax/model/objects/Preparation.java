/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Preparation implements Assigner {

	private static final String XML_TOUCH_POINT = "TouchPoint";
	private static final String XML_SCREEN_GRAFIC = "ScreenGrafic";

	private final String id;
	private final String clearScreenId;
	private final TouchPoint touchPoint;
	private final ScreenGraficDescription screenGrafic;

	private OfConfigs<Screen> clearScreen = null;

	public Preparation(String id, String graficsClearPreparationId, TouchPoint touchPoint,
			ScreenGraficDescription screenGrafic) {
		this.id = id;
		this.touchPoint = touchPoint;
		this.screenGrafic = screenGrafic;
		this.clearScreenId = graficsClearPreparationId;
	}

	@Override
	public void assign(SolvisDescription description) {
		if (this.clearScreenId != null) {
			this.clearScreen = description.getScreens().get(clearScreenId);
		}
	}

	public Screen getClearScreen(int configurationMask) {
		if (this.clearScreen == null) {
			return null;
		} else {
			return clearScreen.get(configurationMask);
		}
	}

	public String getId() {
		return id;
	}

	public boolean execute(Solvis solvis) throws IOException, TerminationException {
		solvis.send(touchPoint);
		return this.screenGrafic.isElementOf(solvis.getCurrentImage(), solvis);
	}

	public boolean learn(Solvis solvis, Screen screen) throws IOException, TerminationException {
		if (this.clearScreen != null) {
			solvis.gotoScreen(this.getClearScreen(solvis.getConfigurationMask()));
		}
		solvis.gotoScreen(screen);
		MyImage unselected = new MyImage(solvis.getCurrentImage(), screenGrafic.getRectangle(), false);
		solvis.send(this.touchPoint);
		MyImage selected = new MyImage(solvis.getCurrentImage(), screenGrafic.getRectangle(), false);
		if (unselected.equals(selected)) {
			return false;
		} else {
			screenGrafic.learn(solvis);
			return true;
		}

	}

	public static class Creator extends CreatorByXML<Preparation> {

		private String id;
		private String clearScreenId;
		private TouchPoint touchPoint;
		private ScreenGraficDescription screenGrafic;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
			case "id":
				this.id = value;
				break;
			case "clearScreenId":
				this.clearScreenId = value;
				break;
			}
		}

		@Override
		public Preparation create() throws XmlError, IOException {
			return new Preparation(id, clearScreenId, touchPoint, screenGrafic);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
			case XML_TOUCH_POINT:
				return new TouchPoint.Creator(id, getBaseCreator());
			case XML_SCREEN_GRAFIC:
				return new ScreenGraficDescription.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
			case XML_TOUCH_POINT:
				this.touchPoint = (TouchPoint) created;
				break;
			case XML_SCREEN_GRAFIC:
				this.screenGrafic = (ScreenGraficDescription) created;
			}

		}
	}

	public boolean isLearned(Solvis solvis) {
		return this.screenGrafic.isLearned(solvis);
	}

}