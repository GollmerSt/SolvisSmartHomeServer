/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.util.Collection;

public interface ScreenLearnable extends GraficsLearnable {
	
	public void createAndAddLearnScreen( LearnScreen learnScreen, Collection<LearnScreen > learnScreens, int configurationMask) ;
	
	public static class LearnScreen implements Cloneable{
		private Screen screen ;
		private ScreenGraficDescription description ;
		
		@Override
		public LearnScreen clone() {
			try {
				return (LearnScreen)super.clone() ;
			} catch (CloneNotSupportedException e) {
			}
			return null ;
		}
		
		
		public Screen getScreen() {
			return screen;
		}
		public void setScreen(Screen screen) {
			this.screen = screen;
		}
		public ScreenGraficDescription getDescription() {
			return description;
		}
		public void setDescription(ScreenGraficDescription description) {
			this.description = description;
		}
		
		@Override
		public int hashCode() {
			return 173 + this.description.hashCode() ;
		}
		
		@Override
		public boolean equals( Object obj ) {
			if ( ! (obj instanceof LearnScreen )) {
				return false ;
			} else {
				return this.description == ((LearnScreen)obj).description ;
			}
		}
	}
}
