package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.BaseData;
import de.sgollmer.solvismax.Constants.Debug;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.WatchDog.Event;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.model.objects.unit.Unit;

public class HumanAccess extends Observer.Observable<HumanAccess.Status> {

	private static final ILogger logger = LogManager.getInstance().getLogger(HumanAccess.class);

	public enum Status {
		USER(false, "User", SolvisStatus.USER_ACCESS_DETECTED),
		SERVICE(false, "Service", SolvisStatus.SERVICE_ACCESS_DETECTED),
		NONE(true, "None", SolvisStatus.HUMAN_ACCESS_FINISHED),
		UNKNOWN(true, "None", SolvisStatus.HUMAN_ACCESS_FINISHED);

		private final boolean serverAccessEnabled;
		private final String accessType;
		private final SolvisStatus status;

		private Status(boolean serverAccessEnabled, String accessType, SolvisStatus connectionStatus) {
			this.serverAccessEnabled = serverAccessEnabled;
			this.accessType = accessType;
			this.status = connectionStatus;
		}

		public boolean isServerAccessEnabled() {
			return this.serverAccessEnabled;
		}

		public String getAccessType() {
			return this.accessType;
		}

		public SolvisStatus getStatus() {
			return this.status;
		}

	}

	private final Solvis solvis;

	private final boolean endOfUserByScreenSaver;
	private final int releaseBlockingAfterUserChange_ms;
	private final int releaseBlockingAfterServiceAccess_ms;
	private final boolean detectServiceAccess;

	private long lastAccess = System.currentTimeMillis();
	private long lastUserAccessTime = 0;
	private boolean serviceScreenDetected = false;
	private long serviceAccessFinishedTime = 0;
	private boolean powerOff = false;

	private Status status = Status.UNKNOWN;

	public HumanAccess(final Solvis solvis) {
		this.solvis = solvis;
		Unit unit = solvis.getUnit();
		this.endOfUserByScreenSaver = unit.getFeatures().isEndOfUserByScreenSaver();
		this.releaseBlockingAfterUserChange_ms = BaseData.DEBUG ? Debug.USER_ACCESS_TIME
				: unit.getReleaseBlockingAfterUserAccess_ms();
		this.releaseBlockingAfterServiceAccess_ms = unit.getReleaseBlockingAfterServiceAccess_ms();
		this.detectServiceAccess = solvis.getFeatures().isDetectServiceAccess();

	}

	public boolean isServerAccessEnabled() {
		return this.status.isServerAccessEnabled();
	}

	public Status getStatus() {
		return this.status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public void processEvent(final Event event, final SolvisScreen realScreen)
			throws IOException, TerminationException {
		HumanAccess.Status current = this.status;
		long currentTime = System.currentTimeMillis();
		switch (event) {
			case POWER_OFF:
				this.powerOff = true;
				current = HumanAccess.Status.SERVICE;
				this.solvis.setScreenSaverActive(false);
				break;

			case NONE:
			case SCREENSAVER:
				switch (current) {
					case USER:
						synchronized (this) {
							if (event == Event.SCREENSAVER) {
								current = HumanAccess.Status.NONE;
							} else if (!this.endOfUserByScreenSaver
									&& currentTime > this.lastUserAccessTime + this.releaseBlockingAfterUserChange_ms) {
								current = HumanAccess.Status.NONE;
							}
						}
						break;
					case SERVICE:
						synchronized (this) {
							if ((!this.endOfUserByScreenSaver || event == Event.SCREENSAVER)
									&& !this.serviceScreenDetected && !this.powerOff
									&& currentTime > this.serviceAccessFinishedTime
											+ this.releaseBlockingAfterServiceAccess_ms) {
								current = HumanAccess.Status.NONE;
							}
						}
						break;
				}
				if (event == Event.SCREENSAVER) {
					this.solvis.setScreenSaverActive(true);
				}
				break;

			case POWER_ON:
				this.powerOff = false;
				if (current == HumanAccess.Status.SERVICE) {
					synchronized (this) {
						this.serviceAccessFinishedTime = currentTime;
						this.serviceScreenDetected = false;
					}
				}
				this.solvis.setScreenSaverActive(false);
				break;

			case HUMAN_ACCESS_SERVICE:
				if (this.powerOff) {
					current = HumanAccess.Status.SERVICE;
				} else {
					synchronized (this) {
						this.serviceScreenDetected = true;
						current = HumanAccess.Status.SERVICE;
					}
				}
				this.solvis.setScreenSaverActive(false);
				break;

			case HUMAN_ACCESS_USER:
				if (this.powerOff) {
					current = HumanAccess.Status.SERVICE;
				} else if (current == HumanAccess.Status.SERVICE) {
					synchronized (this) {
						this.serviceAccessFinishedTime = currentTime;
						this.serviceScreenDetected = false;
					}
				} else {
					synchronized (this) {
						this.lastUserAccessTime = currentTime;
						current = HumanAccess.Status.USER;
					}
				}
				this.solvis.setScreenSaverActive(false);
				break;

			case SET_ERROR_BY_BUTTON:
			case SET_ERROR_BY_MESSAGE:
			case RESET_ERROR:
				this.solvis.setScreenSaverActive(false);
				break;

			case INIT:
				if (realScreen != null && realScreen.isService()) {
					synchronized (this) {
						this.serviceScreenDetected = true;
						this.setStatus(HumanAccess.Status.SERVICE);
					}
				}
				this.solvis.setScreenSaverActive(false);
				break;

			case TRIGGER_SERVICE_BY_COMMAND:
				this.serviceAccessFinishedTime = currentTime;
				current = HumanAccess.Status.SERVICE;
				break;

			case RESET_SERVICE_BY_COMMAND:
				if (!this.serviceScreenDetected && !this.powerOff && !this.isServerAccessEnabled()) {
					this.serviceAccessFinishedTime = 0;
					current = HumanAccess.Status.NONE;
				}
				break;

		}

		processHumanAccess(current, currentTime);
	}

	private void processHumanAccess(final HumanAccess.Status access, final long time)
			throws IOException, TerminationException {

		HumanAccess.Status last = this.getStatus();

		if (last != HumanAccess.Status.UNKNOWN) {
			if (last == access) {
				return;
			}
			this.notify(access);
		}

		if (last != access && access == HumanAccess.Status.NONE) {
			this.lastAccess = time;
		}
		
		HumanAccess.Status current = access;


		switch (current) {
			case SERVICE:
			case USER:
				logger.info(current.getAccessType() + " access detected.");
				break;
			case NONE:
				logger.info(last.getAccessType() + " access finished.");
				this.solvis.saveScreen();
				break;
			case UNKNOWN:
				current = HumanAccess.Status.NONE;
				this.solvis.saveScreen();
				break;
		}
		this.setStatus(current);
	}

	synchronized void serviceAccess(Event event) throws IOException, TerminationException {
		switch (event) {
			case TRIGGER_SERVICE_BY_COMMAND:
			case RESET_SERVICE_BY_COMMAND:
				this.processEvent(event, null);
				break;
			default:
				logger.error("Unexpected call of \"serviceAccess\". Ignored");
		}
	}

	public long getLastAccess() {
		return this.lastAccess;
	}

	public Event getEvent(SolvisScreen realScreen) throws IOException, TerminationException {
		
		SolvisScreen current = this.solvis.getCurrentScreen(false);
				
		boolean changed =  SolvisScreen.get(realScreen) !=(SolvisScreen.get(current));
		changed |= !realScreen.equalsWoIgnore(current);

		if (!changed) {
			return Event.NONE;
		} else if (realScreen.isService() && this.detectServiceAccess) {
			return Event.HUMAN_ACCESS_SERVICE;
		} else {
			return Event.HUMAN_ACCESS_USER;
		}
	}

}
