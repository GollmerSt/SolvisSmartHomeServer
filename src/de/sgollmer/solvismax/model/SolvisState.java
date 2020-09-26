/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.util.HashSet;
import java.util.Set;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.ISendData;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.connection.transfer.SolvisStatePackage;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.WatchDog.Event;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;

public class SolvisState extends Observable<de.sgollmer.solvismax.model.SolvisState.State> {

	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisState.class);

	private final Solvis solvis;
	private State state = State.UNDEFINED;
	private SolvisScreen errorScreen = null;
	private final Set<ChannelDescription> errorChannels = new HashSet<>();
	private boolean error = false;
	private long timeOfLastSwitchingOn = -1;

	SolvisState(Solvis solvis) {
		this.solvis = solvis;
	}

	public enum State implements ISendData {
		POWER_OFF, REMOTE_CONNECTED, SOLVIS_CONNECTED, SOLVIS_DISCONNECTED, ERROR, UNDEFINED;

		@Override
		public SolvisStatePackage createJsonPackage() {
			return new SolvisStatePackage(this);
		}

		public MqttData getMqttData(SolvisState state) {
			return new MqttData(state.solvis, Constants.Mqtt.STATUS, this.name(), 0, true);
		}
	}

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

	public void setError(boolean error, ChannelDescription description) {
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
	 * @return
	 */

	boolean setError(Event errorEvent, SolvisScreen errorScreen) {
		boolean errorVisible = errorEvent.isError();
		ErrorChanged changed = ErrorChanged.NONE;
		boolean isHomeScreen = SolvisScreen.get(errorScreen) == this.solvis.getHomeScreen();

		synchronized (this) {

			if (errorVisible || this.errorScreen != null) {
				if (!errorVisible && isHomeScreen) { // Nur der Homescreen kann Fehler zuücksetzen
					this.errorScreen = null;
					changed = ErrorChanged.RESET;
				} else if (errorEvent == Event.SET_ERROR_BY_MESSAGE || this.errorScreen == null) {
					this.errorScreen = errorScreen;
					changed = ErrorChanged.SET;
				} else {
					changed = ErrorChanged.NONE;
				}
			}
		}

		return processError(changed, null);
	}

	private boolean processError(ErrorChanged errorChangeState, ChannelDescription description) {
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
			this.notify(this.state, this);
			return this.solvis.notifySolvisErrorObserver(solvisErrorInfo, this);
		}
		return true;
	}

	public void setPowerOff() {
		this.setState(State.POWER_OFF);
	}

	public void setConnected() {
		this.setState(State.SOLVIS_CONNECTED);
	}

	public void setDisconnected() {
		this.setState(State.SOLVIS_DISCONNECTED);
	}

	void setRemoteConnected() {
		this.setState(State.REMOTE_CONNECTED);
	}

	public synchronized State getState() {
		if (this.error) {
			return State.ERROR;
		} else if (this.state == State.UNDEFINED) {
			return State.POWER_OFF;
		} else {
			return this.state;
		}
	}

	private void setState(State state) {
		boolean changed = false;
		synchronized (this) {

			if (this.state != state) {
				if (state == State.SOLVIS_CONNECTED) {
					switch (this.state) {
						case POWER_OFF:
						case REMOTE_CONNECTED:
							this.timeOfLastSwitchingOn = System.currentTimeMillis();
					}
				}
				this.state = state;
				changed = true;
			}
		}
		if (changed) {
			logger.info("Solvis state changed to <" + state.name() + ">.");
			this.notify(this.state, this);
		}
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
		return this.state == State.SOLVIS_CONNECTED;
	}

	boolean isErrorMessage() {
		return this.errorScreen != null;
	}

}
