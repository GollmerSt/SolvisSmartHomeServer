package de.sgollmer.solvismax.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.AllScreens;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.objects.Coordinate;

public class Solvis {

	private Screen homeScreen;

	private AllScreens screens;

	private MyImage currentImage = null;
	private Screen currentScreen = null;

	public MyImage getCurrentImage() {
		if (this.currentImage == null) {
			// TODO
		}
		return this.currentImage;
	}

	public Screen getCurrentScreen() {
		if (this.currentScreen == null) {
			this.currentScreen = this.screens.getScreen(this.getCurrentImage());
		}
		return this.currentScreen ;
	}
	
	public void clearCurrent() {
		this.currentImage = null ;
		this.currentScreen = null ;
	}

	public void send(TouchPoint point ) {
		// TODO
		this.clearCurrent();
	}

	public void sendBack() {
		// TODO
		this.clearCurrent();
	}

	public void gotoHome() {

		while (this.getCurrentScreen() != this.homeScreen) {
			this.sendBack();
		}
	}

	public boolean gotoScreen( Screen screen ) {
		
		if ( this.getCurrentScreen() == null ) {
			this.gotoHome(); 
		}

		if ( this.getCurrentScreen() == screen ) {
			return true ;
		}
		
		if  ( screen == this.homeScreen ) {
			this.gotoHome() ;
			return true ;
		}
		
		List< Screen > previousScreens = new ArrayList<>() ;
		
		Screen ref = screen ;
		
		while( ref.getPreviousScreen() != null ) {
			ref = ref.getPreviousScreen() ;
			previousScreens.add( ref) ;
		}
		
		while ( this.getCurrentScreen() != screen && this.getCurrentScreen() != this.homeScreen ) {
			int pos = previousScreens.indexOf( this.getCurrentScreen() ) ;
			if ( pos < 0 ) {
				this.sendBack(); 
			} else {
				Screen newScreen = previousScreens.get(pos-1) ;
				for ( TouchPoint point : newScreen.getTouchPoints() ) {
					this.send(point);
				}
			}
			
		}
		return this.getCurrentScreen() == screen ;
	}

}
