/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.connection.transfer.SolvisStatePackage;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.Observer.Observable;

public class SolvisState extends Observable<SolvisState> {

	private static final Logger logger = LogManager.getLogger(SolvisState.class);

	private final Solvis solvis;
	private State state = State.UNDEFINED;
	private int errorCnt = 0;
	private long timeOfLastSwitchingOn = -1;
	private Map<String, Boolean> errorStates = new HashMap<>();
	private MyImage errorScreen = null;
	private String lastMessage = null;

	public SolvisState(Solvis solvis) {
		this.solvis = solvis;
	}

	public enum State {
		POWER_OFF, REMOTE_CONNECTED, SOLVIS_CONNECTED, SOLVIS_DISCONNECTED, ERROR, UNDEFINED
	}

	public synchronized void error(boolean error, String errorName, MyImage image) {
		Boolean last = this.errorStates.get(errorName);
		if (last == null) {
			last = false;
		}
		if (last != error) {
			this.errorScreen = image;
			this.errorStates.put(errorName, error);
			this.errorCnt += error ? 1 : -1;
			if (error) {
				this.lastMessage = "The Solvis system \"" + this.solvis.getUnit().getId()
						+ "\" reports the following error: " + errorName;
			}
			this.notify(this);
			logger.info(error ? this.lastMessage : "Solvis error  <" + errorName + "> cleared.");
			logger.info(this.errorCnt > 0 ? "Solvis error!" : "Solvis error cleared.");
		}
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

	public void remoteConnected() {
		this.setState(State.REMOTE_CONNECTED);
	}

	public State getState() {
		if (this.errorCnt > 0) {
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

	public SolvisStatePackage getPackage() {
		return new SolvisStatePackage(this.getState());
	}

	public long getTimeOfLastSwitchingOn() {
		return this.timeOfLastSwitchingOn;
	}

	public synchronized MyImage getErrorScreen() {
		return this.errorScreen;
	}

	public synchronized String getLastMessage() {
		return this.lastMessage;
	}

}
