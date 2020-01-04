/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.transfer.ConnectionState;
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.connection.transfer.MeasurementsPackage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.SolvisState;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class Distributor extends Observable<JsonPackage> {

	private static final Logger logger = LogManager.getLogger(Distributor.class);

	private Measurements collectedMeasurements = new Measurements() {

		private Collection<SolvisData> measurements = new ArrayList<>();

		@Override
		public void add(SolvisData data) {
			this.measurements.add(data);
		}

		@Override
		public void clear() {
			this.measurements = new ArrayList<>() ;

		}

		@Override
		public Collection<SolvisData> get() {
			return this.measurements;
		}

		@Override
		public boolean isEmpty() {
			return measurements.isEmpty();
		}
	};
	private Measurements collectedBufferedMeasurements = new Measurements() {

		private Map<String, SolvisData> measurements = new HashMap<>();

		@Override
		public Collection<SolvisData> get() {
			return this.measurements.values();
		}

		@Override
		public void clear() {
			this.measurements = new HashMap<>();
		}

		@Override
		public void add(SolvisData data) {
			this.measurements.put(data.getId(), data) ;
		}

		@Override
		public boolean isEmpty() {
			return measurements.isEmpty();
		}
	};
	private final SolvisDataObserver solvisDataObserver = new SolvisDataObserver();
	private final ConnectionStateObserver connectionStateObserver = new ConnectionStateObserver();
	private final SolvisStateObserver solvisStateObserver = new SolvisStateObserver();
	private final UserAccessObserver userAccessObserver = new UserAccessObserver();
	private final AliveThread aliveThread = new AliveThread();
	private final PeriodicBurstThread periodicBurstThread;
	private final int bufferedIntervall_ms;
	private boolean burstUpdate = false;

	public Distributor(int bufferedIntervall_ms) {
		this.bufferedIntervall_ms = bufferedIntervall_ms;
		if (bufferedIntervall_ms > 0) {
			this.periodicBurstThread = new PeriodicBurstThread();
			this.periodicBurstThread.start();
		} else {
			this.periodicBurstThread = null;
		}
	}

	private class SolvisDataObserver implements Observer.ObserverI<SolvisData> {

		@Override
		public void update(SolvisData data, Object source) {
			synchronized (Distributor.this) {

				if (data.getDescription().isBuffered() && periodicBurstThread != null) {
					collectedBufferedMeasurements.add(data);

				} else {
					collectedMeasurements.add(data);
				}
				if (!burstUpdate) {
					sendCollection(collectedMeasurements);
				}
			}
		}
	}

	private class SolvisStateObserver implements Observer.ObserverI<SolvisState> {

		@Override
		public void update(SolvisState data, Object source) {
			Distributor.this.notify(data.getPackage());
		}

	}

	private class ConnectionStateObserver implements Observer.ObserverI<ConnectionState> {

		@Override
		public void update(ConnectionState data, Object source) {
			Distributor.this.notify(data.createJsonPackage());

		}

	}

	private class UserAccessObserver implements Observer.ObserverI<Boolean> {

		@Override
		public void update(Boolean data, Object source) {
			ConnectionStatus status = data ? ConnectionStatus.USER_ACCESS_DETECTED
					: ConnectionStatus.USER_ACCESS_FINISHED;
			Distributor.this.notify(new ConnectionState(status).createJsonPackage());

		}

	}

	private void sendCollection(Measurements measurements) {
		Collection<SolvisData> collectedMeasurements = null;
		synchronized (this) {
			if (!measurements.isEmpty()) {
				collectedMeasurements = measurements.get();
				measurements.clear();
			}
		}
		if (collectedMeasurements != null) {
			aliveThread.trigger();
			MeasurementsPackage sendPackage = new MeasurementsPackage(collectedMeasurements);
			this.notify(sendPackage);
		}
	}

	public void abort() {
		this.aliveThread.abort();
		if (this.periodicBurstThread != null) {
			this.periodicBurstThread.abort();
		}
	}

	public SolvisDataObserver getSolvisDataObserver() {
		return this.solvisDataObserver;
	}

	public ConnectionStateObserver getConnectionStateObserver() {
		return this.connectionStateObserver;
	}

	private class AliveThread extends Thread {

		public AliveThread() {
			super("Alive thread");
		}

		boolean abort = false;
		boolean triggered = false;

		@Override
		public void run() {

			while (!abort) {
				boolean sendAlive = false;
				synchronized (this) {
					try {
						this.wait(Constants.ALIVE_TIME);
					} catch (InterruptedException e) {
					}
					if (!triggered) {
						sendAlive = true;
					} else {
						triggered = false;
					}
				}
				if (sendAlive) {
					Distributor.this.notify(new ConnectionState(ConnectionStatus.ALIVE, "").createJsonPackage());
				}
			}
		}

		public synchronized void trigger() {
			this.triggered = true;
			this.notifyAll();
		}

		public synchronized void abort() {
			this.abort = true;
			this.triggered = true; // disable send alive
			this.notifyAll();
		}

	}

	private class PeriodicBurstThread extends Thread {

		private boolean abort;

		public PeriodicBurstThread() {
			super("PeriodicBurst");
		}

		@Override
		public void run() {
			while (!abort) {
				synchronized (this) {
					Calendar midNight = Calendar.getInstance();
					long now = midNight.getTimeInMillis();
					midNight.set(Calendar.HOUR_OF_DAY, 0);
					midNight.set(Calendar.MINUTE, 0);
					midNight.set(Calendar.SECOND, 0);
					midNight.set(Calendar.MILLISECOND, 200);

					long nextBurst = (now - midNight.getTimeInMillis()) / bufferedIntervall_ms * bufferedIntervall_ms
							+ midNight.getTimeInMillis() + bufferedIntervall_ms;

					try {
						this.wait(nextBurst - now);
					} catch (InterruptedException e) {
					}
				}
				if (!abort) {
					sendCollection(collectedBufferedMeasurements);
				}
			}
		}

		public synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}
	}

	public void setBurstUpdate(boolean burstUpdate) {
		boolean send = false;
		synchronized (this) {
			send = !burstUpdate && this.burstUpdate;
			this.burstUpdate = burstUpdate;
		}
		if (send && this.periodicBurstThread == null) {
			sendCollection(this.collectedMeasurements);
		}
		String comment = this.burstUpdate ? "started" : "finished";
		logger.debug("Burst update " + comment);
	}

	public SolvisStateObserver getSolvisStateObserver() {
		return solvisStateObserver;
	}

	public UserAccessObserver getUserAccessObserver() {
		return userAccessObserver;
	}

	public void register(Solvis solvis) {
		solvis.registerAbortObserver(new ObserverI<Boolean>() {

			@Override
			public void update(Boolean data, Object source) {
				abort();

			}
		});
		solvis.getAllSolvisData().registerObserver(this.getSolvisDataObserver());
		solvis.getConnection().register(this.getConnectionStateObserver());
		solvis.getSolvisState().register(this.getSolvisStateObserver());
		solvis.registerScreenChangedByUserObserver(userAccessObserver);
		this.aliveThread.start();
	}

	public interface Measurements {
		public void add(SolvisData data);

		public boolean isEmpty();

		public void clear();

		public Collection<SolvisData> get();
	}
}
