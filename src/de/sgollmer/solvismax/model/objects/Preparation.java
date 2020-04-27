/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Preparation implements Assigner {

	private static final String XML_TOUCH_POINT = "TouchPoint";
	private static final String XML_SCREEN_GRAFIC = "ScreenGrafic";

	private final String id;
	private final TouchPoint touchPoint;
	private final ScreenGraficDescription screenGrafic;

	public Preparation(String id, TouchPoint touchPoint, ScreenGraficDescription screenGrafic) {
		this.id = id;
		this.touchPoint = touchPoint;
		this.screenGrafic = screenGrafic;
	}

	public String getId() {
		return this.id;
	}

	public boolean execute(Solvis solvis) throws IOException, TerminationException {
		solvis.getHistory().set(this);
		if (this.screenGrafic.isElementOf(solvis.getCurrentScreen().getImage(), solvis)) {
			return true;
		} else {
			solvis.send(this.touchPoint);
			return this.screenGrafic.isElementOf(solvis.getCurrentScreen().getImage(), solvis);
		}
	}

	/**
	 * This methods touch two times the button. After learning, the status is like
	 * after the execution of the preparation
	 * 
	 * @param solvis
	 * @return true, if learning successfull
	 * @throws IOException
	 * @throws TerminationException
	 */
	public boolean learn(Solvis solvis) throws IOException, TerminationException {
		if (!this.isLearned(solvis)) {
			solvis.send(this.touchPoint);
			solvis.send(this.touchPoint);
			this.screenGrafic.learn(solvis);
			solvis.getHistory().set(this);
			return true;
		} else {
			boolean result = false;
			for (int cnt = 0; !result && cnt < Constants.SET_REPEATS; ++cnt) {
				result = this.execute(solvis);
			}
			return result;
		}
	}

	public static class Creator extends CreatorByXML<Preparation> {

		private String id;
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
			}
		}

		@Override
		public Preparation create() throws XmlError, IOException {
			return new Preparation(this.id, this.touchPoint, this.screenGrafic);
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

	@Override
	public void assign(SolvisDescription description) {
		if (this.touchPoint != null) {
			this.touchPoint.assign(description);
		}

	}

}