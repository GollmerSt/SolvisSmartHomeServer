/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.objects.Assigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Mode implements Assigner, ModeI {

	private static final String XML_TOUCH = "Touch";
	private static final String XML_SCREEN_GRAFIC = "ScreenGrafic";

	private final String id;
	private final TouchPoint touch;
	private final ScreenGraficDescription grafic;

	public Mode(String id, TouchPoint touch, ScreenGraficDescription grafic) {
		this.id = id;
		this.touch = touch;
		this.grafic = grafic;
	}

	/**
	 * @return the grafic
	 */
	public ScreenGraficDescription getGrafic() {
		return grafic;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the touch
	 */
	public TouchPoint getTouch() {
		return touch;
	}

	@Override
	public void assign(SolvisDescription description) {
		if (this.touch != null) {
			this.touch.assign(description);
		}

	}

	public static class Creator extends CreatorByXML<Mode> {

		private String id;
		private TouchPoint touch;
		private ScreenGraficDescription grafic;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
			}

		}

		@Override
		public Mode create() throws XmlError, IOException {
			return new Mode(id, touch, grafic);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_TOUCH:
					return new TouchPoint.Creator(id, this.getBaseCreator());
				case XML_SCREEN_GRAFIC:
					return new ScreenGraficDescription.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_TOUCH:
					this.touch = (TouchPoint) created;
					break;
				case XML_SCREEN_GRAFIC:
					this.grafic = (ScreenGraficDescription) created;
					break;
			}
		}

	}

	@Override
	public String getName() {
		return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Mode) {
			return this.id.equals(((Mode) obj).id);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

}