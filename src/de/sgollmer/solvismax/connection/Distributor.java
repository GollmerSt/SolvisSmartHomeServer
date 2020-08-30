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
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.connection.transfer.MeasurementsPackage;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.SolvisState;
import de.sgollmer.solvismax.model.WatchDog.HumanAccess;
import de.sgollmer.solvismax.model.objects.Measurements;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.Units.Unit;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class Distributor extends Observable<JsonPackage> {

	private static final ILogger logger = LogManager.getInstance().getLogger(Distributor.class);

	private Measurements collectedMeasurements = new Measurements() ;
	private Measurements collectedBufferedMeasurements = new Measurements() ;
	private final SolvisDataObserver solvisDataObserver = new SolvisDataObserver();
	private final ConnectionStateObserver connectionStateObserver = new ConnectionStateObserver();
	private final SolvisStateObserver solvisStateObserver = new SolvisStateObserver();
	private final HumanAccessObserver humanAccessObserver = new HumanAccessObserver();
	private final AliveThread aliveThread = new AliveThread();
	private final PeriodicBurstThread periodicBurstThread;
	private final int bufferedIntervall_ms;
	private boolean burstUpdate = false;

	public Distributor(Unit unit) {
		this.bufferedIntervall_ms = unit.getBufferedInterval_ms();
		if (this.bufferedIntervall_ms > 0) {
			this.periodicBurstThread = new PeriodicBurstThread();
			this.periodicBurstThread.start();
		} else {
			this.periodicBurstThread = null;
		}
	}

	private class SolvisDataObserver implements Observer.IObserver<SolvisData> {

		@Override
		public void update(SolvisData data, Object source) {

			Collection<SolvisData> toSend = null;

			synchronized (Distributor.this) {

				boolean buffered = data.getDescription().isBuffered() && Distributor.this.periodicBurstThread != null;

				if (buffered && data.isFastChange()
						&& data.getTimeStamp() - data.getSentTimeStamp() > Constants.FORCE_UPDATE_AFTER_N_INTERVALS
								* Distributor.this.bufferedIntervall_ms) {
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

	private void add(SolvisData data, Measurements measurements) {
		SolvisData former = measurements.add(data);
		if (former != null) {
			logger.error(
					"measurements not unique. Timestamps of <" + data.getId() + ">: Former: " + former.getTimeStamp()
							+ ", New: " + data.getTimeStamp() + ". Former measurement value is ignored");
		}
	}

	private class SolvisStateObserver implements Observer.IObserver<SolvisState> {

		@Override
		public void update(SolvisState data, Object source) {
			Distributor.this.notify(data.createJsonPackage());
		}

	}

	private class ConnectionStateObserver implements Observer.IObserver<ConnectionState> {

		@Override
		public void update(ConnectionState data, Object source) {
			Distributor.this.notify(data.createJsonPackage());

		}

	}

	private class HumanAccessObserver implements Observer.IObserver<HumanAccess> {

		@Override
		public void update(HumanAccess data, Object source) {
			ConnectionStatus status = data.getConnectionStatus();
			try {
				Distributor.this.notify(new ConnectionState(status).createJsonPackage());
			} catch (Throwable e) {
			}

		}

	}

	private void sendCollection(Collection< SolvisData > sendData) {
		long timeStamp = System.currentTimeMillis();
		if (!sendData.isEmpty()) {
			for (SolvisData data : sendData) {
				data.setSentData(timeStamp);
			}
			this.aliveThread.trigger();
			MeasurementsPackage sendPackage = new MeasurementsPackage(sendData);
			this.notify(sendPackage);
		}
	}

	public void sendCollection(Measurements measurements) {
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
						if (!this.triggered) {
							sendAlive = true;
						} else {
							this.triggered = false;
						}
					}
					if (sendAlive) {
						Distributor.this.notify(new ConnectionState(ConnectionStatus.ALIVE, "").createJsonPackage());
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
			this.triggered = true; // disable send alive
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

	public void setBurstUpdate(boolean burstUpdate) {
		boolean send;
		Collection<SolvisData> collection = null;
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
		logger.debug("Burst update " + comment);
	}

	private SolvisStateObserver getSolvisStateObserver() {
		return this.solvisStateObserver;
	}

	public void register(Solvis solvis) {
		solvis.registerAbortObserver(new IObserver<Boolean>() {

			@Override
			public void update(Boolean data, Object source) {
				abort();

			}
		});
		solvis.getAllSolvisData().registerObserver(this.getSolvisDataObserver());
		solvis.getConnection().register(this.getConnectionStateObserver());
		solvis.getSolvisState().register(this.getSolvisStateObserver());
		solvis.registerScreenChangedByHumanObserver(this.humanAccessObserver);
		this.aliveThread.start();
	}

}
