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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.FatalError;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllPreparations.PreparationRef;
import de.sgollmer.solvismax.model.objects.Preparation;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.configuration.ConfigurationMasks;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Screen implements IScreen, IScreenLearnable, Comparable<Screen> {

	private static final ILogger logger = LogManager.getInstance().getLogger(Screen.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private static final int LEARN_REPEAT_COUNT = 3;

	private static final String XML_CONFIGURATION_MASKS = "ConfigurationMasks";
	private static final String XML_TOUCH_POINT = "TouchPoint";
	private static final String XML_ALTERNATIVE_TOUCH_POINT = "AlternativeTouchPoint";
	private static final String XML_SCREEN_GRAFICS = "ScreenGrafic";
	private static final String XML_SCREEN_GRAFICS_REF = "ScreenGraficRef";
	private static final String XML_SCREEN_OCR = "ScreenOcr";
	private static final String XML_IGNORE_RECTANGLE = "IgnoreRectangle";
	private static final String XML_PREPARATION_REF = "PreparationRef";
	private static final String XML_LAST_PREPARATION_REF = "LastPreparationRef";

	private final String id;
	private final String previousId;
	private final String alternativePreviousId;
	private final String backId;
	private final boolean ignoreChanges;
	private final ConfigurationMasks configurationMasks;
	private final TouchPoint touchPoint;
	private final TouchPoint alternativeTouchPoint;
	private final Collection<IScreenCompare> screenCompares = new ArrayList<>();
	private final Collection<String> screenGraficRefs;
	private final Collection<Rectangle> ignoreRectangles;
	private final String preparationId;
	private final String lastPreparationId;
	private Preparation preparation = null;
	private Preparation lastPreparation = null;

	private OfConfigs<IScreen> previousScreen = null;
	private OfConfigs<IScreen> alternativePreviousScreen = null;
	private OfConfigs<IScreen> backScreen = null;
	private List<IScreen> nextScreens = new ArrayList<>(3);

	private Screen(String id, String previousId, String alternativePreviousId, String backId, boolean ignoreChanges,
			ConfigurationMasks configurationMasks, TouchPoint touchPoint, TouchPoint alternativeTouchPoint,
			Collection<String> screenGraficRefs, Collection<ScreenOcr> ocrs, Collection<Rectangle> ignoreRectangles,
			String preparationId, String lastPreparationId) {
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
		this.lastPreparationId = lastPreparationId;
	}

	/**
	 * @return the id
	 */
	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getName() {
		return this.getId();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof String) {
			return this.id.equals(obj);
		} else if (obj instanceof Screen) {
			return this.getId().contentEquals(((Screen) obj).getId());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	private Preparation getPreparation() {
		return this.preparation;
	}

	@Override
	public void assign(SolvisDescription description) throws ReferenceException, XmlException {
		for (String id : this.screenGraficRefs) {
			ScreenGraficDescription grafic = description.getScreenGrafics().get(id);
			if (grafic == null) {
				throw new ReferenceException("Screen grafic reference < " + id + " > not found");
			}
			this.screenCompares.add(grafic);
		}
		if ( this.screenCompares.isEmpty()) {
			throw new XmlException(
					"Error in XML definition: Grafic information of screen <" + this.getId() + "> is missing.");
		}

		if (this.backId != null) {
			this.backScreen = description.getScreens().get(this.backId);
			if (this.backScreen == null) {
				throw new ReferenceException("Screen reference < " + this.backId + " > not found");
			}
		}
		if (this.previousId != null) {
			this.previousScreen = description.getScreens().get(this.previousId);
			if (this.previousScreen == null) {
				throw new ReferenceException("Screen reference < " + this.previousId + " > not found");
			}
			for (IScreen ps : this.previousScreen.getElements()) {
				ps.addNextScreen(this);
			}
		}
		if (this.alternativePreviousId != null) {
			this.alternativePreviousScreen = description.getScreens().get(this.alternativePreviousId);
			if (this.alternativePreviousScreen == null) {
				throw new ReferenceException("Screen reference < " + this.alternativePreviousId + " > not found");
			}
		}
		if (this.touchPoint != null) {
			this.touchPoint.assign(description);
		}
		if (this.alternativeTouchPoint != null) {
			this.alternativeTouchPoint.assign(description);
		}

		if (this.preparationId != null) {
			this.preparation = description.getPreparations().get(this.preparationId);
			if (this.preparation == null) {
				throw new ReferenceException("Preparation of reference < " + this.preparationId + " > not found");
			}
			this.preparation.assign(description);
		}

		if (this.lastPreparationId != null) {
			this.lastPreparation = description.getPreparations().get(this.lastPreparationId);
			if (this.lastPreparation == null) {
				throw new ReferenceException("Preparation of reference < " + this.lastPreparationId + " > not found");
			}
			this.lastPreparation.assign(description);
		}

	}

	@Override
	public void addNextScreen(IScreen nextScreen) {
		if (!nextScreen.isScreen()) {
			throw new FatalError("Only objects of class Screen are allowed.");
		}
		int thisBack = 0;
		for (IScreen back : nextScreen.getBackScreen().getElements()) {
			if (back == this) {
				++thisBack;
			}
		}

		if (thisBack > nextScreen.getBackScreen().getElements().size() / 2) {
			this.nextScreens.add(0, nextScreen);
		} else {
			this.nextScreens.add(nextScreen);
		}
	}

	@Override
	public boolean isScreen(MyImage image, Solvis solvis) {
		if (this.lastPreparation != null && solvis.getHistory().getLastPreparation() != this.lastPreparation) {
			return false;
		}
		for (IScreenCompare grafic : this.screenCompares) {
			if (!grafic.isElementOf(image, solvis)) {
				return false;
			}
		}
		return true;
	}

	public boolean isLearned(Solvis solvis) {

		for (IScreenCompare cmp : this.screenCompares) {
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
		private final Preparation preparation;

		private ScreenTouch(Screen screen, TouchPoint touchPoint, Preparation preparation) {
			this.screen = screen;
			this.touchPoint = touchPoint;
			this.preparation = preparation;
		}

		public Screen getScreen() {
			return this.screen;
		}

		private TouchPoint getTouchPoint() {
			return this.touchPoint;
		}

		private Preparation getPreparation() {
			return this.preparation;
		}

		private boolean execute(Solvis solvis) throws IOException, TerminationException {
			boolean success = Preparation.prepare(this.preparation, solvis);
			if (success) {
				solvis.send(this.touchPoint);
			}
			return success;
		}
	}

	public List<ScreenTouch> getPreviousScreens(int configurationMask) {
		return this.getPreviousScreens(false, configurationMask);
	}

	/**
	 * Alle Bildschirme davor ohne this
	 * 
	 * @param learn
	 * @return
	 */

	private List<ScreenTouch> getPreviousScreens(boolean learn, int configurationMask) {

		List<ScreenTouch> screens = new ArrayList<>();

		Screen current = this;
		IScreen previous = current.getPreviousScreen(configurationMask);

		while (previous != null) {
			TouchPoint touch = current.getTouchPoint();
			Preparation preparation = current.getPreparation();

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
			screens.add(new ScreenTouch(previous, touch, preparation));
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
	@Override
	public IScreen getPreviousScreen(int configurationMask) {
		return (IScreen) OfConfigs.get(configurationMask, this.previousScreen);
	}

	/**
	 * @return the touchPoint
	 */
	@Override
	public TouchPoint getTouchPoint() {
		return this.touchPoint;
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
		private String lastPreparationId;

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
		public Screen create() throws XmlException {
			if (this.ignoreRectangles != null)
				Collections.sort(this.ignoreRectangles, new Comparator<Rectangle>() {

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
			return new Screen(this.id, this.previousId, this.alternativePreviousId, this.backId, this.ignoreChanges,
					this.configurationMasks, this.touchPoint, this.alternativeTouchPoint, this.screenGraficRefs,
					this.screenOcr, this.ignoreRectangles, this.preparationId, this.lastPreparationId);
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
				case XML_LAST_PREPARATION_REF:
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
				case XML_LAST_PREPARATION_REF:
					this.lastPreparationId = ((PreparationRef) created).getPreparationId();
					break;
			}

		}

	}

	private static class CreatorScreenGraficRef extends CreatorByXML<String> {

		private String refId;

		private CreatorScreenGraficRef(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			if (name.getLocalPart().equals("refId")) {
				this.refId = value;
			}

		}

		@Override
		public String create() throws XmlException {
			return this.refId;
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
	public void createAndAddLearnScreen(LearnScreen learnScreen, Collection<LearnScreen> learnScreens, Solvis solvis) {
		for (IScreenCompare cmp : this.screenCompares) {
			if (cmp instanceof ScreenGraficDescription) {
				ScreenGraficDescription description = (ScreenGraficDescription) cmp;
				if (!description.isLearned(solvis)) {
					LearnScreen learn = new LearnScreen();
					learn.setScreen(this);
					learn.setDescription(description);
					learnScreens.add(learn);
				}
			}
		}
	}

	public static void learnScreens(Solvis solvis) throws IOException {
		Collection<LearnScreen> learnScreens = solvis.getSolvisDescription().getLearnScreens(solvis);
		while (learnScreens.size() > 0) {
			solvis.getHomeScreen().learn(solvis, learnScreens, solvis.getConfigurationMask());
			IScreenLearnable.clean(learnScreens, null, solvis);
			solvis.gotoHome();
		}
	}

	public void learn(Solvis solvis, Collection<LearnScreen> learnScreens, int configurationMask)
			throws IOException, TerminationException {

		// Seach all LearnScreens object of current screen and learn the
		// ScreenGrafic
		boolean success = false;
		for (int cnt = LEARN_REPEAT_COUNT; cnt > 0 && !success; --cnt) {
			try {
				for (Iterator<LearnScreen> it = learnScreens.iterator(); it.hasNext();) {
					LearnScreen learn = it.next();
					if (learn.getScreen() == this) {
						success = true;
						learn.getDescription().learn(solvis);
						solvis.clearCurrentScreen();
					}
				}
			} catch (IOException e) {
				logger.log(LEARN,
						"Screen <" + this.getId() + "> not learned in case of IOEexception, will be tried again.");
				success = false;
				if (cnt <= 0) {
					throw e;
				}
			}
			if (success) {
				success = this.goTo(solvis);
			}
		}

		IScreenLearnable.clean(learnScreens, null, solvis);

		if (learnScreens.size() > 0) { // Yes
			Screen current = this;
			for (Screen nextScreen : this.getNextScreen(configurationMask)) {
				if (nextScreen.isToBeLearning(solvis, learnScreens, configurationMask)) {
					success = false;
					for (int cnt = LEARN_REPEAT_COUNT; cnt > 0 && !success; --cnt) {
						try {
							nextScreen.gotoLearning(solvis, current, learnScreens);
							current = nextScreen;
							Screen cmpScreen = SolvisScreen.get(solvis.getCurrentScreen());
							if (cmpScreen == null || cmpScreen == nextScreen) {
								nextScreen.learn(solvis, learnScreens, configurationMask);
								current = SolvisScreen.get(solvis.getCurrentScreen());
								if (current != null) {
									success = true;
									;
								}
							}
						} catch (IOException e) {
							logger.log(LEARN, "Screen <" + this.getId()
									+ "> not learned in case of IOEexception, will be tried again.");
							if (cnt <= 0) {
								throw e;
							}
						}
					}
					if (!success) {
						String message = "Learning of screen <" + nextScreen.getId()
								+ "> not possible. Learning terminated.";
						logger.error(message);
						throw new LearningException(message);
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
				if (current == null) {
					String message = "The follower to the screen <" + this.getId()
							+ "> is missing, please check the <control.xml> file";
					logger.error(message);
					throw new LearningException(message);
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
	 * current muss angelernt sein, Nur für den Lern-Modus!!!!!!!
	 * 
	 * @param solvis
	 * @param screen
	 * @param current
	 * @param learnScreens
	 * @throws IOException
	 */
	@Override
	public void gotoLearning(Solvis solvis, Screen current, Collection<LearnScreen> learnScreens)
			throws IOException, TerminationException {
		if (current == null) {
			if (this != solvis.getHomeScreen()) {
				logger.log(LEARN, "Warning: Goto screen <" + this + "> not successfull, home screen is forced");
			}
			solvis.gotoHome(true);
			current = solvis.getHomeScreen();
		}
		if (this == current) {
			return;
		}
		List<ScreenTouch> previousScreens = this.getPreviousScreens(true, solvis.getConfigurationMask());
		boolean gone = false;
		while (!gone) {
			ScreenTouch foundScreenTouch = null;
			Screen next = this;
			// check, if current screen is one of the previous screens of the learning
			// screen
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
				// if not, one screen back
				current = back(solvis, current, learnScreens);
			} else {
				Preparation preparation = foundScreenTouch.getPreparation();
				boolean preparationSuccess = true;
				if (preparation != null) {
					preparationSuccess = preparation.learn(solvis);
				}
				if (preparationSuccess) {
					solvis.send(foundScreenTouch.getTouchPoint());
					current = SolvisScreen.get(solvis.getCurrentScreen());
					if (current == null) {
						current = next;
						if (next != this) {
							logger.log(LEARN, "Warning: Goto with an unlearned Screen, algorithm or control.xml fail?");
						}
					}
				} else {
					solvis.gotoHome();
					current = solvis.getHomeScreen();
					logger.log(LEARN, "Pepartation failed, learning will tried again.");
				}
			}
			if (this == current) {
				gone = true;
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
		int configurationMask = solvis.getConfigurationMask();
		Screen back = current.getBackScreen(configurationMask);
		solvis.sendBack();
		current = SolvisScreen.get(solvis.getCurrentScreen());
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
		return this.ignoreRectangles;
	}

	private TouchPoint getAlternativeTouchPoint() {
		return this.alternativeTouchPoint;
	}

	@Override
	public String toString() {
		return this.getId();
	}

	public boolean isIgnoreChanges() {
		return this.ignoreChanges;
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
	public boolean isConfigurationVerified(IScreen screen) {
		if ((this.configurationMasks == null) || (screen.getConfigurationMasks() == null)) {
			return false;
		} else {
			return this.configurationMasks.isVerified(screen.getConfigurationMasks());
		}
	}

	private Screen getAlternativePreviousScreen(int configurationMask) {
		if (this.alternativePreviousScreen != null) {
			return this.alternativePreviousScreen.get(configurationMask);
		}
		return null;
	}

	@Override
	public IScreen getBackScreen(int configurationMask) {
		return (IScreen) OfConfigs.get(configurationMask, this.backScreen);
	}

	private Collection<IScreen> getNextScreen(int configurationMask) {
		Collection<IScreen> result = new ArrayList<>(3);
		for (IScreen screen : this.nextScreens) {
			if (screen.isInConfiguration(configurationMask)) {
				result.add(screen);
			}
		}
		return result;
	}

	public boolean goTo(Solvis solvis) throws IOException, TerminationException {

		if (SolvisScreen.get(solvis.getCurrentScreen()) == null) {
			solvis.gotoHome();
			if (SolvisScreen.get(solvis.getCurrentScreen()) == null) {
				solvis.gotoHome(true);
			}
		}

		if (SolvisScreen.get(solvis.getCurrentScreen()) == this) {
			return true;
		}

//		if (this == solvis.getHomeScreen()) {
//			solvis.gotoHome();
//			return true;
//		}
//
		List<ScreenTouch> previousScreens = this.getPreviousScreens(solvis.getConfigurationMask());

		boolean gone = false;

		for (int cnt = 0; !gone && cnt < Constants.FAIL_REPEATS; ++cnt) {

			for (int gotoDeepth = 0; !gone && SolvisScreen.get(solvis.getCurrentScreen()) != null
					& gotoDeepth < Constants.MAX_GOTO_DEEPTH; ++gotoDeepth) {

				// && this.getCurrentScreen() != this.homeScreen) {
				// ListIterator<Screen> it = null;
				ScreenTouch foundScreenTouch = null;
				for (Iterator<ScreenTouch> it = previousScreens.iterator(); it.hasNext();) {
					ScreenTouch st = it.next();
					Screen previous = st.getScreen();
					if (previous == SolvisScreen.get(solvis.getCurrentScreen())) {
						foundScreenTouch = st;
						break;
					}
				}

				if (foundScreenTouch == null) {
					solvis.sendBack();
				} else {
					if (!foundScreenTouch.execute(solvis)) {
						gone = false;
						break;
					}
				}

				if (SolvisScreen.get(solvis.getCurrentScreen()) == this) {
					gone = true;
				}
			}
			if (!gone) {
				solvis.gotoHome(); // try it from beginning
				logger.info("Goto screen <" + this.getId() + "> not succcessful. Will be retried.");
			}
		}
		if (!gone) {
			logger.error("Screen <" + this.getId() + "> not found.");
		}
		return SolvisScreen.get(solvis.getCurrentScreen()) == this;

	}

}
