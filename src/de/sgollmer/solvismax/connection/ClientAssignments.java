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
import de.sgollmer.solvismax.error.ClientAssignmentError;
import de.sgollmer.solvismax.model.CommandScreenRestore;
import de.sgollmer.solvismax.model.Solvis;

public class ClientAssignments {
	private final String clientId;
	private IClient client;
	private final Map<Solvis, State> states = new HashMap<>();
	private ClosingThread closingThread = null;

	public ClientAssignments(int clientid, Solvis solvis, IClient client) {
		this.clientId = Integer.toString(clientid);
		this.client = client;
		this.states.put(solvis, new State(solvis));
	}

	public ClientAssignments(String clientid, IClient client) {
		this.clientId = clientid;
		this.client = client;
	}

	public void add(Solvis solvis) {
		if (solvis != null && !this.states.containsKey(solvis)) {
			this.states.put(solvis, new State(solvis));
		}
	}

	public void reconnect(IClient client) {
		this.abort();
		this.client = client;
	}

	public synchronized void abort() {
		if (this.closingThread != null) {
			this.closingThread.abort();
			this.closingThread = null;
		}
	}

	public IClient getClient() {
		return this.client;
	}

	public void setClient(IClient client) {
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
		private boolean screenRestoreEnable = true;
		private boolean optimizationEnable = true;
		private boolean commandEnable = true;

		public State(Solvis solvis) {
			this.solvis = solvis;
		}

		public boolean isScreenRestoreEnable() {
			return this.screenRestoreEnable;
		}

		public void setScreenRestoreEnable(boolean screenRestoreInhibit) {
			this.screenRestoreEnable = screenRestoreInhibit;
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

	public void enableGuiCommands(Solvis solvis, boolean enable) {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentError("Error: Client assignment error");
		}
		if (enable != state.isCommandEnable()) {
			state.setCommandEnable(enable);
			this.getSolvis().commandEnable(enable);
		}

	}

	public void screenRestoreEnable(Solvis solvis, boolean enable) {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentError("Error: Client assignment error");
		}
		if (enable != state.isScreenRestoreEnable()) {
			state.setScreenRestoreEnable(enable);
			this.getSolvis().execute(new CommandScreenRestore(enable));
		}
	}

	public void optimizationEnable(Solvis solvis, boolean enable) {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentError("Error: Client assignment error");
		}
		if (enable != state.isOptimizationEnable()) {
			this.getState().setOptimizationEnable(enable);
			this.getSolvis().commandOptimization(enable);
		}
	}

	public void clientClosed() {
		for (State state : this.states.values()) {
			this.enableGuiCommands(state.solvis, true);
			this.optimizationEnable(state.solvis, true);
			this.screenRestoreEnable(state.solvis, true);
		}
	}

	public void serviceReset(Solvis solvis) {
		State state = this.getState(solvis);
		state.solvis.serviceReset();
	}

	public void updateControlChannels(Solvis solvis) {
		State state = this.getState(solvis);
		state.solvis.updateControlChannels();

	}
}