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
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
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

public class ScreenSequence extends AbstractScreen {

	private static final ILogger logger = LogManager.getInstance().getLogger(ScreenSequence.class);

	private static final String XML_CONFIGURATION_MASKS = "ConfigurationMasks";
	private static final String XML_TOUCH_POINT = "TouchPoint";
	private static final String XML_PREPARATION_REF = "PreparationRef";
	private static final String XML_SCREEN_REF = "ScreenRef";

	private final boolean wrapArround;

	private final TouchPoint touchPoint;
	private final String preparationId;
	private final List<ScreenRef> screenRefs;

	private Preparation preparation = null;
	private OfConfigs<AbstractScreen> previousScreen;

	public ScreenSequence(String id, String previousId, boolean wrapArround, ConfigurationMasks configurationMasks,
			TouchPoint touchPoint, String preparationId, List<ScreenRef> screenRefs) {
		super(id, previousId, configurationMasks);
		this.wrapArround = wrapArround;

		this.touchPoint = touchPoint;
		this.preparationId = preparationId;
		this.screenRefs = screenRefs;

	}

	@Override
	public String getName() {
		return this.id;
	}

	@Override
	public void assign(SolvisDescription description) throws XmlException, AssignmentException, ReferenceException {

		if (this.previousId != null) {
			OfConfigs<AbstractScreen> screen = description.getScreens().get(this.previousId);
			if (screen == null) {
				throw new ReferenceException("Screen reference < " + this.previousId + " > not found");
			}
			if (!AbstractScreen.isScreen(screen)) {
				throw new ReferenceException(
						"Screen reference < " + this.previousId + " > must be an element <Screen>.");
			}

			this.previousScreen = screen;

			for (AbstractScreen ps : screen.getElements()) {
				ps.addNextScreen(this);
			}
		}

		for (ScreenRef screenRef : this.screenRefs) {
			screenRef.assign(description);
		}

		if (this.preparationId != null) {
			this.preparation = description.getPreparations().get(this.preparationId);
			if (this.preparation == null) {
				throw new AssignmentException("Preparation of id <" + this.preparationId + "> not found.");
			}
		}

		if (this.touchPoint != null) {
			this.touchPoint.assign(description);
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
	public boolean isScreen(MyImage image, Solvis solvis) {
		return false;
	}

	@Override
	public void addLearnScreenGrafics(Collection<ScreenGraficDescription> learnGrafics, Solvis solvis) {
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
	public AbstractScreen getBackScreen(int configurationMask) {
		for (ScreenRef screenRef : this.screenRefs) {
			AbstractScreen screen = screenRef.getScreen(configurationMask);
			if (screen != null) {
				return screen.getBackScreen(configurationMask);
			}
		}
		return null;
	}

	@Override
	public AbstractScreen getPreviousScreen(int configurationMask) {
		return (AbstractScreen) OfConfigs.get(configurationMask, this.previousScreen);
	}

	@Override
	public ConfigurationMasks getConfigurationMasks() {
		return this.configurationMasks;
	}

	@Override
	public TouchPoint getTouchPoint() {
		return this.touchPoint;
	}

	@Override
	public boolean gotoLearning(Solvis solvis, AbstractScreen current, Collection<ScreenGraficDescription> descriptions)
			throws IOException, TerminationException {
		AbstractScreen previous = this.getPreviousScreen(solvis.getConfigurationMask());
		previous.goTo(solvis);
		return previous == SolvisScreen.get(solvis.getCurrentScreen());
	}

	@Override
	public void learn(Solvis solvis, Collection<ScreenGraficDescription> descriptions)
			throws TerminationException, IOException, LearningException {
		boolean preparationSuccess = true;
		if (this.preparation != null) {
			preparationSuccess = this.preparation.learn(solvis);
		}
		if (!preparationSuccess) {
			throw new LearningException("Prepartaion not succesfull, will be tried again");
		}
		solvis.send(this.getTouchPoint());
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
			TouchPoint touch;
			if (down) {
				touch = current.getSequenceDown();
				if (touch == null) {
					down = false;
				}
			} else {
				touch = current.getSequenceUp();
				if (touch == null) {
					throw new LearningException("Not all screens in the sequence could be learned. XML-Error?");
				}
			}

			if (touch != null) {
				solvis.send(touch);
			}
		}

	}

	private boolean isContaining(AbstractScreen screen, Solvis solvis) {
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

	private AbstractScreen getMatching(Solvis solvis) throws IOException, TerminationException, LearningException {
		AbstractScreen result = null;
		SolvisScreen current = solvis.getCurrentScreen();
		for (ScreenRef screenRef : this.screenRefs) {
			AbstractScreen screen = screenRef.getScreen(solvis);
			if (screen != null && screen.isWhiteMatching(current)) {
				if (result != null) {
					throw new LearningException("Screen of sequence is not unique");
				}
				result = screen;
			}
		}
		return result;
	}

	@Override
	public boolean goTo(Solvis solvis) throws IOException, TerminationException {
		logger.error("Goto a screen sequence not possible, ignored");
		return true;
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
	public boolean isToBeLearning(Solvis solvis) {

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

		private ConfigurationMasks configurationMasks;
		private TouchPoint touchPoint;
		private String preparationId = null;
		private List<ScreenRef> screenRefs = new ArrayList<>();

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
				case "wrapArround":
					this.wrapArround = Boolean.getBoolean(value);
					break;

			}

		}

		@Override
		public ScreenSequence create() throws XmlException, IOException, AssignmentException, ReferenceException {
			return new ScreenSequence(this.id, this.previousId, this.wrapArround, this.configurationMasks,
					this.touchPoint, this.preparationId, this.screenRefs);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_CONFIGURATION_MASKS:
					return new ConfigurationMasks.Creator(id, this.getBaseCreator());
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
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_CONFIGURATION_MASKS:
					this.configurationMasks = (ConfigurationMasks) created;
					break;
				case XML_PREPARATION_REF:
					this.preparationId = ((PreparationRef) created).getPreparationId();
					break;
				case XML_SCREEN_REF:
					ScreenRef screenRef = (ScreenRef) created;
					this.screenRefs.add(screenRef);
					break;
				case XML_TOUCH_POINT:
					this.touchPoint = (TouchPoint) created;
			}

		}

	}

	@Override
	protected void addToPreviousScreenTouches(AbstractScreen next, Collection<ScreenTouch> allPreviousScreenTouches,
			int configurationMask) {
		List<AbstractScreen> screenList = new ArrayList<AbstractScreen>();
		int index = -1;
		int ix = 0;
		for (ScreenRef screenRef : this.screenRefs) {
			AbstractScreen screen = screenRef.getScreen(configurationMask);
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
				TouchPoint touch = delta < 0 ? screen.getSequenceUp() : screen.getSequenceDown();

				allPreviousScreenTouches.add(new ScreenTouch(next, touch, screen.getPreparation()));
			}
		}

	}

	@Override
	public boolean isWhiteMatching(SolvisScreen screen) {
		return false;
	}

}
