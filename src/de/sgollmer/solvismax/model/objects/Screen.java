/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.error.ReferenceError;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllPreparations.PreparationRef;
import de.sgollmer.solvismax.model.objects.configuration.ConfigurationMasks;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Screen implements ScreenLearnable, Comparable<Screen>, OfConfigs.Element<Screen> {

	private static final Logger logger = LogManager.getLogger(Screen.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private final int LEARN_REPEAT_COUNT = 3;

	private static final String XML_CONFIGURATION_MASKS = "ConfigurationMasks";
	private static final String XML_TOUCH_POINT = "TouchPoint";
	private static final String XML_ALTERNATIVE_TOUCH_POINT = "AlternativeTouchPoint";
	private static final String XML_SCREEN_GRAFICS = "ScreenGrafic";
	private static final String XML_SCREEN_GRAFICS_REF = "ScreenGraficRef";
	private static final String XML_SCREEN_OCR = "ScreenOcr";
	private static final String XML_IGNORE_RECTANGLE = "IgnoreRectangle";
	private static final String XML_PREPARATION_REF = "PreparationRef";

	private final String id;
	private final String previousId;
	private final String alternativePreviousId;
	private final String backId;
	private final boolean ignoreChanges;
	private final ConfigurationMasks configurationMasks;
	private final TouchPoint touchPoint;
	private final TouchPoint alternativeTouchPoint;
	private final Collection<ScreenCompare> screenCompares = new ArrayList<>();
	private final Collection<String> screenGraficRefs;
	private final Collection<Rectangle> ignoreRectangles;
	private final String preparationId;
	private Preparation preparation = null;

	public OfConfigs<Screen> previousScreen = null;
	public OfConfigs<Screen> alternativePreviousScreen = null;
	public OfConfigs<Screen> backScreen = null;
	public List<Screen> nextScreens = new ArrayList<>(3);

	public Screen(String id, String previousId, String alternativePreviousId, String backId, boolean ignoreChanges,
			ConfigurationMasks configurationMasks, TouchPoint touchPoint, TouchPoint alternativeTouchPoint,
			Collection<String> screenGraficRefs, Collection<ScreenOcr> ocrs, Collection<Rectangle> ignoreRectangles,
			String preparationId) {
		this.id = id;
		this.previousId = previousId;
		this.alternativePreviousId = alternativePreviousId;
		this.backId = backId;
		this.ignoreChanges = ignoreChanges;
		this.configurationMasks = configurationMasks;
		this.touchPoint = touchPoint;
		this.alternativeTouchPoint = alternativeTouchPoint;
		this.screenGraficRefs = screenGraficRefs;
		this.screenCompares.addAll(ocrs);
		this.ignoreRectangles = ignoreRectangles;
		this.preparationId = preparationId;
	}

	/**
	 * @return the id
	 */
	@Override
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

	public boolean prepare(Solvis solvis) throws IOException, TerminationException {
		if (this.preparation == null) {
			return true;
		} else {
			return this.preparation.execute(solvis);
		}
	}

	public Preparation getPreparation() {
		return this.preparation;
	}

	@Override
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
			for (Screen ps : this.previousScreen.getElements()) {
				ps.addNextScreen(this);
			}
		}
		if (alternativePreviousId != null) {
			this.alternativePreviousScreen = description.getScreens().get(alternativePreviousId);
			if (this.alternativePreviousScreen == null) {
				throw new ReferenceError("Screen reference < " + this.alternativePreviousId + " > not found");
			}
		}
		if (this.touchPoint != null) {
			this.touchPoint.assign(description);
		}
		if (this.alternativeTouchPoint != null) {
			this.alternativeTouchPoint.assign(description);
		}

		if (preparationId != null) {
			this.preparation = description.getPreparations().get(preparationId);
			if (this.preparation == null) {
				throw new ReferenceError("Preparation of reference < " + this.preparationId + " > not found");
			}
			this.preparation.assign(description);
		}

	}

	public void addNextScreen(Screen nextScreen) {
		int thisBack = 0;
		for (Screen back : nextScreen.backScreen.getElements()) {
			if (back == this) {
				++thisBack;
			}
		}

		if (thisBack > nextScreen.backScreen.getElements().size() / 2) {
			this.nextScreens.add(0, nextScreen);
		} else {
			this.nextScreens.add(nextScreen);
		}
	}

	public boolean isScreen(MyImage image, Solvis solvis) {
		if (this.screenCompares.isEmpty()) {
			throw new XmlError(
					"Error in XML definition: Grafic information of screen <" + this.getId() + "> is missing.");
		}
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
		if (this.preparation != null) {
			return this.preparation.isLearned(solvis);
		}
		return true;
	}

	public static class ScreenTouch {
		private final Screen screen;
		private final TouchPoint touchPoint;

		public ScreenTouch(Screen screen, TouchPoint touchPoint) {
			this.screen = screen;
			this.touchPoint = touchPoint;
		}

		public Screen getScreen() {
			return screen;
		}

		public TouchPoint getTouchPoint() {
			return touchPoint;
		}
	}

	public List<ScreenTouch> getPreviosScreens(int configurationMask) {
		return this.getPreviousScreens(false, configurationMask);
	}

	/**
	 * Alle Bildschirme davor ohne this
	 * 
	 * @param learn
	 * @return
	 */

	public List<ScreenTouch> getPreviousScreens(boolean learn, int configurationMask) {

		List<ScreenTouch> screens = new ArrayList<>();

		Screen current = this;
		Screen previous = current.getPreviousScreen(configurationMask);

		while (previous != null) {
			TouchPoint touch = current.getTouchPoint();

			Screen altScreen = current.getAlternativePreviousScreen(configurationMask);
			if (altScreen != null && !learn) {
				if (previous.getAlternativePreviousScreen(configurationMask) != null) {
					if (contains(screens, previous)) {
						previous = altScreen;
						touch = current.getAlternativeTouchPoint();
					}
				} else if (altScreen.getAlternativePreviousScreen(configurationMask) != null) {
					if (!contains(screens, altScreen)) {
						previous = altScreen;
						touch = current.getAlternativeTouchPoint();
					}
				}
			}
			screens.add(new ScreenTouch(previous, touch));
			current = previous;
			previous = current.getPreviousScreen(configurationMask);
		}
		return screens;
	}

	private static boolean contains(List<ScreenTouch> list, Screen screen) {
		for (ScreenTouch screenTouch : list) {
			if (screen == screenTouch.screen) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the previousScreen
	 */
	public Screen getPreviousScreen(int configurationMask) {
		if (previousScreen == null) {
			return null;
		} else {
			return previousScreen.get(configurationMask);
		}
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
		private String alternativePreviousId = null;
		private String backId;
		private boolean ignoreChanges;
		private ConfigurationMasks configurationMasks;
		private TouchPoint touchPoint;
		private TouchPoint alternativeTouchPoint = null;
		private Collection<String> screenGraficRefs = new ArrayList<>();
		private Collection<ScreenOcr> screenOcr = new ArrayList<>();
		private List<Rectangle> ignoreRectangles = null;
		private String preparationId = null;

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
					if (!value.isEmpty()) {
						this.previousId = value;
					}
					break;
				case "alternativePreviousId":
					if (!value.isEmpty()) {
						this.alternativePreviousId = value;
					}
					break;
				case "backId":
					if (!value.isEmpty()) {
						this.backId = value;
					}
					break;
				case "ignoreChanges":
					this.ignoreChanges = Boolean.parseBoolean(value);
					break;
			}

		}

		@Override
		public Screen create() throws XmlError {
			if (ignoreRectangles != null)
				Collections.sort(ignoreRectangles, new Comparator<Rectangle>() {

					@Override
					public int compare(Rectangle o1, Rectangle o2) {
						if (o1.isInvertFunction() == o2.isInvertFunction()) {
							return 0;
						} else if (o1.isInvertFunction()) {
							return -1;
						} else
							return 1;
					}
				});
			return new Screen(id, previousId, alternativePreviousId, backId, ignoreChanges, configurationMasks,
					touchPoint, alternativeTouchPoint, screenGraficRefs, screenOcr, ignoreRectangles, preparationId);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (name.getLocalPart()) {
				case XML_CONFIGURATION_MASKS:
					return new ConfigurationMasks.Creator(id, this.getBaseCreator());
				case XML_TOUCH_POINT:
					return new TouchPoint.Creator(id, this.getBaseCreator());
				case XML_ALTERNATIVE_TOUCH_POINT:
					return new TouchPoint.Creator(id, this.getBaseCreator());
				case XML_SCREEN_GRAFICS:
					return new ScreenGraficDescription.Creator(id, this.getBaseCreator());
				case XML_SCREEN_GRAFICS_REF:
					return new CreatorScreenGraficRef(id, this.getBaseCreator());
				case XML_SCREEN_OCR:
					return new ScreenOcr.Creator(id, getBaseCreator());
				case XML_IGNORE_RECTANGLE:
					return new Rectangle.Creator(id, getBaseCreator());
				case XML_PREPARATION_REF:
					return new PreparationRef.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_CONFIGURATION_MASKS:
					this.configurationMasks = (ConfigurationMasks) created;
					break;
				case XML_TOUCH_POINT:
					this.touchPoint = (TouchPoint) created;
					break;
				case XML_ALTERNATIVE_TOUCH_POINT:
					this.alternativeTouchPoint = (TouchPoint) created;
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
					if (this.ignoreRectangles == null) {
						this.ignoreRectangles = new ArrayList<>();
					}
					this.ignoreRectangles.add((Rectangle) created);
					break;
				case XML_PREPARATION_REF:
					this.preparationId = ((PreparationRef) created).getPreparationId();
					break;
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
	public void createAndAddLearnScreen(LearnScreen learnScreen, Collection<LearnScreen> learnScreens,
			int configurationMask) {
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

	public void learn(Solvis solvis, Collection<LearnScreen> learnScreens, int configurationMask)
			throws IOException, TerminationException {

		// Seach all LearnScreens object of current screen and learn the
		// ScreenGrafic
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
						solvis.clearCurrentImage();
						it.remove();
					} catch (IOException e) {
						logger.log(LEARN, "Screen <" + learn.getScreen().getId()
								+ "> not learned in case of IOEexception, will be tried again.");
						success = false;
						if (cnt <= 0) {
							throw e;
						}
					}
				}
			}
		}

		if (learnScreens.size() > 0) { // Yes
			Screen current = this;
			for (Screen nextScreen : this.getNextScreen(configurationMask)) {
				if (nextScreen.isToBeLearning(solvis, learnScreens, configurationMask)) {
					boolean success = false;
					for (int cnt = LEARN_REPEAT_COUNT; cnt > 0 && !success; --cnt) {
						try {
							Screen gotoScreen = nextScreen;
							Preparation preparation = nextScreen.getPreparation();
							if (preparation != null) {
								gotoScreen = nextScreen.getPreviousScreen(configurationMask);
								Screen clearScreen = preparation.getClearScreen(configurationMask);
								if (clearScreen != null) {
									Screen.gotoScreenLearning(solvis, clearScreen, current, learnScreens,
											configurationMask);
								}
							}
							Screen.gotoScreenLearning(solvis, gotoScreen, current, learnScreens, configurationMask);
							if (preparation != null) {
								preparation.learn(solvis, gotoScreen);
								preparation.execute(solvis);
								Screen.gotoScreenLearning(solvis, nextScreen, current, learnScreens, configurationMask);
							}
							current = nextScreen;
							Screen cmpScreen = solvis.getCurrentScreen();
							if (cmpScreen == null || cmpScreen == nextScreen) {
								nextScreen.learn(solvis, learnScreens, configurationMask);
								current = solvis.getCurrentScreen();
								success = current != null;
							}
						} catch (IOException e) {
							logger.log(LEARN, "Screen <" + this.getId()
									+ "> not learned in case of IOEexception, will be tried again.");
							if (cnt <= 0) {
								throw e;
							}
						}
					}
				}
				// this.gotoScreen(solvis, this, current, learnObjects);
				// current = this;
			}
		}
		if (this.getNextScreen(configurationMask).size() == 0 && this.alternativePreviousId != null) {
			Screen alter = this.getAlternativePreviousScreen(configurationMask);
			Screen current = this;
			boolean found = false;
			TouchPoint currentTouch = current.getTouchPoint();
			TouchPoint previousTouch = null;
			while (!found) {
				Screen previous = current.getPreviousScreen(configurationMask);
				previousTouch = current.getTouchPoint();
				if (previous == alter) {
					previous = current.getAlternativePreviousScreen(configurationMask);
					previousTouch = current.getAlternativeTouchPoint();
				}
				if (previous == this) {
					found = true;
				} else {
					current = previous;
					currentTouch = previousTouch;
				}
			}
			solvis.send(currentTouch);
		}
	}

	private boolean isToBeLearning(Solvis solvis, Collection<LearnScreen> learnScreens, int configurationMask) {
		if (!this.isLearned(solvis)) {
			for (LearnScreen learnScreen : learnScreens) {
				if (learnScreen.getScreen() == this) {
					return true;
				}
			}
		}
		for (Screen screen : this.getNextScreen(configurationMask)) {
			if (screen.isToBeLearning(solvis, learnScreens, configurationMask)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * current muss angelernt sein, Nur fü den Lern-Modus!!!!!!!
	 * 
	 * @param solvis
	 * @param screen
	 * @param current
	 * @param learnScreens
	 * @throws IOException
	 */
	public static void gotoScreenLearning(Solvis solvis, Screen screen, Screen current,
			Collection<LearnScreen> learnScreens, int configurationMask) throws IOException, TerminationException {
		if (current == null) {
			String homeId = solvis.getSolvisDescription().getHomeId();
			if (! homeId.equals(screen.getId())) {
				logger.warn("Goto screen <" + screen + "> not succesfull, home screen is forced");
			}
			solvis.gotoHome();
			current = solvis.getSolvisDescription().getScreens().get(homeId, solvis.getConfigurationMask());
		}
		if (screen == current) {
			return;
		}
		List<ScreenTouch> previousScreens = screen.getPreviousScreens(true, configurationMask);
		boolean found = false;
		while (!found) {
			ScreenTouch foundScreenTouch = null;
			Screen next = screen;
			for (Iterator<ScreenTouch> it = previousScreens.iterator(); it.hasNext();) {
				ScreenTouch st = it.next();
				Screen previous = st.getScreen();
				if (previous == current) {
					foundScreenTouch = st;
					break;
				} else {
					next = previous;
				}
			}
			if (foundScreenTouch == null) {
				current = back(solvis, current, learnScreens, configurationMask);
				if (screen == current) {
					found = true;
				}
			} else {
				found = true;
				solvis.send(foundScreenTouch.getTouchPoint());
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
	private static Screen back(Solvis solvis, Screen current, Collection<LearnScreen> learnScreens,
			int configurationMask) throws IOException {
		Screen back = current.getBackScreen(configurationMask);
		solvis.sendBack();
		current = solvis.getCurrentScreen();
		if (current == null) {
			current = back;
			if (learnScreens != null) {
				current.learn(solvis, learnScreens, configurationMask);
			}
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

	public TouchPoint getAlternativeTouchPoint() {
		return alternativeTouchPoint;
	}

	@Override
	public String toString() {
		return this.getId();
	}

	public boolean isIgnoreChanges() {
		return ignoreChanges;
	}

	@Override
	public boolean isInConfiguration(int configurationMask) {
		if (this.configurationMasks == null) {
			return true;
		} else {
			return this.configurationMasks.isInConfiguration(configurationMask);
		}
	}

	@Override
	public boolean isConfigurationVerified(Screen screen) {
		if ((this.configurationMasks == null) || (screen.configurationMasks == null)) {
			return false;
		} else {
			return this.configurationMasks.isVerified(screen.configurationMasks);
		}
	}

	public Screen getAlternativePreviousScreen(int configurationMask) {
		if (alternativePreviousScreen != null) {
			return this.alternativePreviousScreen.get(configurationMask);
		}
		return null;
	}

	public Screen getBackScreen(int configurationMask) {
		if (backScreen == null) {
			return null;
		} else {
			return backScreen.get(configurationMask);
		}
	}

	public Collection<Screen> getNextScreen(int configurationMask) {
		Collection<Screen> result = new ArrayList<>(3);
		for (Screen screen : this.nextScreens) {
			if (screen.isInConfiguration(configurationMask)) {
				result.add(screen);
			}
		}
		return result;
	}

	public ConfigurationMasks getConfigurationMask() {
		return configurationMasks;
	}

}
