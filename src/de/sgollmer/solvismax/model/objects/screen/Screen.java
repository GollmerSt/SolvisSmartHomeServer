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
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.AllPreparations.PreparationRef;
import de.sgollmer.solvismax.model.objects.Preparation;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.TouchPointStrategy;
import de.sgollmer.solvismax.model.objects.configuration.Configuration;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Screen extends AbstractScreen implements Comparable<AbstractScreen> {

	private static final ILogger logger = LogManager.getInstance().getLogger(Screen.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private static final String XML_CONFIGURATION = "Configuration";
	private static final String XML_TOUCH_POINT = "TouchPoint";
	private static final String XML_USER_SELECTION = "UserSelection";
	private static final String XML_UP_TOUCH_POINT = "SequenceUp";
	private static final String XML_DOWN_TOUCH_POINT = "SequenceDown";
	private static final String XML_IDENTIFICATION = "Identification";
	private static final String XML_IGNORE_RECTANGLE = "IgnoreRectangle";
	private static final String XML_PREPARATION_REF = "PreparationRef";
	private static final String XML_LAST_PREPARATION_REF = "LastPreparationRef";

	private final String sortId;
	private final String backId;

	private final boolean ignoreChanges;
	private final boolean mustSave;

	private final TouchPointStrategy sequenceUp;
	private final TouchPointStrategy sequenceDown;

	private final Collection<Identification> identifications;

	private final Collection<Rectangle> ignoreRectangles;
	private final String lastPreparationId;
	private Preparation lastPreparation = null;
	private final boolean noRestore;

	private OfConfigs<AbstractScreen> backScreen = null;

	private Screen(final String id, final String sortId, final String previousId, final String backId,
			final boolean ignoreChanges, final boolean mustSave, final Configuration configurationMasks,
			final ISelectScreenStrategy selectScreenStrategy, final TouchPointStrategy sequenceUp,
			final TouchPointStrategy sequenceDown, final Collection<Identification> identifications,
			final Collection<Rectangle> ignoreRectangles, final String preparationId, final String lastPreparationId,
			final boolean noRestore, final boolean service) {
		super(id, previousId, preparationId, configurationMasks, selectScreenStrategy, service);
		this.sortId = sortId;
		this.backId = backId;
		this.ignoreChanges = ignoreChanges;
		this.mustSave = mustSave;
		this.sequenceUp = sequenceUp;
		this.sequenceDown = sequenceDown;
		this.identifications = identifications;
		this.ignoreRectangles = ignoreRectangles;
		this.lastPreparationId = lastPreparationId;
		this.noRestore = noRestore;
	}

	/**
	 * @return the id
	 */
	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Screen) {
			return this.getId().equals(((Screen) obj).getId());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public void assign(final SolvisDescription description)
			throws ReferenceException, XmlException, AssignmentException {

		super.assign(description);

		for (Identification identification : this.identifications) {
			identification.assign(description, this);
		}

		if (this.backId != null) {
			this.backScreen = description.getScreens().get(this.backId);
			if (this.backScreen == null) {
				throw new ReferenceException("Screen reference < " + this.backId + " > not found");
			}
		}

		if (this.sequenceDown != null) {
			this.sequenceDown.assign(description);
		}

		if (this.sequenceUp != null) {
			this.sequenceUp.assign(description);
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
	public void addNextScreen(final AbstractScreen nextScreen) {
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
	public boolean isMatchingScreen(final MyImage image, final Solvis solvis) {

		if (!this.isInConfiguration(solvis, false)) {
			return false;
		}

		if (this.lastPreparation != null && solvis.getHistory().getLastPreparation() != this.lastPreparation) {
			return false;
		}
		for (Identification identification : this.identifications) {
			if (identification.isMatchingScreen(image, solvis)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isMatchingWOGrafics(final MyImage image, final Solvis solvis) {
		for (Identification identification : this.identifications) {
			if (identification.isMatchingWOGrafics(image, solvis)) {
				return true;
			}
		}
		return false;
	}

	public boolean isLearned(final Solvis solvis) {

		for (Identification identification : this.identifications) {
			if (!identification.isLearned(solvis)) {
				return false;
			}
		}

		if (this.preparation != null) {
			return this.preparation.isLearned(solvis);
		}
		return true;
	}

	public static class Creator extends CreatorByXML<Screen> {

		private String id;
		private String sortId = null;
		private String previousId;
		private String backId;
		private boolean ignoreChanges;
		private boolean mustSave = false;
		private Configuration configurationMasks;
		private ISelectScreenStrategy selectScreenStrategy = null;
		private TouchPointStrategy sequenceUp = null;
		private TouchPointStrategy sequenceDown = null;
		private Collection<Identification> identifications = new ArrayList<>();
		private List<Rectangle> ignoreRectangles = null;
		private String preparationId = null;
		private String lastPreparationId;
		private boolean noRestore = false;
		private boolean service = false;

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "sortId":
					this.sortId = value;
					break;
				case "previousId":
					if (!value.isEmpty()) {
						this.previousId = value;
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
				case "mustSave":
					this.mustSave = Boolean.parseBoolean(value);
					break;
				case "noRestore":
					this.noRestore = Boolean.parseBoolean(value);
					break;
				case "service":
					this.service = Boolean.parseBoolean(value);
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
			return new Screen(this.id, this.sortId, this.previousId, this.backId, this.ignoreChanges, this.mustSave,
					this.configurationMasks, this.selectScreenStrategy, this.sequenceUp, this.sequenceDown,
					this.identifications, this.ignoreRectangles, this.preparationId, this.lastPreparationId,
					this.noRestore, this.service);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (name.getLocalPart()) {
				case XML_CONFIGURATION:
					return new Configuration.Creator(id, this.getBaseCreator());
				case XML_TOUCH_POINT:
				case XML_UP_TOUCH_POINT:
				case XML_DOWN_TOUCH_POINT:
					return new TouchPoint.Creator(id, this.getBaseCreator());
				case XML_USER_SELECTION:
					return new UserSelectionStrategy.Creator(id, this.getBaseCreator());
				case XML_IGNORE_RECTANGLE:
					return new Rectangle.Creator(id, this.getBaseCreator());
				case XML_IDENTIFICATION:
					return new Identification.Creator(id, this.getBaseCreator());
				case XML_PREPARATION_REF:
					return new PreparationRef.Creator(id, this.getBaseCreator());
				case XML_LAST_PREPARATION_REF:
					return new PreparationRef.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_CONFIGURATION:
					this.configurationMasks = (Configuration) created;
					break;
				case XML_TOUCH_POINT:
					this.selectScreenStrategy = new TouchPointStrategy((TouchPoint) created);
					break;
				case XML_USER_SELECTION:
					this.selectScreenStrategy = (ISelectScreenStrategy) created;
					break;
				case XML_UP_TOUCH_POINT:
					this.sequenceUp = new TouchPointStrategy((TouchPoint) created);
					break;
				case XML_DOWN_TOUCH_POINT:
					this.sequenceDown = new TouchPointStrategy((TouchPoint) created);
					break;
				case XML_IDENTIFICATION:
					this.identifications.add((Identification) created);
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

	@Override
	public void addLearnScreenGrafics(final Collection<IScreenPartCompare> descriptions, final Solvis solvis) {
		for (Identification identification : this.identifications) {
			identification.addLearnScreenGrafics(descriptions, solvis);
		}
	}

//
	public static void learnScreens(final Solvis solvis) throws IOException, TerminationException, LearningException {
		Collection<IScreenPartCompare> learnGrafics = solvis.getSolvisDescription().getLearnGrafics(solvis);
		while (learnGrafics.size() > 0) {
			solvis.getHomeScreen().learn(solvis, learnGrafics);
//			solvis.gotoHome();
		}
	}

	@Override
	public void learn(final Solvis solvis, final Collection<IScreenPartCompare> descriptions)
			throws IOException, TerminationException, LearningException {

		// Seach all LearnScreens object of current screen and learn the
		// ScreenGrafic
		boolean success = false;
		for (int cnt = Constants.LEARNING_RETRIES; cnt > 0 && !success; --cnt) {
			success = true;
			if (!this.isLearned(solvis) || this.mustSave) {
				solvis.writeLearningImage(solvis.getCurrentScreen(), this.id);
			}
			try {
				boolean learned = false;

				for (Identification identification : this.identifications) {
					learned |= identification.learn(solvis);
				}

				if (learned) {
					for (Iterator<IScreenPartCompare> it = descriptions.iterator(); it.hasNext();) {
						IScreenPartCompare toLearn = it.next();
						if (toLearn.isLearned(solvis)) {
							it.remove();
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
						success = this.goTo(solvis) != GotoStatus.FAILED;
					} catch (IOException e) {
					}
				}

			}
		}

		if (!success) {
			String message = "Screen <" + this.getId() + "> couldn't be learned. Learning terminated.";
			logger.error(message);
			throw new LearningTerminationException(message);
		}

		if (descriptions.size() > 0) { // Yes
			AbstractScreen current = this;
			// next screens could contain ScreenSequence
			for (AbstractScreen nextScreen : this.getNextScreens(solvis)) {
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
	public boolean isToBeLearning(final Solvis solvis) {
		if (!this.isLearned(solvis) || this.mustSave) {
			return true;
		}
		for (AbstractScreen screen : this.getNextScreens(solvis)) {
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
	public boolean gotoLearning(final Solvis solvis, final AbstractScreen currentScreen,
			final Collection<IScreenPartCompare> descriptions)
			throws IOException, TerminationException, LearningException {
		AbstractScreen current = currentScreen;
		if (current == null) {
			if (this != solvis.getHomeScreen()) {
				logger.log(LEARN, "Warning: Goto screen <" + this + "> not successfull, home screen is forced");
			}
			solvis.gotoHome(true);
			current = solvis.getHomeScreen();
		}
		if (this != current) {
			List<ScreenTouch> previousScreens = this.getPreviousScreenTouches(solvis);
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
						foundScreenTouch.selectScreen(solvis, this);
						current = SolvisScreen.get(solvis.getCurrentScreen());
						if (current == null) {
							current = next;
							if (next != this) {
								logger.log(LEARN,
										"Warning: Goto with an unlearned Screen, algorithm or control.xml fail?");
//								solvis.gotoHome();
								solvis.sendBack();
								logger.log(LEARN, "Pepartation failed, goto learning will tried again.");
								return false;
							}
						}
					} else {
						solvis.gotoHome();
						current = solvis.getHomeScreen();
						logger.log(LEARN, "Pepartation failed, goto learning will tried again.");
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
	private static AbstractScreen back(final Solvis solvis, final AbstractScreen currentScreen,
			Collection<IScreenPartCompare> descriptions) throws IOException, TerminationException, LearningException {
		AbstractScreen back = currentScreen.getBackScreen(solvis);
		solvis.sendBack();
		AbstractScreen current = SolvisScreen.get(solvis.getCurrentScreen());
		if (current == null) {
			solvis.gotoHome();
			current = solvis.getHomeScreen();
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
	public AbstractScreen getBackScreen(final Solvis solvis) {
		return (AbstractScreen) OfConfigs.get(solvis, this.backScreen);
	}

	public Collection<AbstractScreen> getNextScreens(final Solvis solvis) {
		List<AbstractScreen> result = new ArrayList<>(3);
		for (AbstractScreen screen : this.nextScreens) {
			if (screen.isInConfiguration(solvis, false)) {
				if (screen.getBackScreen(solvis) == screen.getPreviousScreen(solvis)) {
					result.add(0, screen);
				} else {
					result.add(screen);
				}
			}
		}

		return result;
	}

	@Override
	public GotoStatus goTo(final Solvis solvis) throws IOException, TerminationException {

		if (SolvisScreen.get(solvis.getCurrentScreen()) == this) {
			return GotoStatus.SAME;
		}

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
					return GotoStatus.CHANGED;
				}
				success = true;

			} catch (IOException e) {
				logger.info("Goto screen <" + this.getId() + "> not succcessful. Will be retried.");
			}
		}

		if (!success) {
			logger.error("Screen <" + this.getId() + "> not found.");
		}

		List<ScreenTouch> previousScreens = this.getPreviousScreenTouches(solvis);

		boolean gone = false;

		for (int cnt = 0; !gone && cnt < Constants.FAIL_REPEATS; ++cnt) {

			try {

				for (int gotoDeepth = 0; !gone && SolvisScreen.get(solvis.getCurrentScreen()) != null
						& gotoDeepth < Constants.MAX_GOTO_DEEPTH; ++gotoDeepth) {

					AbstractScreen current = SolvisScreen.get(solvis.getCurrentScreen());
					ScreenTouch foundScreenTouch = null;
					for (Iterator<ScreenTouch> it = previousScreens.iterator(); it.hasNext();) {
						ScreenTouch st = it.next();
						AbstractScreen previous = st.getScreen();
						if (previous == current) {
							foundScreenTouch = st;
							break;
						}
					}

					if (foundScreenTouch == null) {
						solvis.sendBack();
					} else {
						if (!foundScreenTouch.execute(solvis, current)) {
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
				logger.info("Goto screen <" + this.getId() + "> not succcessful. Will be retried.");
				if (cnt == 0) {
					solvis.sendBack();
				} else {
					solvis.gotoHome(); // try it from beginning
				}
			}
		}
		if (!gone) {
			logger.error("Screen <" + this.getId() + "> not found.");
		}
		return SolvisScreen.get(solvis.getCurrentScreen()) == this ? GotoStatus.CHANGED : GotoStatus.FAILED;

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
	protected void addToPreviousScreenTouches(final AbstractScreen next,
			final Collection<ScreenTouch> allPreviousScreenTouches, final Solvis solvis) {
		ISelectScreenStrategy selectScreenStrategy = next.getSelectScreenStrategy();
		Preparation preparation = next.getPreparation();

		allPreviousScreenTouches.add(new ScreenTouch(this, selectScreenStrategy, preparation));
	}

	public TouchPointStrategy getSequenceUp() {
		return this.sequenceUp;
	}

	public TouchPointStrategy getSequenceDown() {
		return this.sequenceDown;
	}

	@Override
	public boolean isNoRestore() {
		return this.noRestore;
	}

	@Override
	public String getElementType() {
		return this.getClass().getSimpleName();
	}

	public String getSortId() {
		if (this.sortId == null) {
			return this.id;
		} else {
			return this.sortId;
		}
	}

}
