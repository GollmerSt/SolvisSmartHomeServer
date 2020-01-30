package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.connection.CommandHandler.ClosingThread;
import de.sgollmer.solvismax.connection.Server.Client;
import de.sgollmer.solvismax.model.Solvis;

public class ClientAssignments {
	private final int clientId;
	private final Solvis solvis;
	private Client client;
	private final State state = new State() ;
	private ClosingThread closingThread = null;

	public ClientAssignments(int clientid, Solvis solvis, Client client) {
		this.clientId = clientid;
		this.solvis = solvis;
		this.client = client;
	}

	public void reconnect(Client client) {
		this.abort();
		this.client = client;
	}

	public synchronized void abort() {
		if (this.closingThread != null) {
			this.closingThread.abort();
			this.closingThread = null;
		}
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public State getState()  {
		return state;
	}

	public ClosingThread getClosingThread() {
		return closingThread;
	}

	public void setClosingThread(ClosingThread closingThread) {
		this.closingThread = closingThread;
	}

	public int getClientId() {
		return clientId;
	}

	public Solvis getSolvis() {
		return solvis;
	}
	
	public static class State {
		private boolean screenRestoreInhibit = false ;
		private boolean optimizationEnable = true ;
		
		public boolean isScreenRestoreInhibit() {
			return screenRestoreInhibit;
		}
		public void setScreenRestoreInhibit(boolean screenRestoreInhibit) {
			this.screenRestoreInhibit = screenRestoreInhibit;
		}
		public boolean isOptimizationEnable() {
			return optimizationEnable;
		}
		public void setOptimizationEnable(boolean optimizationEnable) {
			this.optimizationEnable = optimizationEnable;
		}

		
	}
}