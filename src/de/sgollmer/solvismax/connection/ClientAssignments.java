/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.sgollmer.solvismax.connection.CommandHandler.ClosingThread;
import de.sgollmer.solvismax.error.ClientAssignmentException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.WatchDog.Event;
import de.sgollmer.solvismax.model.command.CommandScreenRestore;

public class ClientAssignments {
	private final CommandHandler commandHandler;
	private IClient client;
	private final Map<Solvis, State> states = new HashMap<>();
	private ClosingThread closingThread = null;

	ClientAssignments(final CommandHandler commandHandler, final IClient client) {
		this.commandHandler = commandHandler;
		this.client = client;
	}

	void add(final Solvis solvis) {
		if (solvis != null && !this.states.containsKey(solvis)) {
			this.states.put(solvis, new State());
		}
	}

	void reconnected(final IClient client) {
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

	void setClient(final IClient client) {
		this.client = client;
	}

	State getState(final Solvis solvis) {
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

	void setClosingThread(final ClosingThread closingThread) {
		this.closingThread = closingThread;
	}

	private static class State {
		private boolean optimizationEnable = true;
		private boolean controlEnable = true;

		private boolean isOptimizationEnable() {
			return this.optimizationEnable;
		}

		private void setOptimizationEnable(final boolean optimizationEnable) {
			this.optimizationEnable = optimizationEnable;
		}

		private boolean isControlEnable() {
			return this.controlEnable;
		}

		private void setControlEnable(final boolean controlEnable) {
			this.controlEnable = controlEnable;
		}
	}

	private State getStateAndCheck(Solvis solvis) throws ClientAssignmentException {
		State state = this.getState(solvis);
		if (state == null) {
			throw new ClientAssignmentException("Error: Client assignment error");
		}
		return state;
	}

	void enableControlCommands(final Solvis solvis, final boolean enable) throws ClientAssignmentException {
		State state = this.getStateAndCheck(solvis);
		if (enable != state.isControlEnable()) {
			state.setControlEnable(enable);
			this.commandHandler.handleControlEnable(solvis);
		}

	}

	void screenRestoreEnable(final Solvis solvis, final boolean enable) throws ClientAssignmentException {
		State state = this.getStateAndCheck(solvis);
		solvis.execute(new CommandScreenRestore(enable, state));
	}

	void optimizationEnable(final Solvis solvis, final boolean enable) throws ClientAssignmentException {
		State state = this.getStateAndCheck(solvis);
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

	void serviceAccess(final Solvis solvis, Event event)
			throws ClientAssignmentException, IOException, TerminationException {
		this.getStateAndCheck(solvis);
		solvis.serviceAccess(event);
	}

	void updateControlChannels(final Solvis solvis) throws ClientAssignmentException {
		this.getStateAndCheck(solvis);
		solvis.updateReadOnlyControlChannels();

	}

	Solvis getSolvis() {
		if (this.states.size() != 1) {
			return null;
		} else {
			return this.states.keySet().iterator().next();
		}
	}

	enum ControlEnableStatus {
		TRUE, FALSE, INVALID
	}

	ControlEnableStatus getControlEnableStatus(final Solvis solvis) {
		State state = this.getState(solvis);
		if (state == null) {
			return ControlEnableStatus.INVALID;
		}
		return state.controlEnable?ControlEnableStatus.TRUE:ControlEnableStatus.FALSE;
	}

	public void debugClear(Solvis solvis) throws ClientAssignmentException, TypeException {
		this.getStateAndCheck(solvis);
		solvis.getAllSolvisData().debugClear();
		solvis.updateControlChannels();
	}
}