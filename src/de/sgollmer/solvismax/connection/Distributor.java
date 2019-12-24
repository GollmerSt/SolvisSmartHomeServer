package de.sgollmer.solvismax.connection;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.transfer.ConnectionState;
import de.sgollmer.solvismax.connection.transfer.JsonPackage;
import de.sgollmer.solvismax.connection.transfer.MeasurementsPackage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.SolvisState;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class Distributor extends Observable<JsonPackage> {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Distributor.class) ;

	private Collection<SolvisData> collectedMeasurements = null;
	private final SolvisDataObserver solvisDataObserver = new SolvisDataObserver();
	private final ConnectionStateObserver connectionStateObserver = new ConnectionStateObserver();
	private final SolvisStateObserver solvisStateObserver = new SolvisStateObserver() ;
	private final UserAccessObserver userAccessObserver = new UserAccessObserver() ;
	private final AliveThread aliveThread = new AliveThread();
	private boolean burstUpdate = false;

	public Distributor() {
		this.aliveThread.start();
	}

	private class SolvisDataObserver implements Observer.ObserverI<SolvisData> {

		@Override
		public void update(SolvisData data, Object source) {
			synchronized (Distributor.this) {

				if (collectedMeasurements == null) {
					collectedMeasurements = new ArrayList<>();
				}
				collectedMeasurements.add(data);
			}
			if (!burstUpdate) {
				sendCollection();
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
		public void update(ConnectionState data, Object source ) {
			Distributor.this.notify(data.createJsonPackage());

		}

	}
	
	private class UserAccessObserver implements Observer.ObserverI<Boolean> {

		@Override
		public void update(Boolean data, Object source) {
			ConnectionStatus status = data ? ConnectionStatus.USER_ACCESS_DETECTED: ConnectionStatus.USER_ACCESS_FINISHED ;
			Distributor.this.notify(new ConnectionState(status).createJsonPackage());
			
		}
		
	}

	private void sendCollection() {
		Collection<SolvisData> collectedMeasurements = null;
		synchronized (this) {
			if (this.collectedMeasurements != null) {
				collectedMeasurements = this.collectedMeasurements;
				this.collectedMeasurements = null;
			}
		}
		if (collectedMeasurements != null) {
			aliveThread.trigger();
			MeasurementsPackage sendPackage = new MeasurementsPackage(collectedMeasurements);
			this.notify(sendPackage);
		}
	}

	public void teminate() {
		this.aliveThread.shutdown();
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

		boolean terminate = false;
		boolean triggered = false;

		@Override
		public void run() {

			while (!terminate) {
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

		public synchronized void shutdown() {
			this.terminate = true;
			this.triggered = true; // disable send alive
			this.notifyAll();
		}

	}

	public void setBurstUpdate(boolean burstUpdate) {
		boolean send = false ;
		synchronized (this) {
			send = !burstUpdate && this.burstUpdate ;
			this.burstUpdate = burstUpdate;			
		}
		if (send) {
			sendCollection();
		}
		String comment = this.burstUpdate?"started":"finished" ;
		logger.debug("Burst update " + comment );
	}

	public SolvisStateObserver getSolvisStateObserver() {
		return solvisStateObserver;
	}

	public UserAccessObserver getUserAccessObserver() {
		return userAccessObserver;
	}
	
	public void register( Solvis solvis ) {
		solvis.getAllSolvisData().registerObserver(this.getSolvisDataObserver());
		solvis.getConnection().register(this.getConnectionStateObserver());
		solvis.getSolvisState().register(this.getSolvisStateObserver());
		solvis.registerScreenChangedByUserObserver(userAccessObserver);
		
	}
}
