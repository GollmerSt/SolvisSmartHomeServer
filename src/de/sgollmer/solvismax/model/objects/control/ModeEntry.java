/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.WhiteGraficRectangle;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.screen.IScreenPartCompare;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficDescription;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class ModeEntry implements IMode<ModeEntry> {

	private static final String XML_GUI_SET = "GuiSet";
	private static final String XML_TOUCH = "Touch";
	private static final String XML_SCREEN_GRAFIC = "ScreenGrafic";
	private static final String XML_MUST_BE_WHITE = "MustBeWhite";

	private final String id;
	private final GuiSet guiSet;
	private final Collection<WhiteGraficRectangle> whiteGraficRectangles;
	private final Handling handling;

	private ModeEntry(final String id, final GuiSet guiSet,
			final Collection<WhiteGraficRectangle> whiteGraficRectangles, final Handling handling) {
		this.id = id;
		this.guiSet = guiSet;
		this.whiteGraficRectangles = whiteGraficRectangles;
		this.handling = handling;
	}

	/**
	 * @return the id
	 */
	String getId() {
		return this.id;
	}

	GuiSet getGuiSet() {
		return this.guiSet;
	}

	static class Creator extends CreatorByXML<ModeEntry> {

		private String id;
		private GuiSet guiSet;
		private final Collection<WhiteGraficRectangle> whiteGraficRectangles = new ArrayList<>();
		private Handling handling = Handling.RW;

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "handling":
					this.handling = Handling.valueOf(value);
					break;
			}

		}

		@Override
		public ModeEntry create() throws XmlException, IOException {
			return new ModeEntry(this.id, this.guiSet, this.whiteGraficRectangles, this.handling);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_GUI_SET:
					return new GuiSet.Creator(id, this.getBaseCreator());
				case XML_MUST_BE_WHITE:
					return new WhiteGraficRectangle.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_GUI_SET:
					this.guiSet = (GuiSet) created;
					break;
				case XML_MUST_BE_WHITE:
					this.whiteGraficRectangles.add((WhiteGraficRectangle) created);
			}
		}

	}

	@Override
	public String getName() {
		return this.id;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ModeEntry) {
			return this.id.equals(((ModeEntry) obj).id);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	static class GuiSet {
		private final TouchPoint touch;
		private final ScreenGraficDescription grafic;

		private GuiSet(final TouchPoint touch, final ScreenGraficDescription grafic) {
			this.touch = touch;
			this.grafic = grafic;
		}

		TouchPoint getTouch() {
			return this.touch;
		}

		ScreenGraficDescription getGrafic() {
			return this.grafic;
		}

		private static class Creator extends CreatorByXML<GuiSet> {

			private TouchPoint touch;
			private ScreenGraficDescription grafic;

			private Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) {
			}

			@Override
			public GuiSet create() throws XmlException, IOException {
				return new GuiSet(this.touch, this.grafic);
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
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
			public void created(final CreatorByXML<?> creator, final Object created) {
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

	}

	boolean isXmlValid() {
		return this.guiSet != null;
	}

	public Collection<WhiteGraficRectangle> getWhiteGraficRectangles() {
		return this.whiteGraficRectangles;
	}

	public boolean isMatchingWhite(final MyImage image, final Solvis solvis) {
		for (IScreenPartCompare screenPart : this.whiteGraficRectangles) {
			if (!screenPart.isElementOf(image, solvis)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int compareTo(final ModeEntry o) {
		if (o == null) {
			return 1;
		}
		return this.id.compareTo(o.getId());
	}

	@Override
	public ModeValue<?> create(final long timeStamp) {
		return new ModeValue<>(this, timeStamp);
	}

	@Override
	public Handling getHandling() {
		return this.handling;
	}

	@Override
	public String getCvsMeta() {
		return this.id + this.handling.getCvsMeta();
	}

}
