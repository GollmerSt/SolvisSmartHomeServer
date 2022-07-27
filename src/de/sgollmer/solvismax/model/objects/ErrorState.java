package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.sgollmer.solvismax.error.FatalError;
import de.sgollmer.solvismax.error.ObserverException;
import de.sgollmer.solvismax.error.SolvisErrorException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.Solvis.SynchronizedScreenResult;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.screen.ErrorDetection;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;

public class ErrorState extends Observable<ErrorState.Info> {

	private static final ILogger logger = LogManager.getInstance().getLogger(ErrorState.class);

	private final Solvis solvis;

	private final boolean clearErrorMessageAfterMail;
	private final int resetErrorDelayTime;

	private int errorState = 0;
	private boolean messageVisible = false;

	// private int errorState = 0;
	private SolvisScreen errorScreen = null;
	private final Set<ChannelDescription> errorChannels = new HashSet<>();
	private Long resetErrorTime = null;

	public ErrorState(final Solvis solvis) {
		this.solvis = solvis;

		this.clearErrorMessageAfterMail = solvis.getUnit().getFeatures().isClearErrorMessageAfterMail();
		this.resetErrorDelayTime = solvis.getUnit().getResetErrorDelayTime();
	}

	public enum ErrorType {
		/*
		 * Bit0: Channel error Bit1: Message error detected
		 */
		CHANNEL(1), //
		MESSAGE_VISIBLE(2, true), //
		MESSAGE_UNVISBLE(2, true), //
		MESSAGE(2, true), //
		STD_SCREEN(0), //
		HOME_NONE(2, false); // Home without error button

		private final int mask;
		private final Boolean fixValue;

		private ErrorType(final int mask, Boolean fixValue) {
			this.mask = mask;
			this.fixValue = fixValue;
		}

		private ErrorType(final int mask) {
			this(mask, null);
		}

		public int setError(final int mask, final boolean error) {
			boolean set = this.fixValue == null ? error : this.fixValue;
			if (set) {
				return mask | this.mask;
			} else {
				return mask & ~this.mask;
			}
		}

		public boolean isError(final int mask) {
			return (mask & this.mask) != 0;
		}

	}

	/**
	 * handles the error detection
	 * 
	 * @param visible Screen of the SolvisControl
	 * @return true if the realScreen can be interpreted further (no MessageBox or
	 *         not modified)
	 * @throws IOException
	 */

	public boolean handleError(final SolvisScreen realScreen) throws IOException {

		ErrorDetection.Type type = this.solvis.getSolvisDescription().getErrorDetection().getType(realScreen);

		ErrorType errorType = null;

		synchronized (this) {

			switch (type) {

				case MESSAGE_BOX:
					this.resetErrorTime = null;
					errorType = ErrorType.MESSAGE_VISIBLE;
					break;

				case ERROR_BUTTON:
					this.resetErrorTime = null;
					errorType = ErrorType.MESSAGE_UNVISBLE;
					break;

				case HOME_NONE:
					if (ErrorType.MESSAGE.isError(this.errorState) && realScreen.isHomeScreen()) {

						boolean reset = false;

						if (this.resetErrorDelayTime > 0) {

							long time = System.currentTimeMillis();

							if (this.resetErrorTime == null) {

								this.resetErrorTime = time;
								logger.info("Error cleared detected.");

							} else if (time > this.resetErrorTime + this.resetErrorDelayTime) {

								reset = true;
							}
						} else {

							reset = true;
						}
						if (reset) {

							this.resetErrorTime = null;
							errorType = ErrorType.HOME_NONE;

						} else {

							errorType = null;
						}
					}
					break;

				case NONE:
					errorType = ErrorType.STD_SCREEN;
					break;
			}
		}

		boolean canBeInterpretedFurther = !this.messageVisible;

		if (errorType != null) {

			Collection<ObserverException> exceptions = this.setStatus(errorType, realScreen, null);

			boolean successfull = exceptions == null || exceptions.isEmpty();

			boolean back;

			synchronized (this) {
				back = successfull && this.messageVisible && this.clearErrorMessageAfterMail
						&& this.solvis.isControlEnabled();
			}

			canBeInterpretedFurther = !this.messageVisible;

			if (back) {
				try {

					this.solvis.sendBack();
					this.setStatus(ErrorType.MESSAGE_UNVISBLE, this.solvis.getCurrentScreen(), null);

				} catch (TerminationException e) {
				}
			}
		}

		return canBeInterpretedFurther;
	}

	/**
	 * Set error if an error message/button is visible on screen (from watch dog)
	 * 
	 * @param errorVisible if error button or error message box is visible
	 * @param realScreen
	 * @return true if setting of the error states are successful.
	 */

	/**
	 * Set/reset error, if error channel detects an error (caused by measurements
	 * worker)
	 * 
	 * @param error
	 * @param description
	 */

	public void handleChannelError(final boolean error, final ChannelDescription description) {
		boolean former = this.errorChannels.isEmpty();
		synchronized (this) {
			if (error) {
				this.errorChannels.add(description);
			} else {
				this.errorChannels.remove(description);
			}
		}
		if (former != this.errorChannels.isEmpty()) {
			setStatus(ErrorType.CHANNEL, null, description);
		}
	}

	/**
	 * 
	 * @param errorChangeState NONE: error not changed, SET: Error was set, RESET:
	 *                         Error could be reseted
	 * @param description      null, if error was displayed by the screen, otherwise
	 *                         the channel description
	 * @return
	 */
	private Collection<ObserverException> setStatus(final ErrorType errorType, final SolvisScreen realScreen,
			final ChannelDescription description) {

		int last;
		boolean changed;
		boolean messageVisibleChanged;
		boolean error;
		MyImage errorImage = null;

		synchronized (this) {

			last = this.errorState;
			boolean lastMessageVisible = this.messageVisible;
			this.errorState = errorType.setError(this.errorState, !this.errorChannels.isEmpty());
			changed = last != this.errorState;
			error = errorType.isError(this.errorState);

			switch (errorType) {
				case CHANNEL:
					break;

				case MESSAGE_VISIBLE:
					this.messageVisible = true;
					changed |= realScreen.equals(this.errorScreen);
					this.errorScreen = realScreen;
					errorImage = SolvisScreen.getImage(realScreen);
					break;

				case MESSAGE_UNVISBLE:
					this.messageVisible = false;
					if (this.errorScreen == null) {
						this.errorScreen = realScreen;
						errorImage = SolvisScreen.getImage(realScreen);
					}
					break;

				case MESSAGE:
					throw new FatalError("MESSAGE can't be a errorType for set");

				case HOME_NONE:
					this.messageVisible = false;
					this.errorScreen = null;
					break;

				case STD_SCREEN:
					this.messageVisible = false;
					break;
			}

			messageVisibleChanged = this.messageVisible != lastMessageVisible;

		}

		String errorName = description == null ? "Message box" : description.getId();

		Info solvisErrorInfo = null;

		if (changed) {
			String message = "The Solvis system \"" + this.solvis.getUnit().getId() + "\" reports: ";
			if (error) {
				message += " Error: " + errorName + " occured.";
			} else {
				message += errorName + " cleared.";
			}
			logger.info(message);
			solvisErrorInfo = new Info(this.solvis, errorImage, message, !error);
		}

		if (this.errorState == 0 && last != 0) {
			String message = "All errors of Solvis system \"" + this.solvis.getUnit().getId() + "\" cleared.";
			logger.info(message);
			solvisErrorInfo = new Info(this.solvis, SolvisScreen.getImage(this.errorScreen), message, true);
		}

		if (changed || messageVisibleChanged) {
			return this.notify(solvisErrorInfo, this);
		} else {
			return null;
		}
	}

	public static class Info {
		private final Solvis solvis;
		private final MyImage image;
		private final String message;
		private final boolean cleared;

		public Info(Solvis solvis, MyImage image, String message, boolean cleared) {
			this.solvis = solvis;
			this.image = image;
			this.message = message;
			this.cleared = cleared;
		}

		public Solvis getSolvis() {
			return this.solvis;
		}

		public MyImage getImage() {
			return this.image;
		}

		public String getMessage() {
			return this.message;
		}

		public boolean isCleared() {
			return this.cleared;
		}
	}

	public boolean isMessageError() {
		return ErrorType.MESSAGE.isError(this.errorState);
	}

	public boolean isMessageErrorVisible() {
		return this.messageVisible;
	}

	public boolean isError() {
		return this.errorState != 0;
	}

	public void back() throws IOException, TerminationException, SolvisErrorException {

		SynchronizedScreenResult synchronizedScreenResult = this.solvis.getSyncronizedRealScreen();
		SolvisScreen realScreen = SynchronizedScreenResult.getScreen(synchronizedScreenResult);
		if (realScreen != null) {
			this.handleError(realScreen);
		}
		if (this.isGuiEnabled()) {
			this.solvis.sendBack();
		} else {
			throw new SolvisErrorException();
		}

	}

	public boolean isGuiEnabled() {
		return !this.messageVisible;
	}

}
