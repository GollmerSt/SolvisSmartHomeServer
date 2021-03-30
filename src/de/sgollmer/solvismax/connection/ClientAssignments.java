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
import de.sgollmer.solvismax.error.ClientAssignmentException;
import de.sgollmer.solvismax.model.CommandScreenRestore;
import de.sgollmer.solvismax.model.Solvis;

public class ClientAssignments {
	private final CommandHandler commandHandler;
	private IClient client;
	private final Map<Solvis, State> states = new HashMap<>();
	private ClosingThread closingThread = null;

	ClientAssignments(CommandHandler commandHandler, IClient client) {
		this.commandHandler = commandHandler;
		this.client = client;
	}

	void add(Solvis solvis) {
		if (solvis != null && !this.states.containsKey(solvis)) {
			this.states.put(solvis, new State());
		}
	}

	void reconnected(IClient client) {
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
		private boolean optimizationEnable = true;
		private boolean controlEnable = true;

		private boolean isOptimizationEnable() {
			return this.optimizationEnable;
		}

		private void setOptimizationEnable(boolean optimizationEnable) {
			this.optimizationEnable = optimizationEnable;
		}

		private boolean isControlEnable() {
			return this.controlEnable;
		}

		private void setControlEnable(boolean controlEnable) {
			this.controlEnable = controlEnable;
		}
	}

	void enableControlCommands(Solvis solvis, boolean enable) throws ClientAssignmentException {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentException("Error: Client assignment error");
		}
		if (enable != state.isControlEnable()) {
			state.setControlEnable(enable);
			this.commandHandler.handleControlEnable(solvis);
		}

	}

	void screenRestoreEnable(Solvis solvis, boolean enable) throws ClientAssignmentException {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentException("Error: Client assignment error");
		}
		solvis.execute(new CommandScreenRestore(enable, state));
	}

	void optimizationEnable(Solvis solvis, boolean enable) throws ClientAssignmentException {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentException("Error: Client assignment error");
		}
		if (enable != state.isOptimizationEnable()) {
			this.getState().setOptimizationEnable(enable);
			solvis.commandOptimization(enable);
		}
	}

	void clientClosed() throws ClientAssignmentException {
		for (Solvis solvis : this.states.keySet()) {
			this.enableControlCommands(solvis, true);
			this.optimizationEnable(solvis, true);
			this.screenRestoreEnable(solvis, true);
		}
	}

	void serviceReset(Solvis solvis) throws ClientAssignmentException {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentException("Error: Client assignment error");
		}
		solvis.serviceReset();
	}

	void updateControlChannels(Solvis solvis) throws ClientAssignmentException {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentException("Error: Client assignment error");
		}
		solvis.updateReadOnlyControlChannels();

	}

	Solvis getSolvis() {
		if (this.states.size() != 1) {
			return null;
		} else {
			return this.states.keySet().iterator().next();
		}
	}
	
	boolean getControlEnabled(Solvis solvis) {
		return this.getState(solvis).controlEnable;
	}
}