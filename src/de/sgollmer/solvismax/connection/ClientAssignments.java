/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import de.sgollmer.solvismax.connection.CommandHandler.ClosingThread;
import de.sgollmer.solvismax.connection.Server.Client;
import de.sgollmer.solvismax.model.CommandScreenRestore;
import de.sgollmer.solvismax.model.Solvis;

public class ClientAssignments {
	private final int clientId;
	private final Solvis solvis;
	private Client client;
	private final State state = new State();
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
		return this.client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public State getState() {
		return this.state;
	}

	public ClosingThread getClosingThread() {
		return this.closingThread;
	}

	public void setClosingThread(ClosingThread closingThread) {
		this.closingThread = closingThread;
	}

	public int getClientId() {
		return this.clientId;
	}

	public Solvis getSolvis() {
		return this.solvis;
	}

	public static class State {
		private boolean screenRestoreInhibit = false;
		private boolean optimizationEnable = true;
		private boolean commandEnable = true;

		public boolean isScreenRestoreInhibit() {
			return this.screenRestoreInhibit;
		}

		public void setScreenRestoreInhibit(boolean screenRestoreInhibit) {
			this.screenRestoreInhibit = screenRestoreInhibit;
		}

		public boolean isOptimizationEnable() {
			return this.optimizationEnable;
		}

		public void setOptimizationEnable(boolean optimizationEnable) {
			this.optimizationEnable = optimizationEnable;
		}

		public boolean isCommandEnable() {
			return this.commandEnable;
		}

		public void setCommandEnable(boolean commandEnable) {
			this.commandEnable = commandEnable;
		}
	}

	public void enableGuiCommands(boolean enable) {
		boolean set = false;
		if (enable && !this.getState().isCommandEnable()) {
			set = true;
		} else if (!enable && this.getState().isCommandEnable()) {
			set = true;
		}
		if (set) {
			this.getState().setCommandEnable(enable);
			this.getSolvis().commandEnable(enable);
			;
		}

	}

	public void screenRestoreInhibit(boolean inhibit) {
		boolean set = false;
		if (inhibit && !this.getState().isScreenRestoreInhibit()) {
			set = true;
		} else if (!inhibit && this.getState().isScreenRestoreInhibit()) {
			set = true;
		}
		if (set) {
			this.getState().setScreenRestoreInhibit(inhibit);
			this.getSolvis().execute(new CommandScreenRestore(!inhibit));
		}
	}

	public void optimizationInhibit(boolean inhibit) {
		boolean set = false;
		if (inhibit && this.getState().isOptimizationEnable()) {
			set = true;
		} else if (!inhibit && !this.getState().isOptimizationEnable()) {
			set = true;
		}
		if (set) {
			this.getState().setOptimizationEnable(!inhibit);
			this.getSolvis().commandOptimization(!inhibit);
		}
	}

	public void clientClosed() {
		this.enableGuiCommands(true);
		this.optimizationInhibit(false);
		this.screenRestoreInhibit(false);
	}

	public void serviceReset() {
		this.solvis.serviceReset();

	}
}