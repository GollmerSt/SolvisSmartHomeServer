/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.Collection;

import de.sgollmer.solvismax.connection.transfer.SolvisStatePackage;
import de.sgollmer.solvismax.error.ObserverException;
import de.sgollmer.solvismax.error.SolvisErrorException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.ErrorState;
import de.sgollmer.solvismax.model.objects.ErrorState.Info;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;

public class SolvisState extends Observable<SolvisStatePackage> {

	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisState.class);

	private final Solvis solvis;

	private SolvisStatus state = SolvisStatus.UNDEFINED;
	private final ErrorState errorState;

	private long timeOfLastSwitchingOn = -1;
	private boolean solvisClockValid = false;
	private boolean solvisDataValid = false;

	SolvisState(final Solvis solvis) {
		this.solvis = solvis;
		this.errorState = new ErrorState(solvis);

		this.errorState.register(new IObserver<ErrorState.Info>() {

			@Override
			public void update(Info info, Object source) {
				if (info != null) {
					SolvisState.this.notify(new SolvisStatePackage(getState(), solvis));
				}
				Collection<ObserverException> exceptions = solvis.notifySolvisErrorObserver(info, this);
				if (exceptions != null) {
					throw new ObserverException(exceptions);
				}
			}
		});
	}

	public enum ErrorChanged {
		SET, NONE, RESET
	}

	public void setPowerOff() {
		this.setState(SolvisStatus.POWER_OFF);
	}

	public void setDisconnected() {
		this.setState(SolvisStatus.SOLVIS_DISCONNECTED);
	}

	public synchronized SolvisStatus getState() {
		if (this.errorState.isError()) {
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

		public SolvisErrorInfo(Solvis solvis, MyImage image, String message, boolean cleared) {
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
		return this.errorState.isMessageError();
	}

	public boolean isMessageErrorVisible() {
		return this.errorState.isMessageErrorVisible();
	}

	boolean isConnected() {
		return this.state == SolvisStatus.SOLVIS_CONNECTED;
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

	/**
	 * handles the error detection
	 * 
	 * @param visible Screen of the SolvisControl
	 * @return true if the realScreen can be interpreted further (no MessageBox or
	 *         not modified)
	 * @throws IOException
	 */

	public boolean handleError(SolvisScreen realScreen) throws IOException {
		return this.errorState.handleError(realScreen);
	}

	public void handleChannelError(final boolean error, final ChannelDescription description) {
		this.errorState.handleChannelError(error, description);
	}

	public void back() throws IOException, TerminationException, SolvisErrorException {
		this.errorState.back();
	}

}
