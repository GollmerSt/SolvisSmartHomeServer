package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.ReferenceError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Screen implements GraficsLearnable, Comparable<Screen> {

	private final int LEARN_REPEAT_COUNT = 3;

	private static final String XML_SCREEN_GRAFICS = "ScreenGrafic";
	private static final String XML_SCREEN_GRAFICS_REF = "ScreenGraficRef";
	private static final String XML_SCREEN_OCR = "ScreenOcr";
	private static final String XML_IGNORE_RECTANGLE = "IgnoreRectangle";

	private final String id;
	private final String previousId;
	private final String backId;
	private final TouchPoint touchPoint;
	private final Collection<ScreenCompare> screenCompares = new ArrayList<>();
	private final Collection<String> screenGraficRefs;
	private final Collection<Rectangle> ignoreRectangles;

	public Screen previousScreen = null;
	public Screen backScreen = null;
	public Collection<Screen> nextScreens = new ArrayList<>(3);

	public Screen(String id, String previousId, String backId, TouchPoint touchPoint,
			Collection<String> screenGraficRefs, Collection<ScreenOcr> ocrs, Collection<Rectangle> ignoreRectangles) {
		this.id = id;
		this.previousId = previousId;
		this.backId = backId;
		this.touchPoint = touchPoint;
		this.screenGraficRefs = screenGraficRefs;
		this.screenCompares.addAll(ocrs);
		this.ignoreRectangles = ignoreRectangles;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the backId
	 */
	public String getBackId() {
		return backId;
	}

	@Override
	public boolean equals(Object obj) {
		return this.id.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	public void assign(SolvisDescription description) throws ReferenceError {
		for (String id : screenGraficRefs) {
			ScreenGraficDescription grafic = description.getScreenGrafics().get(id);
			if (grafic == null) {
				throw new ReferenceError("Screen grafic reference < " + id + " > not found");
			}
			this.screenCompares.add(grafic);
		}

		if (backId != null) {
			this.backScreen = description.getScreens().get(backId);
			if (this.backScreen == null) {
				throw new ReferenceError("Screen reference < " + this.backId + " > not found");
			}
		}
		if (previousId != null) {
			this.previousScreen = description.getScreens().get(previousId);
			if (this.previousScreen == null) {
				throw new ReferenceError("Screen reference < " + this.previousId + " > not found");
			}
			this.previousScreen.addNextScreen(this);
		}
		if (this.touchPoint != null) {
			this.touchPoint.assign(description);
		}
	}

	public void addNextScreen(Screen nextScreen) {
		this.nextScreens.add(nextScreen);
	}

	public boolean isScreen(MyImage image, Solvis solvis) {
		for (ScreenCompare grafic : this.screenCompares) {
			if (!grafic.isElementOf(image, solvis)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isLearned(Solvis solvis) {
		for (ScreenCompare cmp : this.screenCompares) {
			if (cmp instanceof ScreenGraficDescription) {
				if (!((ScreenGraficDescription) cmp).isLearned(solvis)) {
					return false;
				}
			}
		}
		return true;
	}

	public List<Screen> getPreviosScreens() { // Alle Bildschirme davor einschl.
												// this

		List<Screen> screens = new ArrayList<>();

		Screen previous = this.getPreviousScreen();

		while (previous != null) {
			screens.add(previous);
			previous = previous.getPreviousScreen();
		}
		return screens;
	}

	/**
	 * @return the previousScreen
	 */
	public Screen getPreviousScreen() {
		return previousScreen;
	}

	/**
	 * @return the touchPoint
	 */
	public TouchPoint getTouchPoint() {
		return touchPoint;
	}

	public static class Creator extends CreatorByXML<Screen> {

		private String id;
		private String previousId;
		private String backId;
		private TouchPoint touchPoint;
		private Collection<String> screenGraficRefs = new ArrayList<>();
		private Collection<ScreenOcr> screenOcr = new ArrayList<>();
		private Collection<Rectangle> ignoreRectangles = null ;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "previousId":
					this.previousId = value;
					break;
				case "backId":
					this.backId = value;
					break;
			}

		}

		@Override
		public Screen create() throws XmlError {
			return new Screen(id, previousId, backId, touchPoint, screenGraficRefs, screenOcr, ignoreRectangles);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (name.getLocalPart()) {
				case "TouchPoint":
					return new TouchPoint.Creator(id, this.getBaseCreator());
				case XML_SCREEN_GRAFICS:
					return new ScreenGraficDescription.Creator(id, this.getBaseCreator());
				case XML_SCREEN_GRAFICS_REF:
					return new CreatorScreenGraficRef(id, this.getBaseCreator());
				case XML_SCREEN_OCR:
					return new ScreenOcr.Creator(id, getBaseCreator());
				case XML_IGNORE_RECTANGLE:
					return new Rectangle.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case "TouchPoint":
					this.touchPoint = (TouchPoint) created;
					break;
				case XML_SCREEN_GRAFICS:
					ScreenGraficDescription grafic = (ScreenGraficDescription) created;
					this.screenGraficRefs.add(grafic.getId());
					((SolvisDescription.Creator) this.getBaseCreator()).getScreenGraficDescriptions().add(grafic);
					break;
				case XML_SCREEN_GRAFICS_REF:
					this.screenGraficRefs.add((String) created);
					break;
				case XML_SCREEN_OCR:
					this.screenOcr.add((ScreenOcr) created);
					break;
				case XML_IGNORE_RECTANGLE:
					if ( this.ignoreRectangles == null) {
						this.ignoreRectangles = new ArrayList<>() ;
					}
					this.ignoreRectangles.add((Rectangle) created);
			}

		}

	}

	private static class CreatorScreenGraficRef extends CreatorByXML<String> {

		private String refId;

		public CreatorScreenGraficRef(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			if (name.getLocalPart().equals("refId")) {
				this.refId = value;
			}

		}

		@Override
		public String create() throws XmlError {
			return refId;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
		}

	}

	@Override
	public void createAndAddLearnScreen(LearnScreen learnScreen, Collection<LearnScreen> learnScreens) {
		for (ScreenCompare cmp : this.screenCompares) {
			if (cmp instanceof ScreenGraficDescription) {
				ScreenGraficDescription description = (ScreenGraficDescription) cmp;
				LearnScreen learn = new LearnScreen();
				learn.setScreen(this);
				learn.setDescription(description);
				learnScreens.add(learn);
			}
		}
	}

	public void learn(Solvis solvis, Collection<LearnScreen> learnScreens) throws IOException {

		Iterator<LearnScreen> it = learnScreens.iterator();
		for (; it.hasNext();) {
			LearnScreen learn = it.next();
			if (learn.getScreen().isLearned(solvis)) {
				it.remove();
			} else if (learn.getScreen() == this) {
				boolean success = false;
				for (int cnt = LEARN_REPEAT_COUNT; cnt > 0 && !success; --cnt) {
					success = true;
					try {
						learn.getDescription().learn(solvis);
						it.remove();
					} catch (IOException e) {
						if (cnt <= 0) {
							throw e;
						}
					}
				}
			}
		}
		if (learnScreens.size() > 0) {
			Screen current = this;
			for (Screen nextScreen : this.nextScreens) {
				boolean success = false;
				for (int cnt = LEARN_REPEAT_COUNT; cnt > 0 && !success; --cnt) {
					try {
						Screen.gotoScreen(solvis, nextScreen, current, learnScreens);
						current = nextScreen;
						Screen cmpScreen = solvis.getCurrentScreen();
						if (cmpScreen == null || cmpScreen == nextScreen) {
							nextScreen.learn(solvis, learnScreens);
							current = solvis.getCurrentScreen();
							success = true;
						}
					} catch (IOException e) {
						if (cnt <= 0) {
							throw e;
						}
					}
				}
				// this.gotoScreen(solvis, this, current, learnObjects);
				// current = this;
			}
		}

	}

	/**
	 * current muss angelernt sein
	 * 
	 * @param solvis
	 * @param screen
	 * @param current
	 * @param learnScreens
	 * @throws IOException
	 */
	private static void gotoScreen(Solvis solvis, Screen screen, Screen current, Collection<LearnScreen> learnScreens)
			throws IOException {
		if (screen == current) {
			return;
		}
		List<Screen> previousScreens = screen.getPreviosScreens();
		boolean found = false;
		while (!found) {
			Screen next = screen;
			for (Iterator<Screen> it = previousScreens.iterator(); it.hasNext() && !found;) {
				Screen previous = it.next();
				if (previous == current) {
					found = true;
					break;
				} else {
					next = previous;
				}
			}
			if (!found) {
				current = back(solvis, current, learnScreens);
				if (screen == current) {
					found = true;
				}
			} else {
				solvis.send(next.getTouchPoint());
				current = solvis.getCurrentScreen();
				if (current == null) {
					current = next;
				}
			}
		}

	}

	/**
	 * current muss angelernt sein
	 * 
	 * @param solvis
	 * @param current
	 * @param learnScreens
	 * @throws IOException
	 */
	private static Screen back(Solvis solvis, Screen current, Collection<LearnScreen> learnScreens) throws IOException {
		Screen back = current.backScreen;
		solvis.sendBack();
		current = solvis.getCurrentScreen();
		if (current == null) {
			current = back;
			current.learn(solvis, learnScreens);
		}
		return back;
	}

	@Override
	public void learn(Solvis solvis) throws IOException {
	}

	@Override
	public int compareTo(Screen o) {
		return this.id.compareTo(o.id);
	}

	public Collection<Rectangle> getIgnoreRectangles() {
		return ignoreRectangles;
	}

}
