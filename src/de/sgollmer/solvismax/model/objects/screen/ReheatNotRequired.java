package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;
import java.util.Collection;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.configuration.Configuration;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.XmlException;

public class ReheatNotRequired extends AbstractScreen {

	private final String backId;

	private OfConfigs<AbstractScreen> previousScreen = null;
	private OfConfigs<AbstractScreen> backScreen = null;

	protected ReheatNotRequired(final String id, final String previousId, final String backId,
			final Configuration configurationMasks) {
		super(id, previousId, configurationMasks);
		this.backId = backId;
	}

	@Override
	public void addLearnScreenGrafics(Collection<IScreenPartCompare> learnGrafics, Solvis solvis) {
	}

	@Override
	public String getElementType() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void assign(SolvisDescription description) throws XmlException, AssignmentException, ReferenceException {

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

	}

	@Override
	public boolean isScreen() {
		return true;
	}

	@Override
	public boolean isMatchingScreen(MyImage image, Solvis solvis) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isMatchingWOGrafics(MyImage image, Solvis solvis) {
		return isMatchingScreen(image, solvis);
	}

	@Override
	public OfConfigs<AbstractScreen> getBackScreen() {
		return this.backScreen;
	}

	@Override
	public AbstractScreen getBackScreen(Solvis solvis) {
		// TODO Auto-generated method stub
		return this.backScreen.get(solvis);
	}

	@Override
	public AbstractScreen getPreviousScreen(Solvis solvis) {
		return this.previousScreen.get(solvis);
	}

	@Override
	public ISelectScreen getSelectScreen() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean gotoLearning(Solvis solvis, AbstractScreen current, Collection<IScreenPartCompare> desscriptions)
			throws IOException, TerminationException, LearningException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void learn(Solvis solvis, Collection<IScreenPartCompare> descriptions)
			throws IOException, TerminationException, LearningException {
		// TODO Auto-generated method stub

	}

	@Override
	public GotoStatus goTo(Solvis solvis) throws IOException, TerminationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isIgnoreChanges() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Rectangle> getIgnoreRectangles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isToBeLearning(Solvis solvis) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void addToPreviousScreenTouches(AbstractScreen next, Collection<ScreenTouch> allPreviousScreenTouches,
			Solvis solvis) {
		// TODO Auto-generated method stub

	}

}
