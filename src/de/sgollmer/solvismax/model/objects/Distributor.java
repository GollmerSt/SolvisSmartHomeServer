package de.sgollmer.solvismax.model.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import de.sgollmer.solvismax.connection.ConnectionState;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.transfer.JsonPackage;
import de.sgollmer.solvismax.model.transfer.MeasurementsPackage;

public class Distributor extends Observable<JsonPackage> {

	private static int WAIT_TIME_BEFORE_SENDING_MS = 10;

	private Collection<SolvisData> collectedMeasurements = null;
	private SendTrigger sendTrigger = new SendTrigger();
	private final SolvisDataObserver solvisDataObserver = new SolvisDataObserver();
	private final ConnectionStateObserver connectionStateObserver = new ConnectionStateObserver();

	private class SolvisDataObserver implements Observer.ObserverI<SolvisData> {

		@Override
		public void update(SolvisData data) {
			synchronized (Distributor.this) {

				if (collectedMeasurements == null) {
					collectedMeasurements = new ArrayList<>();
				}
				collectedMeasurements.add(data);
			}
			sendTrigger.trigger();
		}
	}
	
	private class ConnectionStateObserver implements Observer.ObserverI<ConnectionState> {

		@Override
		public void update(ConnectionState data) {
			Distributor.this.notify(data.createJsonPackage());
			
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
			MeasurementsPackage sendPackage = new MeasurementsPackage(collectedMeasurements);
			this.notify(sendPackage);
		}
	}

	public void teminate() {
		this.sendTrigger.shutdown();
	}

	public SolvisDataObserver getSolvisDataObserver() {
		return this.solvisDataObserver;
	}
	
	public ConnectionStateObserver getConnectionStateObserver() {
		return this.connectionStateObserver ;
	}

	private class SendTrigger implements Runnable {

		private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

		private boolean terminate;

		@Override
		public void run() {
			synchronized (this) {
				boolean terminate = false ;
				try {
					this.terminate = false;
					this.wait(WAIT_TIME_BEFORE_SENDING_MS);
					terminate = this.terminate ;
				} catch (InterruptedException e) {
				}
				if (!terminate) {
					sendCollection();
				}
			}

		}

		public synchronized void trigger() {
			this.terminate = true;
			this.notifyAll();
			executor.execute(this); // Da nur ein Thread gleichzeitig läuft
									// (FixedThreadPool(1)), kann gleich der
									// neue "gestartet" werden
		}

		public synchronized void shutdown() {
			this.terminate = true;
			this.notifyAll();
			executor.shutdown();
		}
	}

}
