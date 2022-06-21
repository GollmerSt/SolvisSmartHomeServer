/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.sgollmer.solvismax.connection.transfer.SolvisStatePackage;
import de.sgollmer.solvismax.error.ObserverException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.WatchDog.Event;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;

public class SolvisState extends Observable<SolvisStatePackage> {

	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisState.class);

	private final Solvis solvis;
	private SolvisStatus state = SolvisStatus.UNDEFINED;
	private SolvisScreen errorScreen = null;
	private final Set<ChannelDescription> errorChannels = new HashSet<>();
	private boolean error = false;
	private long timeOfLastSwitchingOn = -1;
	private boolean solvisClockValid = false;
	private boolean solvisDataValid = false;
	private Long resetErrorTime = null;
	private final int resetErrorDelayTime;

	SolvisState(final Solvis solvis) {
		this.solvis = solvis;
		this.resetErrorDelayTime = solvis.getUnit().getResetErrorDelayTime();	}

	private enum ErrorChanged {
		SET, NONE, RESET
	}

	/**
	 * Set/reset error, if error channel detects an error (caused by measurements
	 * worker)
	 * 
	 * @param error
	 * @param description
	 */

	public void setError(final boolean error, final ChannelDescription description) {
		ErrorChanged changed;
		synchronized (this) {
			if (error) {
				changed = this.errorChannels.add(description) ? ErrorChanged.SET : ErrorChanged.NONE;
			} else {
				changed = this.errorChannels.remove(description) ? ErrorChanged.RESET : ErrorChanged.NONE;
			}
		}
		processError(changed, null);
	}

	/**
	 * Set error if an error message/button is visible on screen (from watch dog)
	 * 
	 * @param errorVisible if error button or error message box is visible
	 * @param errorScreen
	 * @return true if setting of the error states are successful.
	 */

	boolean setError(final Event errorEvent, final SolvisScreen errorScreen) {
		boolean errorVisible = errorEvent.isError();
		ErrorChanged changed = ErrorChanged.NONE;
		boolean isHomeScreen = SolvisScreen.get(errorScreen) == this.solvis.getHomeScreen();

		synchronized (this) {

			if (errorVisible || this.errorScreen != null) {
				if (!errorVisible && isHomeScreen) { // Nur der Homescreen kann Fehler zuücksetzen
					long time = System.currentTimeMillis();
					boolean resetErrorDelayed = this.resetErrorDelayTime > 0;
					if (this.resetErrorTime == null && resetErrorDelayed) {
						this.resetErrorTime = time;
						logger.debug("Error cleared detected.");
					} else if (time > this.resetErrorTime + this.resetErrorDelayTime | !resetErrorDelayed) {
						this.errorScreen = null;
						changed = ErrorChanged.RESET;
					}
				} else if (errorEvent == Event.SET_ERROR_BY_MESSAGE || this.errorScreen == null) {
					this.resetErrorTime = null;
					this.errorScreen = errorScreen;
					changed = ErrorChanged.SET;
				} else {
					changed = ErrorChanged.NONE;
				}
			}
		}

		return processError(changed, null) == null;
	}

	/**
	 * 
	 * @param errorChangeState NONE: error not changed, SET: Error was set, RESET: Error could be reseted
	 * @param description  null, if error was displayed by the screen, otherwise the channel description 
	 * @return
	 */
	private Collection<ObserverException> processError(final ErrorChanged errorChangeState,
			final ChannelDescription description) {
		String errorName = description == null ? "Message box" : description.getId();

		boolean channelError;

		SolvisErrorInfo solvisErrorInfo = null;

		boolean last;
		boolean error;
		MyImage errorImage;

		synchronized (this) {
			channelError = !this.errorChannels.isEmpty();
			last = this.error;
			this.error = this.errorScreen != null || channelError;
			error = this.error;
			errorImage = SolvisScreen.getImage(this.errorScreen);
		}

		if (errorChangeState != ErrorChanged.NONE) {
			String message = "The Solvis system \"" + this.solvis.getUnit().getId() + "\" reports: ";
			boolean cleared = errorChangeState != ErrorChanged.SET;
			if (cleared) {
				message += errorName + " cleared.";
			} else {
				message += " Error: " + errorName + " occured.";
			}
			logger.info(message);
			solvisErrorInfo = new SolvisErrorInfo(this.solvis, errorImage, message, cleared);
		}
		if (!error && last) {
			String message = "All errors of Solvis system \"" + this.solvis.getUnit().getId() + "\" cleared.";
			logger.info(message);
			solvisErrorInfo = new SolvisErrorInfo(this.solvis, SolvisScreen.getImage(this.errorScreen), message, true);
		}
		if (solvisErrorInfo != null) {
			this.notify(new SolvisStatePackage(this.state, this.solvis));
			return this.solvis.notifySolvisErrorObserver(solvisErrorInfo, this);
		}
		return null;
	}

	public void setPowerOff() {
		this.setState(SolvisStatus.POWER_OFF);
	}

	public void setDisconnected() {
		this.setState(SolvisStatus.SOLVIS_DISCONNECTED);
	}

	public synchronized SolvisStatus getState() {
		if (this.error) {
			return SolvisStatus.ERROR;
		} else if (this.state == SolvisStatus.UNDEFINED) {
			return SolvisStatus.POWER_OFF;
		} else {
			return this.state;
		}
	}

	private void setState(final SolvisStatus state) {

		SolvisStatePackage solvisStatePackage = this.setStateWONotify(state);
		this.notify(solvisStatePackage);
	}

	private synchronized SolvisStatePackage setStateWONotify(final SolvisStatus state) {
		if (state == null) {
			return null;
		}
		boolean changed = false;
		synchronized (this) {

			if (this.state != state) {
				if (state == SolvisStatus.SOLVIS_CONNECTED) {
					switch (this.state) {
						case POWER_OFF:
						case REMOTE_CONNECTED:
							this.timeOfLastSwitchingOn = System.currentTimeMillis();
					}
				}
				this.state = state;
				changed = true;
			}
			if (changed) {
				logger.info("Solvis state changed to <" + state.name() + ">.");
				return (this.getSolvisStatePackage());
			} else {
				return null;
			}
		}

	}

	@Override
	public void notify(final SolvisStatePackage solvisStatePackage) {
		if (solvisStatePackage != null) {
			super.notify(solvisStatePackage);
		}
	}

	@Override
	public Collection<ObserverException> notify(final SolvisStatePackage solvisStatePackage, final Object source) {
		if (solvisStatePackage != null) {
			return super.notify(solvisStatePackage, source);
		}
		return null;
	}

	public SolvisStatePackage getSolvisStatePackage() {
		return new SolvisStatePackage(this.state, this.solvis);
	}

	long getTimeOfLastSwitchingOn() {
		return this.timeOfLastSwitchingOn;
	}

	public static class SolvisErrorInfo {
		private final Solvis solvis;
		private final MyImage image;
		private final String message;
		private final boolean cleared;

		private SolvisErrorInfo(Solvis solvis, MyImage image, String message, boolean cleared) {
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

	boolean isError() {
		return this.error;
	}

	boolean isConnected() {
		return this.state == SolvisStatus.SOLVIS_CONNECTED;
	}

	boolean isErrorMessage() {
		return this.errorScreen != null;
	}

	public void setSolvisClockValid(final boolean valid) {
		SolvisStatePackage solvisStatePackage = null;
		synchronized (this) {
			this.solvisClockValid = valid;
			SolvisStatus status = getSolvisConnectionState();
			solvisStatePackage = this.setStateWONotify(status);
		}
		this.notify(solvisStatePackage);
	}

	public void setSolvisDataValid(final boolean valid) {
		SolvisStatePackage solvisStatePackage = null;
		synchronized (this) {
			this.solvisDataValid = valid;
			SolvisStatus status = getSolvisConnectionState();
			solvisStatePackage = this.setStateWONotify(status);
		}
		this.notify(solvisStatePackage);
	}

	private synchronized SolvisStatus getSolvisConnectionState() {
		SolvisStatus state = this.state;
		if (this.solvisClockValid && this.solvisDataValid) {
			state = SolvisStatus.SOLVIS_CONNECTED;
		} else if (this.state != SolvisStatus.UNDEFINED || !this.solvisClockValid && !this.solvisDataValid) {
			state = SolvisStatus.REMOTE_CONNECTED;
		}
		return state;
	}

	public void connectionSuccessfull(final String urlBase) {

		switch (this.state) {
			case POWER_OFF:
			case SOLVIS_DISCONNECTED:
				if (!this.solvisDataValid && !this.solvisClockValid) {
					logger.info("Connection to solvis remote <" + urlBase + "> successfull.");
				}
				break;
		}

	}

}
