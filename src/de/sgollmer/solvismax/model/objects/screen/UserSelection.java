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
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.Ocr;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Duration;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class UserSelection implements ISelectScreen {

	private static final String XML_RECTANGLES = "Rectangle";
	private static final String XML_UPPER = "Upper";
	private static final String XML_LOWER = "Lower";

	private final Collection<Digit> digits;
	private final String waitTimeAfterLastDigitRefId;

	private UserSelection(final Collection<Digit> digits, final String waitTimeAfterLastDigitRefId) {
		this.digits = digits;
		this.waitTimeAfterLastDigitRefId = waitTimeAfterLastDigitRefId;
	}

	@Override
	public boolean execute(final Solvis solvis, final AbstractScreen startingScreen)
			throws IOException, TerminationException {

		boolean success = false;

		Collection<Digit> toSet = new ArrayList<>();

		for (Digit digit : this.digits) {
			if (!digit.isCode(solvis)) {
				toSet.add(digit);
			}
		}

		Duration duration = solvis.getDuration(this.waitTimeAfterLastDigitRefId);
		int waitTimeAfterLastDigit_ms = duration.getTime_ms();

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
							AbortHelper.getInstance().sleep(waitTimeAfterLastDigit_ms);
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

		private final Collection<Digit> digits = new ArrayList<>();
		private String waitTimeAfterLastDigitRefId;

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "waitTimeAfterLastDigitRefId":
					this.waitTimeAfterLastDigitRefId = value;
			}
		}

		@Override
		public UserSelection create() throws XmlException, IOException {
			return new UserSelection(this.digits, this.waitTimeAfterLastDigitRefId);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			return new Digit.Creator(name.getLocalPart(), this.getBaseCreator());
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {
			this.digits.add((Digit) created);

		}

	}

	public static class Digit {
		private final int digit;
		private final Rectangle rectangle;
		private final TouchPoint upper;
		private final TouchPoint lower;

		private Digit(final int digit, final Rectangle rectangle, final TouchPoint upper, final TouchPoint lower) {
			this.digit = digit;
			this.rectangle = rectangle;
			this.upper = upper;
			this.lower = lower;
		}

		public boolean exec(final Solvis solvis) throws IOException, TerminationException, NumberFormatException {

			Calc calc = new Calc(this.getCurrent(solvis));
			boolean adjusted;

			if (calc.cnt == 0) {
				adjusted = true;
			} else {
				for (int i = 0; i < calc.cnt; ++i) {
					solvis.send(calc.touch);
				}
				adjusted = false;
			}
			return adjusted;
		}

		private class Calc {
			private final int cnt;
			private final TouchPoint touch;

			public Calc(final int current) {
				int diff = Digit.this.digit - current;
				if (diff == 0) {
					this.cnt = 0;
					this.touch = null;
				} else {
					if (Math.abs(10 - diff) < Math.abs(diff)) {
						diff -= 10;
					}
					TouchPoint touch = Digit.this.upper;

					if (diff < 0) {
						touch = Digit.this.lower;
						diff = -diff;
					}
					this.cnt = diff;
					this.touch = touch;
				}
			}

		}

		public int getSettingTime(final Solvis solvis) {
			Calc calc = new Calc(0);
			return calc.cnt * calc.touch.getSettingTime(solvis);
		}

		public int getCurrent(final Solvis solvis) throws IOException, TerminationException, NumberFormatException {
			MyImage image = SolvisScreen.getImage(solvis.getCurrentScreen());

			Ocr ocr = new Ocr(image, this.rectangle);
			char ch = ocr.toChar();

			int value = Character.digit(ch, 10);
			if (value < 0) {
				throw new NumberFormatException("Character <" + ch + "> is not a valid digit");
			}

			return value;

		}

		public boolean isCode(final Solvis solvis) throws IOException, TerminationException, NumberFormatException {
			return this.digit == this.getCurrent(solvis);

		}

		public static class Creator extends CreatorByXML<Digit> {

			private int digit;
			private Rectangle rectangle;
			private TouchPoint upper;
			private TouchPoint lower;

			public Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) {
				switch (name.getLocalPart()) {
					case "digit":
						this.digit = Integer.parseInt(value);
						break;
				}
			}

			@Override
			public Digit create() throws XmlException, IOException {
				return new Digit(this.digit, this.rectangle, this.upper, this.lower);
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
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
			public void created(final CreatorByXML<?> creator, final Object created) throws XmlException {
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

		public void assign(final SolvisDescription description) throws AssignmentException {
			if (this.upper != null) {
				this.upper.assign(description);
			}
			if (this.lower != null) {
				this.lower.assign(description);
			}
		}

	}

	@Override
	public void assign(final SolvisDescription description) throws AssignmentException, ReferenceException {
		if (this.waitTimeAfterLastDigitRefId != null) {
			Duration duration = description.getDuration(this.waitTimeAfterLastDigitRefId);
			if (duration == null) {
				throw new ReferenceException("Reference <" + this.waitTimeAfterLastDigitRefId + "> unknown.");
			}
		}
		for (Digit digit : this.digits) {
			digit.assign(description);
		}
	}

	@Override
	public int getSettingTime(final Solvis solvis) {
		Duration duration = solvis.getDuration(this.waitTimeAfterLastDigitRefId);
		int settingTime = duration.getTime_ms();
		for (Digit digit : this.digits) {
			settingTime += digit.getSettingTime(solvis);
		}
		return settingTime;
	}
}
