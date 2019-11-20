package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.sgollmer.solvismax.error.ReferenceError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;

public class Screen {
	private final String id;
	private final String previousId;
	private final String backId;
	private final TouchPoint touchPoint;
	private final Collection<ScreenCompare> screenCompares = new ArrayList<>();
	private final Collection<String> screenGraficRefs = new ArrayList<>();

	public Screen previousScreen = null;
	public Screen backScreen = null;
	public Collection<Screen> nextScreens = new ArrayList<>();

	public Screen(String id, String previousId, String backId, TouchPoint touchPoint) {
		this.id = id;
		this.previousId = previousId;
		this.backId = backId;
		this.touchPoint = touchPoint ;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the backId
	 */
	public String getBackId() {
		return backId;
	}

	@Override
	public boolean equals(Object obj) {
		return this.id.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	public void assign(AllScreenGrafics allScreenGrafics) throws ReferenceError {
		for (String id : screenGraficRefs) {
			ScreenGrafic grafic = allScreenGrafics.get(id);
			if (grafic == null) {
				throw new ReferenceError("Screen grafic reference < " + id + " > not found");
			}
			this.screenCompares.add(grafic);
		}
	}

	public void assign(AllScreens screens) {
		this.backScreen = screens.get(backId);
		if (this.backScreen == null) {
			throw new ReferenceError("Screen reference < " + this.backId + " > not found");
		}
		this.previousScreen = screens.get(previousId);
		if (this.previousScreen == null) {
			throw new ReferenceError("Screen reference < " + this.previousId + " > not found");
		}
		this.previousScreen.addNextScreen(this);
	}

	public void addNextScreen(Screen nextScreen) {
		this.nextScreens.add(nextScreen);
	}

	public boolean isScreen(MyImage image) {
		for (ScreenCompare grafic : this.screenCompares) {
			if (!grafic.isElementOf(image)) {
				return false;
			}
		}
		return true;
	}

	public List<Screen> getPreviosScreens() {

		List<Screen> screens = new ArrayList<>();

		while (this.getPreviousScreen() != null ) {
			screens.add(this.getPreviousScreen());
		}
		return screens ;
	}

	/**
	 * @return the previousScreen
	 */
	public Screen getPreviousScreen() {
		return previousScreen;
	}

	/**
	 * @return the touchPoint
	 */
	public TouchPoint getTouchPoint() {
		return touchPoint;
	}

}
