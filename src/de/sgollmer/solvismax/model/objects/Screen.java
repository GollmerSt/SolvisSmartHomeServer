package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.ReferenceError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Screen {
	private final String id;
	private final String previousId;
	private final String backId;
	private final TouchPoint touchPoint;
	private final Collection<ScreenCompare> screenCompares = new ArrayList<>();
	private final Collection<String> screenGraficRefs = new ArrayList<>(2);

	public Screen previousScreen = null;
	public Screen backScreen = null;
	public Collection<Screen> nextScreens = new ArrayList<>(3);

	public Screen(String id, String previousId, String backId, TouchPoint touchPoint) {
		this.id = id;
		this.previousId = previousId;
		this.backId = backId;
		this.touchPoint = touchPoint;
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

	public void assign(SolvisDescription description ) throws ReferenceError {
		for (String id : screenGraficRefs) {
			ScreenGrafic grafic = description.getScreenGrafics().get(id);
			if (grafic == null) {
				throw new ReferenceError("Screen grafic reference < " + id + " > not found");
			}
			this.screenCompares.add(grafic);
		}
		
		
		if (backId != null) {
			this.backScreen = description.getScreens().get(backId);
			if (this.backScreen == null) {
				throw new ReferenceError("Screen reference < " + this.backId + " > not found");
			}
		}
		if (previousId != null) {
			this.previousScreen = description.getScreens().get(previousId);
			if (this.previousScreen == null) {
				throw new ReferenceError("Screen reference < " + this.previousId + " > not found");
			}
			this.previousScreen.addNextScreen(this);
		}
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

		while (this.getPreviousScreen() != null) {
			screens.add(this.getPreviousScreen());
		}
		return screens;
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

	public static class Creator extends CreatorByXML<Screen> {

		private String id;
		private String previousId;
		private String backId;
		private TouchPoint touchPoint;
		private Collection<String> screenGraficRefs = new ArrayList<>();

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
				case "backId":
					this.backId = value;
					break;
			}

		}

		@Override
		public Screen create() throws XmlError {
			return new Screen(id, previousId, backId, touchPoint);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (name.getLocalPart()) {
				case "TouchPoint":
					return new TouchPoint.Creator(id, this.getBaseCreator());
				case "ScreenGrafic":
					return new ScreenGrafic.Creator(id, this.getBaseCreator());
				case "ScreenGraficRef":
					return new CreatorScreenGraficRef(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case "TouchPoint":
					this.touchPoint = (TouchPoint) created;
					break;
				case "ScreenGrafic":
					ScreenGrafic grafic = (ScreenGrafic) created;
					this.screenGraficRefs.add(grafic.getId());
					((SolvisDescription.Creator) this.getBaseCreator()).getScreenGrafics().add(grafic);
					break;
				case "ScreenGraficRef":
					this.screenGraficRefs.add((String) created);
					break;
			}

		}

	}

	private static class CreatorScreenGraficRef extends CreatorByXML<String> {

		private String refId;

		public CreatorScreenGraficRef(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			if (name.getLocalPart().equals("refId")) {
				this.refId = value;
			}

		}

		@Override
		public String create() throws XmlError {
			return refId;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
		}

	}

}
