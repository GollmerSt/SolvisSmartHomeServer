/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.tinylog.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Preparation implements IAssigner {

	private static final String XML_TOUCH_POINT = "TouchPoint";
	private static final String XML_SCREEN_GRAFIC = "ScreenGrafic";

	private final String id;
	private final TouchPoint touchPoint;
	private final ScreenGraficDescription screenGrafic;

	private Preparation(final String id, final TouchPoint touchPoint, final ScreenGraficDescription screenGrafic) {
		this.id = id;
		this.touchPoint = touchPoint;
		this.screenGrafic = screenGrafic;
	}

	String getId() {
		return this.id;
	}

	public boolean execute(final Solvis solvis) throws IOException, TerminationException {
		boolean success = false;
		for (int c = 0; c < Constants.PREPARATION_REPEATS && !success; ++c) {
			try {
				if (this.screenGrafic.isElementOf(solvis.getCurrentScreen().getImage(), solvis)) {
					success = true;
				} else {
					solvis.send(this.touchPoint);
					success = this.screenGrafic.isElementOf(solvis.getCurrentScreen().getImage(), solvis);
				}
			} catch (IOException e) {
			}
			if (!success && c == 0) {
				Logger.error("Preparation not successfull, will be tried again");
			}
		}
		if (success) {
			solvis.getHistory().set(this);
		}
		return success;
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
	public boolean learn(final Solvis solvis) throws IOException, TerminationException {
		if (!this.isLearned(solvis)) {
			solvis.send(this.touchPoint);
			solvis.send(this.touchPoint);
			SolvisScreen currentScreen = solvis.getCurrentScreen();
			solvis.writeLearningImage(currentScreen, this.id);
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

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
			}
		}

		@Override
		public Preparation create() throws XmlException, IOException {
			return new Preparation(this.id, this.touchPoint, this.screenGrafic);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
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
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_TOUCH_POINT:
					this.touchPoint = (TouchPoint) created;
					break;
				case XML_SCREEN_GRAFIC:
					this.screenGrafic = (ScreenGraficDescription) created;
			}

		}
	}

	public boolean isLearned(final Solvis solvis) {
		return this.screenGrafic.isLearned(solvis);
	}

	@Override
	public void assign(final SolvisDescription description) throws AssignmentException {
		if (this.touchPoint != null) {
			this.touchPoint.assign(description);
		}

	}

	public static boolean prepare(final Preparation preparation, final Solvis solvis)
			throws IOException, TerminationException {
		if (preparation == null) {
			return true;
		} else {
			return preparation.execute(solvis);
		}
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof Preparation)) {
			return false;
		}
		return this.id.equals(((Preparation) o).getId());
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

}