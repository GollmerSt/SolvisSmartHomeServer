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
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.LearningTerminationException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
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

public class Screen extends AbstractScreen implements Comparable<AbstractScreen> {

	private static final ILogger logger = LogManager.getInstance().getLogger(Screen.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private static final String XML_CONFIGURATION_MASKS = "ConfigurationMasks";
	private static final String XML_TOUCH_POINT = "TouchPoint";
	private static final String XML_UP_TOUCH_POINT = "SequenceUp";
	private static final String XML_DOWN_TOUCH_POINT = "SequenceDown";
	private static final String XML_ALTERNATIVE_TOUCH_POINT = "AlternativeTouchPoint";
	private static final String XML_SCREEN_GRAFICS = "ScreenGrafic";
	private static final String XML_SCREEN_GRAFICS_REF = "ScreenGraficRef";
	private static final String XML_SCREEN_OCR = "ScreenOcr";
	private static final String XML_IGNORE_RECTANGLE = "IgnoreRectangle";
	private static final String XML_MUST_BE_WHITE = "MustBeWhite";
	private static final String XML_PREPARATION_REF = "PreparationRef";
	private static final String XML_LAST_PREPARATION_REF = "LastPreparationRef";

	private final String alternativePreviousId;
	private final String backId;

	private final boolean ignoreChanges;

	private final TouchPoint touchPoint;
	private final TouchPoint sequenceUp;
	private final TouchPoint sequenceDown;

	private final Collection<IScreenPartCompare> screenCompares;
	private final Collection<String> screenGraficRefs;
	private final Collection<Rectangle> ignoreRectangles;
	private final String preparationId;
	private final String lastPreparationId;
	private Preparation lastPreparation = null;

	private OfConfigs<AbstractScreen> previousScreen = null;
	private OfConfigs<AbstractScreen> alternativePreviousScreen = null;
	private OfConfigs<AbstractScreen> backScreen = null;

	private Screen(String id, String previousId, String alternativePreviousId, String backId, boolean ignoreChanges,
			ConfigurationMasks configurationMasks, TouchPoint touchPoint, TouchPoint sequenceUp,
			TouchPoint sequenceDown, Collection<String> screenGraficRefs, Collection<IScreenPartCompare> screenCompares,
			Collection<Rectangle> ignoreRectangles, String preparationId, String lastPreparationId) {
		super(id, previousId, configurationMasks);
		this.alternativePreviousId = alternativePreviousId;
		this.backId = backId;
		this.ignoreChanges = ignoreChanges;
		this.touchPoint = touchPoint;
		this.sequenceUp = sequenceUp;
		this.sequenceDown = sequenceDown;
		this.screenGraficRefs = screenGraficRefs;
		this.screenCompares = screenCompares;
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

	@Override
	public void assign(SolvisDescription description) throws ReferenceException, XmlException, AssignmentException {
		for (String id : this.screenGraficRefs) {
			ScreenGraficDescription grafic = description.getScreenGrafics().get(id);
			if (grafic == null) {
				throw new ReferenceException("Screen grafic reference < " + id + " > not found");
			}
			this.screenCompares.add(grafic);
		}
		if (this.screenCompares.isEmpty()) {
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
			OfConfigs<AbstractScreen> screen = description.getScreens().get(this.previousId);
			if (screen == null) {
				throw new ReferenceException("Screen reference <" + this.previousId + "> not found");
			}
			this.previousScreen = screen;

			for (AbstractScreen ps : this.previousScreen.getElements()) {
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

		if (this.sequenceDown != null) {
			this.sequenceDown.assign(description);
		}

		if (this.sequenceUp != null) {
			this.sequenceUp.assign(description);
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
	public void addNextScreen(AbstractScreen nextScreen) {
		this.nextScreens.add(nextScreen);
//
//		int thisBack = 0;
//		for (AbstractScreen back : nextScreen.getBackScreen().getElements()) {
//			if (back == this) {
//				++thisBack;
//			}
//		}
//
//		if (thisBack > nextScreen.getBackScreen().getElements().size() / 2) {
//			this.nextScreens.add(0, nextScreen);
//		} else {
//			this.nextScreens.add(nextScreen);
//		}
	}

	@Override
	public boolean isMatchingScreen(MyImage image, Solvis solvis) {
		if (this.lastPreparation != null && solvis.getHistory().getLastPreparation() != this.lastPreparation) {
			return false;
		}
		for (IScreenPartCompare screenPart : this.screenCompares) {
			if (!screenPart.isElementOf(image, solvis)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isMatchingWOGrafics(MyImage image, Solvis solvis) {
		for (IScreenPartCompare screenPart : this.screenCompares) {
			if (!(screenPart instanceof ScreenGraficDescription) && !screenPart.isElementOf(image, solvis)) {
				return false;
			}
		}
		return true;
	}

	public boolean isLearned(Solvis solvis) {

		for (IScreenPartCompare cmp : this.screenCompares) {
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

	/**
	 * @return the previousScreen
	 */
	@Override
	public AbstractScreen getPreviousScreen(int configurationMask) {
		return (AbstractScreen) OfConfigs.get(configurationMask, this.previousScreen);
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
		private TouchPoint touchPoint = null;
		private TouchPoint sequenceUp = null;
		private TouchPoint sequenceDown = null;
		private final Collection<String> screenGraficRefs = new ArrayList<>();
		private final Collection<IScreenPartCompare> screenCompares = new ArrayList<>();
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
					this.configurationMasks, this.touchPoint, this.sequenceUp, this.sequenceDown, this.screenGraficRefs,
					this.screenCompares, this.ignoreRectangles, this.preparationId, this.lastPreparationId);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (name.getLocalPart()) {
				case XML_CONFIGURATION_MASKS:
					return new ConfigurationMasks.Creator(id, this.getBaseCreator());
				case XML_TOUCH_POINT:
				case XML_UP_TOUCH_POINT:
				case XML_DOWN_TOUCH_POINT:
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
				case XML_MUST_BE_WHITE:
					return new WhiteGraficRectangle.Creator(id, getBaseCreator());
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
				case XML_UP_TOUCH_POINT:
					this.sequenceUp = (TouchPoint) created;
					break;
				case XML_DOWN_TOUCH_POINT:
					this.sequenceDown = (TouchPoint) created;
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
					this.screenCompares.add((ScreenOcr) created);
					break;
				case XML_MUST_BE_WHITE:
					this.screenCompares.add((WhiteGraficRectangle) created);
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
	public void addLearnScreenGrafics(Collection<ScreenGraficDescription> descriptions, Solvis solvis) {
		for (IScreenPartCompare cmp : this.screenCompares) {
			if (cmp instanceof ScreenGraficDescription) {
				ScreenGraficDescription description = (ScreenGraficDescription) cmp;
				if (!description.isLearned(solvis) && !descriptions.contains(description)) {
					descriptions.add(description);
				}
			}
		}
	}

	public static void learnScreens(Solvis solvis) throws IOException, TerminationException, LearningException {
		Collection<ScreenGraficDescription> learnGrafics = solvis.getSolvisDescription().getLearnGrafics(solvis);
		while (learnGrafics.size() > 0) {
			solvis.getHomeScreen().learn(solvis, learnGrafics);
//			solvis.gotoHome();
		}
	}

	@Override
	public void learn(Solvis solvis, Collection<ScreenGraficDescription> descriptions)
			throws IOException, TerminationException, LearningException {

		// Seach all LearnScreens object of current screen and learn the
		// ScreenGrafic
		boolean success = false;
		for (int cnt = Constants.LEARNING_RETRIES; cnt > 0 && !success; --cnt) {
			success = true;
			solvis.getCurrentScreen().writeLearningImage( this.id );
			try {
				for (IScreenPartCompare screenPartCompare : this.screenCompares) {
					if (screenPartCompare instanceof ScreenGraficDescription) {
						ScreenGraficDescription description = (ScreenGraficDescription) screenPartCompare;
						if (!description.isLearned(solvis)) {
							description.learn(solvis);
							descriptions.remove(description);
						}
					}
				}
				solvis.clearCurrentScreen();
			} catch (IOException e) {
				logger.log(LEARN,
						"Screen <" + this.getId() + "> not learned in case of IOEexception, will be tried again.");
				success = false;
				if (cnt <= 0) {
					throw e;
				}
			}
			if (success) {
				for (int gotoRetries = Constants.FAIL_REPEATS; gotoRetries > 0 && !success; --gotoRetries) {
					try {
						success = false;
						success = this.goTo(solvis);
					} catch (IOException e) {
					}
				}

			}
		}

		if (!success) {
			logger.log(LEARN, "Screen <" + this.getId() + "> couldn't be learned.");
			// TODO Abbrechen?
		}

		if (descriptions.size() > 0) { // Yes
			AbstractScreen current = this;
			// next screens could contain ScreenSequence
			for (AbstractScreen nextScreen : this.getNextScreen(solvis)) {
				if (nextScreen.isToBeLearning(solvis)) {
					success = false;
					for (int cnt = Constants.LEARNING_RETRIES; cnt >= 0 && !success; --cnt) {
						try {
							if (nextScreen.gotoLearning(solvis, current, descriptions)) {
								nextScreen.learn(solvis, descriptions);
								current = SolvisScreen.get(solvis.getCurrentScreen());
								if (current != null) {
									success = true;
								}
							}
						} catch (LearningTerminationException e) {
							throw e;
						} catch (IOException e) {
							logger.log(LEARN, "Screen <" + nextScreen.getId()
									+ "> not learned in case of IOEexception, will be tried again.", e);
							if (cnt <= 0) {
								throw e;
							}
						} catch (LearningException e) {
							logger.log(LEARN, "Screen <" + nextScreen.getId() + "> not learned. " + e.getMessage());
						}
						if (!success) {
							solvis.sendBack();
							current = SolvisScreen.get(solvis.getCurrentScreen());
						}
					}
					if (!success) {
						String message = "Learning of screen <" + nextScreen.getId()
								+ "> not possible. Learning terminated.";
						logger.error(message);
						throw new LearningTerminationException(message);
					}
				}
			}
		}
	}

	@Override
	public boolean isToBeLearning(Solvis solvis) {
		if (!this.isLearned(solvis)) {
			return true;
		}
		for (AbstractScreen screen : this.getNextScreen(solvis)) {
			if (screen.isToBeLearning(solvis)) {
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
	 * @throws LearningException
	 */
	@Override
	public boolean gotoLearning(Solvis solvis, AbstractScreen current, Collection<ScreenGraficDescription> descriptions)
			throws IOException, TerminationException, LearningException {
		if (current == null) {
			if (this != solvis.getHomeScreen()) {
				logger.log(LEARN, "Warning: Goto screen <" + this + "> not successfull, home screen is forced");
			}
			solvis.gotoHome(true);
			current = solvis.getHomeScreen();
		}
		if (this != current) {
			List<ScreenTouch> previousScreens = this.getPreviousScreenTouches(solvis.getConfigurationMask());
			boolean gone = false;
			while (!gone) {
				ScreenTouch foundScreenTouch = null;
				AbstractScreen next = this;
				// check, if current screen is one of the previous screens of the learning
				// screen
				for (Iterator<ScreenTouch> it = previousScreens.iterator(); it.hasNext();) {
					ScreenTouch st = it.next();
					AbstractScreen previous = st.getScreen();
					if (previous == current) {
						foundScreenTouch = st;
						break;
					} else {
						next = previous;
					}
				}
				if (foundScreenTouch == null) {
					// if not, one screen back
					current = back(solvis, current, descriptions);
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
								logger.log(LEARN,
										"Warning: Goto with an unlearned Screen, algorithm or control.xml fail?");
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
		AbstractScreen cmpScreen = SolvisScreen.get(solvis.getCurrentScreen());
		return cmpScreen == null || cmpScreen == this;
	}

	/**
	 * current muss angelernt sein
	 * 
	 * @param solvis
	 * @param current
	 * @param learnScreens
	 * @throws IOException
	 * @throws TerminationException
	 * @throws LearningException
	 */
	private static AbstractScreen back(Solvis solvis, AbstractScreen current,
			Collection<ScreenGraficDescription> descriptions)
			throws IOException, TerminationException, LearningException {
		int configurationMask = solvis.getConfigurationMask();
		AbstractScreen back = current.getBackScreen(configurationMask);
		solvis.sendBack();
		current = SolvisScreen.get(solvis.getCurrentScreen());
		if (current == null) {
			// TODO ist das sinnvoll. Wenn Back nicht funktioniert kann es Probleme geben
			// TODO besser von Beginn anfangen
			current = back;
			if (descriptions != null) {
				current.learn(solvis, descriptions);
			}
		}
		return back;
	}

	@Override
	public Collection<Rectangle> getIgnoreRectangles() {
		return this.ignoreRectangles;
	}

	@Override
	public boolean isIgnoreChanges() {
		return this.ignoreChanges;
	}

	@Override
	public AbstractScreen getBackScreen(int configurationMask) {
		return (AbstractScreen) OfConfigs.get(configurationMask, this.backScreen);
	}

	private Collection<AbstractScreen> getNextScreen(Solvis solvis) {
		int mask = solvis.getConfigurationMask();
		List<AbstractScreen> result = new ArrayList<>(3);
		for (AbstractScreen screen : this.nextScreens) {
			if (screen.isInConfiguration(mask)) {
				if (screen.getBackScreen(mask) == screen.getPreviousScreen(mask)) {
					result.add(0, screen);
				} else {
					result.add(screen);
				}
			}
		}

		return result;
	}

	@Override
	public boolean goTo(Solvis solvis) throws IOException, TerminationException {

		boolean success = false;
		for (int failCnt = 0; !success && failCnt < Constants.FAIL_REPEATS; ++failCnt) {
			try {

				for (int cnt = 0; cnt < Constants.FAIL_REPEATS
						&& SolvisScreen.get(solvis.getCurrentScreen()) == null; ++cnt) {
					solvis.sendBack();
				}

				if (SolvisScreen.get(solvis.getCurrentScreen()) == null) {
					solvis.gotoHome(true);
				}

				if (SolvisScreen.get(solvis.getCurrentScreen()) == this) {
					return true;
				}
				success = true ;

			} catch (IOException e) {
				logger.info("Goto screen <" + this.getId() + "> not succcessful. Will be retried.");
			}
		}
		
		if ( !success ) {
			logger.error("Screen <" + this.getId() + "> not found.");
		}

		List<ScreenTouch> previousScreens = this.getPreviousScreenTouches(solvis.getConfigurationMask());

		boolean gone = false;

		for (int cnt = 0; !gone && cnt < Constants.FAIL_REPEATS; ++cnt) {

			try {

				for (int gotoDeepth = 0; !gone && SolvisScreen.get(solvis.getCurrentScreen()) != null
						& gotoDeepth < Constants.MAX_GOTO_DEEPTH; ++gotoDeepth) {

					ScreenTouch foundScreenTouch = null;
					for (Iterator<ScreenTouch> it = previousScreens.iterator(); it.hasNext();) {
						ScreenTouch st = it.next();
						AbstractScreen previous = st.getScreen();
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
			} catch (IOException e) {
				gone = false;
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

	@Override
	public boolean isScreen() {
		return true;
	}

	@Override
	public OfConfigs<AbstractScreen> getBackScreen() {
		return this.backScreen;
	}

	@Override
	public ConfigurationMasks getConfigurationMasks() {
		return this.configurationMasks;
	}

	@Override
	protected void addToPreviousScreenTouches(AbstractScreen next, Collection<ScreenTouch> allPreviousScreenTouches,
			int configurationMask) {
		TouchPoint touch = next.getTouchPoint();
		Preparation preparation = next.getPreparation();

		allPreviousScreenTouches.add(new ScreenTouch(this, touch, preparation));
	}

	public TouchPoint getSequenceUp() {
		return this.sequenceUp;
	}

	public TouchPoint getSequenceDown() {
		return this.sequenceDown;
	}

	private static class WhiteGraficRectangle implements IScreenPartCompare {

		private Rectangle rectangle;

		public WhiteGraficRectangle(Rectangle rectangle) {
			this.rectangle = rectangle;
		}

		@Override
		public boolean isElementOf(MyImage image, Solvis solvis) {
			return image.isWhite(this.rectangle) != this.rectangle.isInvertFunction();
		}

		public static class Creator extends CreatorByXML<WhiteGraficRectangle> {

			private final Rectangle.Creator rectangeleCreator;;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
				this.rectangeleCreator = new Rectangle.Creator(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				this.rectangeleCreator.setAttribute(name, value);

			}

			@Override
			public WhiteGraficRectangle create()
					throws XmlException, IOException, AssignmentException, ReferenceException {
				Rectangle rectangle = this.rectangeleCreator.create();
				return new WhiteGraficRectangle(rectangle);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				return this.rectangeleCreator.getCreator(name);
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) throws XmlException {
				this.rectangeleCreator.created(creator, created);

			}

		}

	}

}
