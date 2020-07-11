/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.util.Collection;
import java.util.Iterator;

import de.sgollmer.solvismax.model.Solvis;

public interface IScreenLearnable extends IGraficsLearnable {

	public static void clean(Collection<LearnScreen> learnScreens, Screen screen, Solvis solvis) {
		for (Iterator<LearnScreen> it = learnScreens.iterator(); it.hasNext();) {
			LearnScreen learnScreen = it.next();
			if ((screen == null || screen == learnScreen.getScreen())
					&& learnScreen.getDescription().isLearned(solvis)) {
				it.remove();
			}
		}
	}

	public void createAndAddLearnScreen(LearnScreen learnScreen, Collection<LearnScreen> learnScreens, Solvis solvis);

	public static class LearnScreen implements Cloneable {
		private Screen screen;
		private ScreenGraficDescription description;

		@Override
		public LearnScreen clone() {
			try {
				return (LearnScreen) super.clone();
			} catch (CloneNotSupportedException e) {
			}
			return null;
		}

		public Screen getScreen() {
			return this.screen;
		}

		public void setScreen(Screen screen) {
			this.screen = screen;
		}

		public ScreenGraficDescription getDescription() {
			return this.description;
		}

		public void setDescription(ScreenGraficDescription description) {
			this.description = description;
		}

		@Override
		public int hashCode() {
			return 173 + this.description.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof LearnScreen)) {
				return false;
			} else {
				return this.description == ((LearnScreen) obj).description;
			}
		}

	}
}
