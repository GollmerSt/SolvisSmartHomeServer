/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import java.util.HashMap;
import java.util.Map;

import de.sgollmer.solvismax.connection.CommandHandler.ClosingThread;
import de.sgollmer.solvismax.connection.Server.Client;
import de.sgollmer.solvismax.error.ClientAssignmentError;
import de.sgollmer.solvismax.model.CommandScreenRestore;
import de.sgollmer.solvismax.model.Solvis;

public class ClientAssignments {
	private final String clientId;
	private Client client;
	private final Map<Solvis, State> states = new HashMap<>();
	private ClosingThread closingThread = null;

	public ClientAssignments(int clientid, Solvis solvis, Client client) {
		this.clientId = Integer.toString(clientid);
		this.client = client;
		this.states.put(solvis, new State(solvis));
	}

	public ClientAssignments(String clientid, Client client) {
		this.clientId = clientid;
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

	public State getState(Solvis solvis) {
		if (solvis == null) {
			return this.getState();
		} else {
			return this.states.get(solvis);
		}
	}

	public State getState() {
		if (this.states.size() == 1) {
			return this.states.values().iterator().next();
		}
		return null;
	}

	public ClosingThread getClosingThread() {
		return this.closingThread;
	}

	public void setClosingThread(ClosingThread closingThread) {
		this.closingThread = closingThread;
	}

	public String getClientId() {
		return this.clientId;
	}

	public Solvis getSolvis() {
		if (this.getState() != null) {
			return this.getState().solvis;
		}
		return null;
	}

	public static class State {
		private final Solvis solvis;
		private boolean screenRestoreInhibit = false;
		private boolean optimizationEnable = true;
		private boolean commandEnable = true;

		public State(Solvis solvis) {
			this.solvis = solvis;
		}

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
		this.enableGuiCommands(null, enable);
	}

		public void enableGuiCommands(Solvis solvis, boolean enable) {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentError("Error: Client assignment error");
		}
		boolean set = false;
		if (enable && !state.isCommandEnable()) {
			set = true;
		} else if (!enable && state.isCommandEnable()) {
			set = true;
		}
		if (set) {
			state.setCommandEnable(enable);
			this.getSolvis().commandEnable(enable);
		}

	}

	public void screenRestoreInhibit(boolean inhibit) {
		this.screenRestoreInhibit(null, inhibit);
	}

	public void screenRestoreInhibit(Solvis solvis, boolean inhibit) {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentError("Error: Client assignment error");
		}
		boolean set = false;
		if (inhibit && !state.isScreenRestoreInhibit()) {
			set = true;
		} else if (!inhibit && state.isScreenRestoreInhibit()) {
			set = true;
		}
		if (set) {
			state.setScreenRestoreInhibit(inhibit);
			this.getSolvis().execute(new CommandScreenRestore(!inhibit));
		}
	}

	
	public void optimizationInhibit(boolean inhibit) {
		this.optimizationInhibit(null, inhibit);
	}

	public void optimizationInhibit(Solvis solvis, boolean inhibit) {
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
		for (State state : this.states.values()) {
			this.enableGuiCommands(state.solvis, true);
			this.optimizationInhibit(state.solvis, false);
			this.screenRestoreInhibit(state.solvis, false);
		}
	}
	
	public void serviceReset() {
		this.serviceReset(null);
	}

	public void serviceReset(Solvis solvis) {
		State state = this.getState(solvis);
		state.solvis.serviceReset();
	}

	public void updateControlChannels() {
		this.updateControlChannels(null);
	}
	public void updateControlChannels(Solvis solvis) {
		State state = this.getState(solvis);
		state.solvis.updateControlChannels();

	}
}