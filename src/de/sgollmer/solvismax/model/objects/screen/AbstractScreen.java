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
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.configuration.ConfigurationMasks;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.objects.Rectangle;

public abstract class AbstractScreen implements IScreenLearnable, OfConfigs.IElement<AbstractScreen> {

	protected final String id;
	protected final String previousId;
	protected final ConfigurationMasks configurationMasks;
	protected List<AbstractScreen> nextScreens = new ArrayList<>(3);
	protected List<ScreenTouch> allPreviousScreenTouches = null;
	protected Preparation preparation = null;

	protected AbstractScreen(String id, String previousId, ConfigurationMasks configurationMasks) {
		this.id = id;
		this.previousId = previousId;
		this.configurationMasks = configurationMasks;
	}

	public String getId() {
		return this.id;
	}

	public abstract boolean isScreen();

	public abstract boolean isScreen(MyImage image, Solvis solvis);

	public void addNextScreen(AbstractScreen nextScreen) {
		this.nextScreens.add(nextScreen);
//
//		// TODO wozu? Optimierung der Suche abhängig von der Vielfalt der
//		// Konfigurationen des Back-Vorgängers. Sinnvoll?
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

	public abstract OfConfigs<AbstractScreen> getBackScreen();

	public abstract AbstractScreen getBackScreen(int configurationMask);

	public abstract AbstractScreen getPreviousScreen(int configurationMask);

	public abstract ConfigurationMasks getConfigurationMasks();

	public abstract TouchPoint getTouchPoint();

	public abstract boolean gotoLearning(Solvis solvis, AbstractScreen current,
			Collection<ScreenGraficDescription> desscriptions)
			throws IOException, TerminationException, LearningException;

	public abstract void learn(Solvis solvis, Collection<ScreenGraficDescription> descriptions)
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

	public List<ScreenTouch> getPreviousScreenTouches(int configurationMask) {

		if (this.allPreviousScreenTouches == null) {

			this.allPreviousScreenTouches = new ArrayList<>();

			AbstractScreen current = this;
			AbstractScreen previous = current.getPreviousScreen(configurationMask);

			while (previous != null) {

				previous.addToPreviousScreenTouches(current, this.allPreviousScreenTouches, configurationMask);

				current = previous;
				previous = current.getPreviousScreen(configurationMask);
			}
		}
		return this.allPreviousScreenTouches;
	}

	/**
	 * 
	 * @param next                     Should be selected
	 * @param allPreviousScreenTouches (write) Previous screen touchs of the screen,
	 *                                 who should be selected
	 * @param configurationMask        Configuration mask
	 */
	protected abstract void addToPreviousScreenTouches(AbstractScreen next,
			Collection<ScreenTouch> allPreviousScreenTouches, int configurationMask);

	public static class ScreenTouch {
		private final AbstractScreen screen;
		private final TouchPoint touchPoint;
		private final Preparation preparation;

		ScreenTouch(AbstractScreen previous, TouchPoint touchPoint, Preparation preparation) {
			this.screen = previous;
			this.touchPoint = touchPoint;
			this.preparation = preparation;
		}

		public AbstractScreen getScreen() {
			return this.screen;
		}

		TouchPoint getTouchPoint() {
			return this.touchPoint;
		}

		Preparation getPreparation() {
			return this.preparation;
		}

		boolean execute(Solvis solvis) throws IOException, TerminationException {
			boolean success = Preparation.prepare(this.preparation, solvis);
			if (success) {
				solvis.send(this.touchPoint);
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

	public abstract boolean isWhiteMatching(SolvisScreen screen);

	@Override
	public boolean isInConfiguration(int configurationMask) {
		if (this.configurationMasks == null) {
			return true;
		} else {
			return this.configurationMasks.isInConfiguration(configurationMask);
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

		private final int mask;

		public OptimizeComparator(int mask) {
			this.mask = mask;
		}

		@Override
		public int compare(AbstractScreen o1, AbstractScreen o2) {

			List<ScreenTouch> p1 = o1.getPreviousScreenTouches(this.mask);
			List<ScreenTouch> p2 = o2.getPreviousScreenTouches(this.mask);

			if (p1.size() == 0 && p2.size() == 0) {
				return 0;
			}

			if (p1.size() == 0) {
				return 1;
			} else if (p2.size() == 0) {
				return -1;
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

		public int getMask() {
			return this.mask;
		}
	}

}
