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
	private IClient client;
	private final Map<Solvis, State> states = new HashMap<>();
	private ClosingThread closingThread = null;

	ClientAssignments(IClient client) {
		this.client = client;
	}

	void add(Solvis solvis) {
		if (solvis != null && !this.states.containsKey(solvis)) {
			this.states.put(solvis, new State());
		}
	}

	void reconnect(IClient client) {
		this.abort();
		this.client = client;
	}

	synchronized void abort() {
		if (this.closingThread != null) {
			this.closingThread.abort();
			this.closingThread = null;
		}
	}

	IClient getClient() {
		return this.client;
	}

	void setClient(IClient client) {
		this.client = client;
	}

	State getState(Solvis solvis) {
		if (solvis == null) {
			return this.getState();
		} else {
			return this.states.get(solvis);
		}
	}

	private State getState() {
		if (this.states.size() == 1) {
			return this.states.values().iterator().next();
		}
		return null;
	}

	ClosingThread getClosingThread() {
		return this.closingThread;
	}

	void setClosingThread(ClosingThread closingThread) {
		this.closingThread = closingThread;
	}

	private static class State {
		private boolean screenRestoreEnable = true;
		private boolean optimizationEnable = true;
		private boolean commandEnable = true;

		private boolean isScreenRestoreEnable() {
			return this.screenRestoreEnable;
		}

		private void setScreenRestoreEnable(boolean screenRestoreInhibit) {
			this.screenRestoreEnable = screenRestoreInhibit;
		}

		private boolean isOptimizationEnable() {
			return this.optimizationEnable;
		}

		private void setOptimizationEnable(boolean optimizationEnable) {
			this.optimizationEnable = optimizationEnable;
		}

		private boolean isCommandEnable() {
			return this.commandEnable;
		}

		private void setCommandEnable(boolean commandEnable) {
			this.commandEnable = commandEnable;
		}
	}

	void enableGuiCommands(Solvis solvis, boolean enable) {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentError("Error: Client assignment error");
		}
		if (enable != state.isCommandEnable()) {
			state.setCommandEnable(enable);
			solvis.commandEnable(enable);
		}

	}

	void screenRestoreEnable(Solvis solvis, boolean enable) {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentError("Error: Client assignment error");
		}
		if (enable != state.isScreenRestoreEnable()) {
			state.setScreenRestoreEnable(enable);
			solvis.execute(new CommandScreenRestore(enable));
		}
	}

	void optimizationEnable(Solvis solvis, boolean enable) {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentError("Error: Client assignment error");
		}
		if (enable != state.isOptimizationEnable()) {
			this.getState().setOptimizationEnable(enable);
			solvis.commandOptimization(enable);
		}
	}

	void clientClosed() {
		for (Solvis solvis : this.states.keySet()) {
			this.enableGuiCommands(solvis, true);
			this.optimizationEnable(solvis, true);
			this.screenRestoreEnable(solvis, true);
		}
	}

	void serviceReset(Solvis solvis) {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentError("Error: Client assignment error");
		}
		solvis.serviceReset();
	}

	void updateControlChannels(Solvis solvis) {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentError("Error: Client assignment error");
		}
		solvis.updateControlChannels();

	}

	Solvis getSolvis() {
		if (this.states.size() != 1) {
			return null;
		} else {
			return this.states.keySet().iterator().next();
		}
	}
}