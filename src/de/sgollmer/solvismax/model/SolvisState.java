/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.connection.transfer.SolvisStatePackage;
import de.sgollmer.solvismax.model.objects.Observer.Observable;

public class SolvisState extends Observable<SolvisState> {

	private static final Logger logger = LogManager.getLogger(SolvisState.class);

	private State state = State.POWER_OFF;
	private boolean error = false;

	public enum State {
		POWER_OFF, REMOTE_CONNECTED, SOLVIS_CONNECTED, ERROR
	}

	public void error(boolean error) {
		boolean last = this.error;
		this.error = error;
		if (last != error) {
			this.notify(this);
		}
	}

	public void powerOff() {
		this.setState(State.POWER_OFF);
	}

	public void connected() {
		this.setState(State.SOLVIS_CONNECTED);
	}

	public void remoteConnected() {
		this.setState(State.REMOTE_CONNECTED);
	}

	public State getState() {
		if (error) {
			return State.ERROR;
		} else {
			return state;
		}
	}

	private void setState(State state) {
		if (this.state != state) {
			this.state = state;
			logger.info("Solvis state changed to <" + this.state.name() + ">.");
			this.notify(this);
		}
	}

	public SolvisStatePackage getPackage() {
		return new SolvisStatePackage(this.getState());
	}

}
