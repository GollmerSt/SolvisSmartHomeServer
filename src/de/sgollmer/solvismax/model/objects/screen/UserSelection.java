package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.HelperException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.Ocr;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class UserSelection {

	private static final String XML_RECTANGLES = "Rectangle";
	private static final String XML_UPPER = "Upper";
	private static final String XML_LOWER = "Lower";

	private final Collection<Digit> digits;

	public UserSelection(Collection<Digit> digits) {
		this.digits = digits;
	}

	public boolean execute(Solvis solvis, Screen startingScreen) throws IOException, TerminationException {

		boolean success = false;

		Collection<Digit> toSet = new ArrayList<>();

		for (Digit digit : this.digits) {
			if (!digit.isCode(solvis)) {
				toSet.add(digit);
			}
		}

		for (int repeatFail = 0; !success && repeatFail < Constants.FAIL_REPEATS; ++repeatFail) {
			success = true;
			if (repeatFail > 0) {
				solvis.gotoHome();
				startingScreen.goTo(solvis);
			}
			try {

				for (Iterator<Digit> it = toSet.iterator(); it.hasNext();) {
					Digit digit = it.next();

					boolean adjusted = false;

					for (int repeat = 0; !adjusted && repeat < Constants.SET_REPEATS + 1; ++repeat) {

						adjusted = digit.exec(solvis);

						if (!it.hasNext()) {
							adjusted = SolvisScreen.get(solvis.getCurrentScreen()) != startingScreen;
						}

					}
					if (!adjusted) {
						success = false;
						throw new HelperException();
					}
				}
			} catch (HelperException he) {
				success = false;
			}
		}
		return success;
	}

	public static class Creator extends CreatorByXML<UserSelection> {

		private Collection<Digit> digits;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public UserSelection create() throws XmlException, IOException, AssignmentException, ReferenceException {
			return new UserSelection(this.digits);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return new Digit.Creator(name.getLocalPart(), this.getBaseCreator());
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
			this.digits.add((Digit) created);

		}

	}

	public static class Digit {
		private final int value;
		private final Rectangle rectangle;
		private final TouchPoint upper;
		private final TouchPoint lower;

		private Digit(int value, Rectangle rectangle, TouchPoint upper, TouchPoint lower) {
			this.value = value;
			this.rectangle = rectangle;
			this.upper = upper;
			this.lower = lower;
		}

		public boolean exec(Solvis solvis) throws IOException, TerminationException, NumberFormatException {

			int diff = this.value - this.getCurrent(solvis) ;

			if (diff == 0) {
				return true;
			}

			TouchPoint touch;

			if (diff > 0) {
				touch = this.upper;
			} else {
				touch = this.lower;
				diff = -diff;
			}

			for (int i = 0; i < diff; ++i) {
				solvis.send(touch);
			}
			return false;
		}
		
		public int getCurrent( Solvis solvis) throws IOException, TerminationException, NumberFormatException {
			MyImage image = SolvisScreen.getImage(solvis.getCurrentScreen());

			Ocr ocr = new Ocr(image, this.rectangle);
			int value = Integer.parseInt(ocr.toString());

			return value;
			
		}

		public boolean isCode(Solvis solvis) throws IOException, TerminationException, NumberFormatException {
			return this.value == this.getCurrent(solvis);

		}

		public static class Creator extends CreatorByXML<Digit> {

			private int value;
			private Rectangle rectangle;
			private TouchPoint upper;
			private TouchPoint lower;

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {
				switch (name.getLocalPart()) {
					case "value":
						this.value = Integer.parseInt(value);
						break;
				}
			}

			@Override
			public Digit create() throws XmlException, IOException, AssignmentException, ReferenceException {
				return new Digit(this.value, this.rectangle, this.upper, this.lower);
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				String id = name.getLocalPart();
				switch (id) {
					case XML_RECTANGLES:
						return new Rectangle.Creator(id, this.getBaseCreator());
					case XML_UPPER:
						return new TouchPoint.Creator(id, this.getBaseCreator());
					case XML_LOWER:
						return new TouchPoint.Creator(id, this.getBaseCreator());
				}
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) throws XmlException {
				switch (creator.getId()) {
					case XML_RECTANGLES:
						this.rectangle = (Rectangle) created;
						break;
					case XML_UPPER:
						this.upper = (TouchPoint) created;
						break;
					case XML_LOWER:
						this.lower = (TouchPoint) created;
						break;
				}
			}

		}
	}
}
