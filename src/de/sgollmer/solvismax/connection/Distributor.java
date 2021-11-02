/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import java.util.Calendar;
import java.util.Collection;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.transfer.ConnectionState;
import de.sgollmer.solvismax.connection.transfer.MeasurementsPackage;
import de.sgollmer.solvismax.connection.transfer.SolvisStatePackage;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.SolvisStatus;
import de.sgollmer.solvismax.model.WatchDog.HumanAccess;
import de.sgollmer.solvismax.model.objects.Measurements;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.data.SolvisData.SmartHomeData;
import de.sgollmer.solvismax.model.objects.unit.Unit;

public final class Distributor extends Observable<ISendData> {

	private static final ILogger logger = LogManager.getInstance().getLogger(Distributor.class);

	private final Solvis solvis;
	private Measurements collectedMeasurements = new Measurements();
	private Measurements collectedBufferedMeasurements = new Measurements();
	private final SolvisDataObserver solvisDataObserver = new SolvisDataObserver();
	private final ConnectionStateObserver connectionStateObserver = new ConnectionStateObserver();
	private final SolvisStateObserver solvisStateObserver = new SolvisStateObserver();
	private final HumanAccessObserver humanAccessObserver = new HumanAccessObserver();
	private final AllSettingsDoneObserver allSettingsDoneObserver = new AllSettingsDoneObserver();
	private final AliveThread aliveThread = new AliveThread();
	private final PeriodicBurstThread periodicBurstThread;
	private final int bufferedIntervall_ms;
	private boolean burstUpdate = false;

	public Distributor(Solvis solvis) {
		this.solvis = solvis;
		Unit unit = solvis.getUnit();
		this.bufferedIntervall_ms = unit.getBufferedInterval_ms();
		if (unit.isBuffered()) {
			this.periodicBurstThread = new PeriodicBurstThread();
			this.periodicBurstThread.start();
		} else {
			this.periodicBurstThread = null;
		}
	}

	private class SolvisDataObserver implements Observer.IObserver<SmartHomeData> {

		@Override
		public void update(final SmartHomeData data, final Object source) {

			Collection<SmartHomeData> toSend = null;

			synchronized (Distributor.this) {

				boolean buffered = data.getDescription().isBuffered() && Distributor.this.periodicBurstThread != null;

				if (data.isForce()) {
					buffered = false;
					Distributor.this.collectedBufferedMeasurements.remove(data);
				}

				if (buffered) {
					Distributor.this.collectedBufferedMeasurements.add(data);

				} else {
					Distributor.this.add(data, Distributor.this.collectedMeasurements);
					if (!Distributor.this.burstUpdate) {
						toSend = Distributor.this.collectedMeasurements.cloneAndClear();
					}
				}
			}
			if (toSend != null) {
				Distributor.this.sendCollection(toSend);
			}
		}
	}

	private void add(final SmartHomeData data, final Measurements measurements) {
		SmartHomeData former = measurements.add(data);
		if (former != null) {
			logger.error("measurements not unique. Timestamps of <" + data.getDescription().getId() + ">: Former: "
					+ former.getTransmittedTimeStamp() + ", New: " + data.getTransmittedTimeStamp()
					+ ". Former measurement value is ignored");
		}
	}

	private class SolvisStateObserver implements Observer.IObserver<SolvisStatePackage> {

		@Override
		public void update(final SolvisStatePackage data, final Object source) {
			Distributor.this.notify(data);
		}

	}

	private class ConnectionStateObserver implements Observer.IObserver<ConnectionState> {

		@Override
		public void update(final ConnectionState data, final Object source) {
			Distributor.this.notify(data);

		}

	}

	private class HumanAccessObserver implements Observer.IObserver<HumanAccess> {

		@Override
		public void update(final HumanAccess data, final Object source) {
			try {
				Distributor.this.notify(new SolvisStatePackage(data.getStatus(), Distributor.this.solvis));
			} catch (Throwable e) {
			}
		}

	}

	private class AllSettingsDoneObserver implements Observer.IObserver<SolvisStatus> {
		
		@Override
		public void update(final SolvisStatus status, final Object source) {
			
			try {
				Distributor.this.notify(new SolvisStatePackage(status, Distributor.this.solvis));
			} catch (Throwable e) {
			}
		}

	}

	private void sendCollection(final Collection<SmartHomeData> sendData) {
		long timeStamp = System.currentTimeMillis();
		if (!sendData.isEmpty()) {
			for (SmartHomeData data : sendData) {
				data.setTransmitted(timeStamp);
			}
			this.aliveThread.trigger();
			MeasurementsPackage sendPackage = new MeasurementsPackage(sendData);
			this.notify(sendPackage);
		}
	}

	public void sendCollection(final Measurements measurements) {
		sendCollection(measurements.cloneAndClear());
	}

	private void abort() {
		this.aliveThread.abort();
		if (this.periodicBurstThread != null) {
			this.periodicBurstThread.abort();
		}
	}

	private SolvisDataObserver getSolvisDataObserver() {
		return this.solvisDataObserver;
	}

	private ConnectionStateObserver getConnectionStateObserver() {
		return this.connectionStateObserver;
	}

	private class AliveThread extends Thread {

		private AliveThread() {
			super("Alive thread");
		}

		boolean abort = false;
		boolean triggered = false;

		@Override
		public void run() {

			while (!this.abort) {
				try {
					boolean sendAlive = false;
					synchronized (this) {
						try {
							this.wait(Constants.ALIVE_TIME);
						} catch (InterruptedException e) {
						}
						if (!this.triggered && !this.abort) {
							sendAlive = true;
						} else {
							this.triggered = false;
						}
					}
					if (sendAlive) {
						Distributor.this.notify(new ConnectionState(ConnectionStatus.ALIVE, ""));
					}
				} catch (Throwable e) {
					logger.error("Error was thrown in alive thread. Cause: ", e);
				}
			}
		}

		private synchronized void trigger() {
			this.triggered = true;
			this.notifyAll();
		}

		private synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}

	}

	private class PeriodicBurstThread extends Thread {

		private boolean abort;

		private PeriodicBurstThread() {
			super("PeriodicBurst");
		}

		@Override
		public void run() {
			while (!this.abort) {
				try {
					synchronized (this) {
						Calendar midNight = Calendar.getInstance();
						long now = midNight.getTimeInMillis();
						midNight.set(Calendar.HOUR_OF_DAY, 0);
						midNight.set(Calendar.MINUTE, 0);
						midNight.set(Calendar.SECOND, 0);
						midNight.set(Calendar.MILLISECOND, 200);

						long nextBurst = (now - midNight.getTimeInMillis()) / Distributor.this.bufferedIntervall_ms
								* Distributor.this.bufferedIntervall_ms + midNight.getTimeInMillis()
								+ Distributor.this.bufferedIntervall_ms;

						try {
							this.wait(nextBurst - now);
						} catch (InterruptedException e) {
						}
					}
					if (!this.abort) {
						sendCollection(Distributor.this.collectedBufferedMeasurements);
					}
				} catch (Throwable e) {
					logger.error("Error was thrown in periodic burst thread. Cause: ", e);
					try {
						AbortHelper.getInstance().sleep(Constants.WAIT_TIME_AFTER_THROWABLE);
					} catch (TerminationException e1) {
						return;
					}
				}

			}
		}

		private synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}
	}

	public void setBurstUpdate(final boolean burstUpdate) {
		boolean send;
		Collection<SmartHomeData> collection = null;
		synchronized (this) {
			send = this.burstUpdate;
			this.burstUpdate = burstUpdate;
			if (send) {
				collection = this.collectedMeasurements.cloneAndClear();
			}
		}
		if (send) {
			this.sendCollection(collection);
		}

		String comment = this.burstUpdate ? "started" : "finished";
		if (Constants.Debug.BURST) {
			logger.debug("Burst update " + comment);
		}
	}

	private SolvisStateObserver getSolvisStateObserver() {
		return this.solvisStateObserver;
	}

	public void register() {
		this.solvis.registerAbortObserver(new IObserver<Boolean>() {

			@Override
			public void update(Boolean data, Object source) {
				abort();

			}
		});
		this.solvis.registerSmartHomeObserver(this.getSolvisDataObserver());
		this.solvis.getConnection().register(this.getConnectionStateObserver());
		this.solvis.getSolvisState().register(this.getSolvisStateObserver());
		this.solvis.registerScreenChangedByHumanObserver(this.humanAccessObserver);
		this.solvis.registerAllSettingsDoneObserver(this.allSettingsDoneObserver);
		this.aliveThread.start();
	}

}
