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
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
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

/**
 * 
 * @author stefa_000
 * 
 *         At SolvisControl 2, this class represents a sequence of screens that
 *         are called up in a specific order. However, it is not determined
 *         which screen appears when you call up the sequence (e.g. system
 *         status).
 *
 */

public class ScreenSequence extends AbstractScreen {

	private static final ILogger logger = LogManager.getInstance().getLogger(ScreenSequence.class);

	private static final String XML_CONFIGURATION = "Configuration";
	private static final String XML_TOUCH_POINT = "TouchPoint";
	private static final String XML_PREPARATION_REF = "PreparationRef";
	private static final String XML_SCREEN_REF = "ScreenRef";

	private final boolean wrapArround;

	private final List<ScreenRef> screenRefs;

	private Preparation preparation = null;

	private ScreenSequence(final String id, final String previousId, final boolean wrapArround,
			final Configuration configurationMasks, final ISelectScreenStrategy selectScreenStrategy,
			final String preparationId, final List<ScreenRef> screenRefs) {
		super(id, previousId, preparationId, configurationMasks, selectScreenStrategy, false);
		this.wrapArround = wrapArround;

		this.screenRefs = screenRefs;

	}

	@Override
	public void assign(final SolvisDescription description)
			throws XmlException, AssignmentException, ReferenceException {

		super.assign(description);

		for (ScreenRef screenRef : this.screenRefs) {
			screenRef.assign(description);
		}

	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public boolean isScreen() {
		return false;
	}

	@Override
	public boolean isMatchingScreen(final MyImage image, final Solvis solvis) {
		return false;
	}

	@Override
	public void addLearnScreenGrafics(final Collection<IScreenPartCompare> learnGrafics, Solvis solvis) {
		for (ScreenRef screenRef : this.screenRefs) {
			AbstractScreen screen = screenRef.getScreen(solvis);
			if (screen != null) {
				screen.addLearnScreenGrafics(learnGrafics, solvis);
			}
		}

	}

	@Override
	public OfConfigs<AbstractScreen> getBackScreen() {
		logger.error("getBackScreen of ScreenSequence should not be executed");
		return null;
	}

	@Override
	public AbstractScreen getBackScreen(final Solvis solvis) {
		for (ScreenRef screenRef : this.screenRefs) {
			AbstractScreen screen = screenRef.getScreen(solvis);
			if (screen != null) {
				return screen.getBackScreen(solvis);
			}
		}
		return null;
	}

	@Override
	public boolean gotoLearning(final Solvis solvis, final AbstractScreen current,
			final Collection<IScreenPartCompare> descriptions) throws IOException, TerminationException {
		AbstractScreen previous = this.getPreviousScreen(solvis);
		previous.goTo(solvis);
		return previous == SolvisScreen.get(solvis.getCurrentScreen());
	}

	@Override
	public void learn(final Solvis solvis, final Collection<IScreenPartCompare> descriptions)
			throws TerminationException, IOException, LearningException {
		boolean preparationSuccess = true;
		if (this.preparation != null) {
			preparationSuccess = this.preparation.learn(solvis);
		}
		if (!preparationSuccess) {
			throw new LearningException("Prepartaion not succesfull, will be tried again");
		}
		this.getSelectScreenStrategy().execute(solvis, this);
		SolvisScreen currentScreen = solvis.getCurrentScreen();
		AbstractScreen start = SolvisScreen.get(currentScreen);
		if (start != null && !isContaining(start, solvis)) {
			throw new LearningException(
					"Selected screen is not part of ScreenSequence. Learning screen will be tried again");
		}
		if (start == null) {
			start = this.getMatching(solvis);
		}
		if (start == null) {
			throw new LearningException("White fields of the screens does not fit to the current screen.");
		}

		boolean down = true;

		while (this.isToBeLearning(solvis)) {
			Screen current = (Screen) this.getMatching(solvis);
			if (current == null) {
				throw new LearningException("White fields of the screens does not fit to the current screen.");
			}
			if (current.isToBeLearning(solvis)) {
				current.learn(solvis, descriptions);
			}

			if (this.isToBeLearning(solvis)) {
				TouchPoint touch;
				if (down) {
					touch = current.getSequenceDown().getTouchPoint();
					if (touch == null) {
						down = false;
					}
				} else {
					touch = current.getSequenceUp().getTouchPoint();
					if (touch == null) {
						throw new LearningException("Not all screens in the sequence could be learned. XML-Error?");
					}
				}

				if (touch != null) {
					solvis.send(touch);
				}
			}
		}

		start.goTo(solvis);

	}

	private boolean isContaining(final AbstractScreen screen, final Solvis solvis) {
		if (screen == null) {
			return false;
		}
		for (ScreenRef screenRef : this.screenRefs) {
			if (screenRef.getScreen(solvis) == screen) {
				return true;
			}
		}
		return false;
	}

	private AbstractScreen getMatching(final Solvis solvis)
			throws IOException, TerminationException, LearningException {
		AbstractScreen result = null;
		MyImage current = SolvisScreen.getImage(solvis.getCurrentScreen());
		for (ScreenRef screenRef : this.screenRefs) {
			AbstractScreen screen = screenRef.getScreen(solvis);
			if (screen != null && screen.isMatchingWOGrafics(current, solvis)) {
				if (result != null) {
					throw new LearningException("Screen of sequence is not unique");
				}
				result = screen;
			}
		}
		return result;
	}

	@Override
	public GotoStatus goTo(final Solvis solvis) throws IOException, TerminationException {
		logger.error("Goto a screen sequence not possible, ignored");
		return GotoStatus.CHANGED;
	}

	@Override
	public boolean isIgnoreChanges() {
		return false;
	}

	@Override
	public Collection<Rectangle> getIgnoreRectangles() {
		return null;
	}

	@Override
	public boolean isToBeLearning(final Solvis solvis) {

		for (ScreenRef screenRef : this.screenRefs) {
			AbstractScreen screen = screenRef.getScreen(solvis);
			if (screen != null && screen.isToBeLearning(solvis)) {
				return true;
			}
		}
		return false;
	}

	public static class Creator extends CreatorByXML<ScreenSequence> {

		private String id;
		private String previousId;
		private boolean wrapArround = false;

		private Configuration configurationMasks;
		private ISelectScreenStrategy selectScreen;
		private String preparationId = null;
		private List<ScreenRef> screenRefs = new ArrayList<>();

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "previousId":
					this.previousId = value;
					break;
				case "wrapArround":
					this.wrapArround = Boolean.getBoolean(value);
					break;

			}

		}

		@Override
		public ScreenSequence create() throws XmlException, IOException {
			return new ScreenSequence(this.id, this.previousId, this.wrapArround, this.configurationMasks,
					this.selectScreen, this.preparationId, this.screenRefs);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_CONFIGURATION:
					return new Configuration.Creator(id, this.getBaseCreator());
				case XML_PREPARATION_REF:
					return new Preparation.Creator(id, this.getBaseCreator());
				case XML_SCREEN_REF:
					return new ScreenRef.Creator(id, this.getBaseCreator());
				case XML_TOUCH_POINT:
					return new TouchPoint.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_CONFIGURATION:
					this.configurationMasks = (Configuration) created;
					break;
				case XML_PREPARATION_REF:
					this.preparationId = ((PreparationRef) created).getPreparationId();
					break;
				case XML_SCREEN_REF:
					ScreenRef screenRef = (ScreenRef) created;
					this.screenRefs.add(screenRef);
					break;
				case XML_TOUCH_POINT:
					this.selectScreen = new TouchPointStrategy((TouchPoint) created);
			}

		}

	}

	@Override
	protected void addToPreviousScreenTouches(final AbstractScreen next,
			final Collection<ScreenTouch> allPreviousScreenTouches, final Solvis solvis) {
		List<AbstractScreen> screenList = new ArrayList<AbstractScreen>();
		int index = -1;
		int ix = 0;
		for (ScreenRef screenRef : this.screenRefs) {
			AbstractScreen screen = screenRef.getScreen(solvis);
			if (screen != null) {
				screenList.add(screen);
				if (screen == next) {
					index = ix;
				}
				++ix;
			}
		}

		for (ix = 0; ix < screenList.size(); ++ix) {
			if (ix != index) {
				int delta = index - ix;
				if (this.wrapArround) {
					int delta2 = index - ix - screenList.size();
					if (Math.abs(delta2) < Math.abs(delta)) {
						delta = delta2;
					}
					delta2 = index - ix + screenList.size();
					if (Math.abs(delta2) < Math.abs(delta)) {
						delta = delta2;
					}
				}
				Screen screen = (Screen) screenList.get(ix);
				ISelectScreenStrategy selectScreen = delta < 0 ? screen.getSequenceUp() : screen.getSequenceDown();

				allPreviousScreenTouches.add(new ScreenTouch(screen, selectScreen, screen.getPreparation()));
			}
		}

	}

	@Override
	public boolean isMatchingWOGrafics(final MyImage image, final Solvis solvis) {
		return false;
	}

	@Override
	public AbstractScreen getSurroundScreen(final MyImage image, final Solvis solvis) {

		AbstractScreen result = null;

		if ((result = this.matches(this.getPreviousScreen(solvis), image, solvis)) != null) {
			//
		} else if ((result = this.matches(this.getBackScreen(solvis), image, solvis)) != null) {
			//
		} else {
			for (ScreenRef ref : this.screenRefs) {
				AbstractScreen screen = ref.getScreen(solvis);
				if (screen != null && screen.isMatchingScreen(image, solvis)) {
					result = screen;
					break;
				}
			}
		}
		return result;
	}

	@Override
	public String getElementType() {
		return this.getClass().getSimpleName();
	}

}
