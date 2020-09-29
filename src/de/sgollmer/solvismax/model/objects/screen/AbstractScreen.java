package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Preparation;
import de.sgollmer.solvismax.model.objects.configuration.Configuration;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.objects.Rectangle;

public abstract class AbstractScreen implements IScreenLearnable, OfConfigs.IElement<AbstractScreen> {

	protected final String id;
	protected final String previousId;
	protected final Configuration configurationMasks;
	protected List<AbstractScreen> nextScreens = new ArrayList<>(3);
	protected List<ScreenTouch> allPreviousScreenTouches = null;
	protected Preparation preparation = null;

	protected AbstractScreen(String id, String previousId, Configuration configurationMasks) {
		this.id = id;
		this.previousId = previousId;
		this.configurationMasks = configurationMasks;
	}

	public String getId() {
		return this.id;
	}

	/**
	 * This method tests whether it is the Screen class.
	 * 
	 * @return True if class of type Screen
	 */
	public abstract boolean isScreen();

	/**
	 * This method checks that the image matches the screen.
	 * 
	 * @param image
	 * @param solvis
	 * @return True if matches
	 */
	public abstract boolean isMatchingScreen(MyImage image, Solvis solvis);

	public abstract boolean isMatchingWOGrafics(MyImage image, Solvis solvis);

	public void addNextScreen(AbstractScreen nextScreen) {
		this.nextScreens.add(nextScreen);
	}

	public abstract OfConfigs<AbstractScreen> getBackScreen();

	public abstract AbstractScreen getBackScreen(Solvis solvis);

	public abstract AbstractScreen getPreviousScreen(Solvis solvis);

	public abstract Configuration getConfigurationMasks();

	public abstract ISelectScreen getSelectScreen();

	public abstract boolean gotoLearning(Solvis solvis, AbstractScreen current,
			Collection<IScreenPartCompare> desscriptions) throws IOException, TerminationException, LearningException;

	public abstract void learn(Solvis solvis, Collection<IScreenPartCompare> descriptions)
			throws IOException, TerminationException, LearningException;

	public abstract boolean goTo(Solvis solvis) throws IOException, TerminationException;

	public abstract boolean isIgnoreChanges();

	public abstract Collection<Rectangle> getIgnoreRectangles();

	public abstract boolean isToBeLearning(Solvis solvis);

	public Preparation getPreparation() {
		return this.preparation;
	}

	public static boolean isScreen(OfConfigs<AbstractScreen> cScreen) throws ReferenceException {
		boolean isScreen = false;
		AbstractScreen firstScreen = null;
		for (AbstractScreen screen : cScreen.getElements()) {
			if (firstScreen == null) {
				isScreen = screen.isScreen();
				firstScreen = screen;
			} else if (screen.isScreen() != isScreen) {
				throw new ReferenceException(
						"the screens <" + firstScreen.getId() + "> and <" + screen.getId() + "> are not compatible.");
			}
		}
		return isScreen;
	}

	public int compareTo(AbstractScreen screen) {
		return this.id.compareTo(screen.id);
	}

	public List<ScreenTouch> getPreviousScreenTouches(Solvis solvis) {

		if (this.allPreviousScreenTouches == null) {

			this.allPreviousScreenTouches = new ArrayList<>();

			AbstractScreen current = this;
			AbstractScreen previous = current.getPreviousScreen(solvis);

			while (previous != null) {

				previous.addToPreviousScreenTouches(current, this.allPreviousScreenTouches, solvis);

				current = previous;
				previous = current.getPreviousScreen(solvis);
			}
		}
		return this.allPreviousScreenTouches;
	}

	/**
	 * 
	 * @param next                     Should be selected
	 * @param allPreviousScreenTouches (write) Previous screen touches of the
	 *                                 screen, who should be selected
	 * @param configurationMask        Configuration mask
	 */
	protected abstract void addToPreviousScreenTouches(AbstractScreen next,
			Collection<ScreenTouch> allPreviousScreenTouches, Solvis solvis);

	public static class ScreenTouch {
		private final AbstractScreen screen;
		private final ISelectScreen selectScreen;
		private final Preparation preparation;

		ScreenTouch(AbstractScreen previous, ISelectScreen selectScreen, Preparation preparation) {
			this.screen = previous;
			this.selectScreen = selectScreen;
			this.preparation = preparation;
		}

		public AbstractScreen getScreen() {
			return this.screen;
		}

		Preparation getPreparation() {
			return this.preparation;
		}

		boolean selectScreen(Solvis solvis, Screen startingScreen) throws IOException, TerminationException {
			return this.selectScreen.execute(solvis, startingScreen);
		}

		boolean execute(Solvis solvis, AbstractScreen startingScreen) throws IOException, TerminationException {
			boolean success = Preparation.prepare(this.preparation, solvis);
			if (success) {
				this.selectScreen.execute(solvis, startingScreen);
			}
			return success;
		}

		@Override
		public String toString() {
			return this.screen.toString();
		}
	}

	public static boolean contains(List<ScreenTouch> list, AbstractScreen screen) {
		for (ScreenTouch screenTouch : list) {
			if (screen == screenTouch.screen) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isInConfiguration(Solvis solvis) {
		if (this.configurationMasks == null) {
			return true;
		} else {
			return this.configurationMasks.isInConfiguration(solvis);
		}
	}

	@Override
	public boolean isConfigurationVerified(AbstractScreen screen) {
		if ((this.configurationMasks == null) || (screen.getConfigurationMasks() == null)) {
			return false;
		} else {
			return this.configurationMasks.isVerified(screen.getConfigurationMasks());
		}
	}

	@Override
	public String toString() {
		return this.id;
	}

	public static class OptimizeComparator implements Comparator<AbstractScreen> {

		private final Solvis solvis;

		public OptimizeComparator(Solvis solvis) {
			this.solvis = solvis;
		}

		@Override
		public int compare(AbstractScreen o1, AbstractScreen o2) {

			List<ScreenTouch> p1 = o1.getPreviousScreenTouches(this.solvis);
			List<ScreenTouch> p2 = o2.getPreviousScreenTouches(this.solvis);

			if (p1.size() == 0 && p2.size() == 0) {
				return 0;
			}

			if (p1.size() == 0) {
				return -1;
			} else if (p2.size() == 0) {
				return 1;
			}

			ListIterator<ScreenTouch> i1 = p1.listIterator(p1.size());
			ListIterator<ScreenTouch> i2 = p2.listIterator(p2.size());

			int cmp;

			while (i1.hasPrevious()) {
				if (i2.hasPrevious()) {
					ScreenTouch s1 = i1.previous();
					ScreenTouch s2 = i2.previous();
					cmp = s1.getScreen().compareTo(s2.getScreen());
					if (cmp != 0) {
						if (i1.hasPrevious()) {
							return -1;
						} else if (i2.hasPrevious()) {
							return 1;
						}
						return cmp;
					}
				} else {
					return -1;
				}
			}
			if (i2.hasPrevious()) {
				return 1;
			}
			return 0;
		}

	}

	public boolean isNoRestore() {
		return false;
	}

	private AbstractScreen matches(AbstractScreen screen, MyImage image, Solvis solvis) {
		if (screen == null) {
			return null;
		}
		if (screen.isMatchingScreen(image, solvis)) {
			return screen;
		} else {
			return null;
		}
	}

	public AbstractScreen getSurroundScreen(MyImage image, Solvis solvis) {

		AbstractScreen result = null;

		if ((result = this.matches(this, image, solvis)) != null) {
		} //
		else if ((result = this.matches(this.getPreviousScreen(solvis), image, solvis)) != null) {
		} //
		else if ((result = this.matches(this.getBackScreen(solvis), image, solvis)) != null) {
		} //
		else {
			for (AbstractScreen screen : this.nextScreens) {
				if (screen.isMatchingScreen(image, solvis)) {
					result = screen;
					break;
				}
			}
		}
		if (result == null && this.getPreviousScreen(solvis) instanceof ScreenSequence) {
			result = this.getPreviousScreen(solvis).getSurroundScreen(image, solvis);
		}

		return result;
	}

}
