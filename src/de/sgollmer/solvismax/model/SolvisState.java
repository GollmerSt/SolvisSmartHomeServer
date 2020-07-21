/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.mail.MessagingException;

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

public class SolvisState extends Observable<SolvisState> implements ISendData {

	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisState.class);

	private final Solvis solvis;
	private State state = State.UNDEFINED;
	private SolvisScreen errorScreen = null;
	private Set<ChannelDescription> errorChannels = new HashSet<>();
	private boolean error = false;
	private long timeOfLastSwitchingOn = -1;

	SolvisState(Solvis solvis) {
		this.solvis = solvis;
	}

	public enum State {
		POWER_OFF, REMOTE_CONNECTED, SOLVIS_CONNECTED, SOLVIS_DISCONNECTED, ERROR, UNDEFINED
	}

	private enum ErrorChanged {
		SET, NONE, RESET
	}

	/**
	 * Set/reset error, if error channel detects an error
	 * 
	 * @param error
	 * @param description
	 */

	public synchronized void setError(boolean error, ChannelDescription description) {
		ErrorChanged changed;
		if (error) {
			changed = this.errorChannels.add(description) ? ErrorChanged.SET : ErrorChanged.NONE;
		} else {
			changed = this.errorChannels.remove(description) ? ErrorChanged.RESET : ErrorChanged.NONE;
		}
		processError(changed, null);
	}

	/**
	 * Set error if an error message/button is visible on screen
	 * 
	 * @param errorVisible if error button or error message box is visible
	 * @param errorScreen
	 * @return
	 */

	synchronized boolean setError(Event errorEvent, SolvisScreen errorScreen) {
		boolean errorVisible = errorEvent.isError();
		ErrorChanged changed = ErrorChanged.NONE;
		boolean isHomeScreen = SolvisScreen.get(errorScreen) == this.solvis.getHomeScreen();
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
		return processError(changed, null);
	}

	private boolean processError(ErrorChanged errorChangeState, ChannelDescription description) {
		String errorName = description == null ? "Message box" : description.getId();
		boolean last = this.error;
		this.error = this.errorScreen != null || !this.errorChannels.isEmpty();
		MailInfo mailInfo = null;
		if (errorChangeState != ErrorChanged.NONE) {
			String message = "The Solvis system \"" + this.solvis.getUnit().getId() + "\" reports: ";
			if (errorChangeState == ErrorChanged.SET) {
				message += " Error: " + errorName + " occured.";
			} else {
				message += errorName + " cleared.";
			}
			logger.info(message);
			mailInfo = new MailInfo(SolvisScreen.getImage(this.errorScreen), message);
		}
		if (!this.error && last) {
			String message = "All errors of Solvis system \"" + this.solvis.getUnit().getId() + "\" cleared.";
			logger.info(message);
			mailInfo = new MailInfo(SolvisScreen.getImage(this.errorScreen), message);
		}
		if (mailInfo != null) {
			this.notify(this);
			if (this.solvis.getExceptionMail() != null && this.solvis.getFeatures().isSendMailOnError()) {
				try {
					this.solvis.getExceptionMail().send(mailInfo);
				} catch (MessagingException | IOException e) {
					return false;
				}
			}
		}
		return true;
	}

	public void powerOff() {
		this.setState(State.POWER_OFF);
	}

	public void connected() {
		this.setState(State.SOLVIS_CONNECTED);
	}

	public void disconnected() {
		this.setState(State.SOLVIS_DISCONNECTED);
	}

	void remoteConnected() {
		this.setState(State.REMOTE_CONNECTED);
	}

	public State getState() {
		if (this.error) {
			return State.ERROR;
		} else if (this.state == State.UNDEFINED) {
			return State.POWER_OFF;
		} else {
			return this.state;
		}
	}

	private void setState(State state) {
		if (this.state != state) {
			if (state == State.SOLVIS_CONNECTED) {
				switch (this.state) {
					case POWER_OFF:
					case REMOTE_CONNECTED:
						this.timeOfLastSwitchingOn = System.currentTimeMillis();
				}
			}
			this.state = state;
			logger.info("Solvis state changed to <" + this.state.name() + ">.");
			this.notify(this);
		}
	}

	@Override
	public SolvisStatePackage createJsonPackage() {
		return new SolvisStatePackage(this.getState());
	}

	long getTimeOfLastSwitchingOn() {
		return this.timeOfLastSwitchingOn;
	}

	public static class MailInfo {
		private final MyImage image;
		private final String message;

		private MailInfo(MyImage image, String message) {
			this.image = image;
			this.message = message;
		}

		public MyImage getImage() {
			return this.image;
		}

		public String getMessage() {
			return this.message;
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

	public MqttData getMqttData() {
		return new MqttData(this.solvis, Constants.Mqtt.STATUS, this.getState().name(), 0, true);
	}

}
