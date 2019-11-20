package de.sgollmer.solvismax.model;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.AllDataDescriptions;
import de.sgollmer.solvismax.model.objects.AllDurations;
import de.sgollmer.solvismax.model.objects.AllScreens;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.DataDescription;
import de.sgollmer.solvismax.model.objects.Duration;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.control.Control;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.objects.Coordinate;

public class Solvis {

	private SmartHome smartHome;

	private Screen homeScreen;

	private AllScreens screens;
	private AllDataDescriptions descriptions;
	private AllDurations durations;
	private SolvisWorker worker = new SolvisWorker(this);
	private WatchDog watchDog = new WatchDog(this) ;

	private MyImage currentImage = null;
	private Screen currentScreen = null;
	private Screen savedScreen = null;
	private String measureData = null;
	private boolean screenSaverActive = false ;
	private TouchPoint resetSceenSaver = new TouchPoint(new Coordinate(0, 0), pushTimeId, releaseTimeId) ;

	private final AllSolvisData allSolvisData = new AllSolvisData(this);

	private Observer.Observable<Screen> screenChangedByUserObserable = new Observable<Screen>();

	private Object solvisObject = new Object();

	public boolean isCurrentImageValid() {
		return this.currentImage != null;
	}

	public MyImage getCurrentImage() {
		if ( this.screenSaverActive ) {
			this.send(resetSceenSaver);
			this.screenSaverActive = false ;
		}
		MyImage image = this.currentImage;
		if (image == null) {
			this.currentImage = this.getRealImage();
		}
		return image;
	}

	public MyImage getRealImage() {
		synchronized (solvisObject) {
			// TODO
			MyImage image = image;
			return image;
		}
	}

	public Screen getCurrentScreen() {
		if ( this.screenSaverActive ) {
			this.send(resetSceenSaver);
			this.screenSaverActive = false ;
		}
		Screen screen = this.currentScreen;
		if (screen == null) {
			synchronized (solvisObject) {
				screen = this.screens.getScreen(this.getCurrentImage());
				this.currentScreen = screen;
			}
		}
		return screen;
	}

	public void clearCurrentImage() {
		synchronized (solvisObject) {
			this.currentImage = null;
			this.currentScreen = null;
		}
	}

	public String getMeasureData() {
		String data = this.measureData;
		if (data == null) {
			synchronized (solvisObject) {
				// TODO
				this.measureData = data;
			}
		}
		return data;
	}

	public void clearMeasuredData() {
		this.measureData = null;
	}

	public void send(TouchPoint point) {
		synchronized (solvisObject) {
			// TODO
			this.clearCurrentImage();
		}
	}

	public void sendBack() {
		synchronized (solvisObject) {
			// TODO
			this.clearCurrentImage();
		}
	}

	public void gotoHome() {
		synchronized (solvisObject) {
			for (int cnt = 0; cnt < 4; ++cnt) {
				this.sendBack();
			}
		}
	}

	public boolean gotoScreen(Screen screen) {
		synchronized (solvisObject) {
			if (screen == null) {
				return false;
			}
			if (this.getCurrentScreen() == null) {
				this.gotoHome();
			}

			if (this.getCurrentScreen() == screen) {
				return true;
			}

			if (screen == this.homeScreen) {
				this.gotoHome();
				return true;
			}

			List<Screen> previousScreens = screen.getPreviosScreens();

			while (this.getCurrentScreen() != screen && this.getCurrentScreen() != this.homeScreen) {
				ListIterator<Screen> it = null;
				for (ListIterator<Screen> its = previousScreens.listIterator(); its.hasNext();) {
					if (its.next() == this.getCurrentScreen()) {
						it = its;
						break;
					}
				}
				if (it == null) {
					this.sendBack();
				} else {
					Screen newScreen = it.previous();
					this.send(newScreen.getTouchPoint());
				}

			}
			return this.getCurrentScreen() == screen;
		}
	}

	public void execute(Command command) {
		this.worker.push(command);
	}

	public String getValue(String description) {
		synchronized (solvisObject) {
			return this.descriptions.getValue(this, description);
		}
	}

	public String getValue(DataDescription description) {
		synchronized (solvisObject) {
			return description.getValue(this);
		}
	}

	public boolean setValue(String description, String value) {
		synchronized (solvisObject) {
			return this.descriptions.setValue(this, description, value);
		}
	}

	public boolean setValue(DataDescription description, String value) {
		synchronized (solvisObject) {
			return description.setValue(this, value);
		}
	}

	public DataDescription getDataDescription(String description) {
		return this.descriptions.get(description);
	}

	public Duration getDuration(String id) {
		return this.durations.get(id);
	}

	public WatchDog getWatchDog() {
		return this.watchDog ;

	}

	public void registerScreenChangedByUserObserver(ObserverI<Screen> observer) {
		this.screenChangedByUserObserable.register(observer);
	}

	public void notifyScreenChangedByUserObserver(Screen screen) {
		this.screenChangedByUserObserable.notify(screen);;
	}

	public void saveScreen() {
		synchronized (solvisObject) {
			this.savedScreen = this.getCurrentScreen();
		}

	}

	public void restoreScreen() {
		synchronized (solvisObject) {
			this.gotoScreen(this.savedScreen);
		}
	}

	public SmartHome getSmartHome() {
		return smartHome;
	}

	/**
	 * @return the allSolvisData
	 */
	public AllSolvisData getAllSolvisData() {
		return allSolvisData;
	}

	/**
	 * @param screenSaver the screenSaver to set
	 */
	public void setScreenSaverActive(boolean screenSaverActive) {
		this.screenSaverActive = screenSaverActive;
	}

}
