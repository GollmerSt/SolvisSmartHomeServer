/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.FatalError;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.helper.Helper.Reference;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.control.Dependency;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;

public class CommandControl extends Command {

	private static final ILogger logger = LogManager.getInstance().getLogger(CommandControl.class);

	private final ChannelDescription description;
	private SingleData<?> setValue;
	private final AbstractScreen screen;
	private boolean inhibit = false;
	private Reference<Integer> writeFailCount = new Reference<Integer>(0);
	private Reference<Integer> dependencyFailCount = new Reference<Integer>(0);
	private int readFailCount = 0;
	private Collection<ChannelDescription> readChannels = null;
	private State state = StateEnum.NONE.getState();
	private final Solvis solvis;
	private boolean mustBeFinished = false;
	private ResultStatus finalStatus = ResultStatus.SUCCESS;

	CommandControl(ChannelDescription description, SingleData<?> setValue, Solvis solvis) {
		this.setValue = setValue;
		this.description = description;
		this.screen = description.getScreen(solvis);
		this.solvis = solvis;
	}

	public CommandControl(ChannelDescription description, Solvis solvis) {
		this(description, null, solvis);
	}

	@Override
	protected boolean isInhibit() {
		return this.inhibit;
	}

	@Override
	protected void setInhibit(boolean inhibit) {
		this.inhibit = inhibit;
	}

	@Override
	protected AbstractScreen getScreen(Solvis solvis) {
		return this.screen;
	}

	/**
	 * 
	 * @param solvis
	 * @param description
	 * @param data         value which should be written
	 * @param failCountRef Reference to the failCount
	 * @param overwrite    true, if the write value of the stotred data should be
	 *                     overwritten after success
	 * @return ResultStatus
	 * @throws IOException
	 * @throws TerminationException
	 */

	private ResultStatus write(Solvis solvis, ChannelDescription description, SingleData<?> setValue,
			Reference<Integer> failCountRef, boolean overwrite) throws IOException, TerminationException {
		SolvisData data = solvis.getAllSolvisData().get(description);
		SolvisData clone = data.clone();
		clone.setSingleData(setValue);
		SetResult setResult = description.setValue(solvis, clone);
		if (setResult == null) {
			this.writeFailCount.increment();
			if (this.writeFailCount.get() < Constants.COMMAND_IGNORED_AFTER_N_FAILURES) {
				return ResultStatus.NO_SUCCESS;
			}
			logger.error("Set of channel <" + this.description.getId() + "> not successfull. Aborted.");
			return ResultStatus.SUCCESS;
		} else {
			switch (setResult.getStatus()) {
				case SUCCESS:
				case VALUE_VIOLATION:
					if (overwrite) {
						data.setSingleData(setResult);
						this.setValue = null;
					}
					return ResultStatus.SUCCESS;
				case NO_SUCCESS:
					return ResultStatus.SUCCESS; // Scanned field not available, ignored.
				case INTERRUPTED:
					return ResultStatus.INTERRUPTED;
				default:
					throw new FatalError("Responds of status <" + setResult.getStatus().name() + "> not defined");
			}
		}
	}

	@Override
	public ResultStatus execute(Solvis solvis) throws IOException, PowerOnException, TerminationException,
			NumberFormatException, TypeException, XmlException {

		boolean finished = false;
		while (!finished) {
			ResultStatus status = this.state.execute(this);
			switch (status) {
				case SUCCESS:
				case CONTINUE:
					this.state = this.state.next(this, solvis);
					break;
				case INTERRUPTED:
					if (!this.mustBeFinished) {
						return status;
					}
					break;
				case NO_SUCCESS:
					return status;
				case VALUE_VIOLATION:
					this.finalStatus = status;
					this.state = this.state.next(this, solvis);
					break;
			}
			if (this.state == null || this.inhibit && !this.mustBeFinished) {
				finished = true;
			}
		}
		return this.finalStatus;
	}

	@Override
	protected synchronized boolean toEndOfQueue() {
		int cmp = Constants.COMMAND_TO_QUEUE_END_AFTER_N_FAILURES;
		return this.writeFailCount.get() >= cmp || this.readFailCount >= cmp || this.dependencyFailCount.get() >= cmp;
	}

	@Override
	protected Handling getHandling(Command queueEntry, Solvis solvis) {
		if (!(queueEntry instanceof CommandControl)) {
			// new Handling(inQueueInhibit, inhibitAdd, insert)
			return new Handling(false, false, false);
		}
		CommandControl queueCommand = (CommandControl) queueEntry;
		if (queueEntry.isInhibit() || queueCommand.mustBeFinished) {
			// new Handling(inQueueInhibit, inhibitAdd, insert)
			return new Handling(false, false, false);
		} else {
			boolean inQueueInhibit = false;
			boolean inhibitAdd = false;
			boolean insert = false; // hat niedrigere Prio als inhibitAdd
			boolean finished = false;
			synchronized (this) {

				if (queueCommand.isWriting()) {
					finished = true;
				}
				boolean sameEnvironment = queueCommand.getScreen(solvis) == this.getScreen(solvis);

				Dependency dependency = this.description.getDependency();
				Dependency queueDependency = queueCommand.description.getDependency();

				sameEnvironment &= Dependency.equals(dependency, queueDependency, solvis);

				if (sameEnvironment) {

					if (queueCommand.isWriting()) {

						if (this.description == queueCommand.description && this.isWriting()) {
							inQueueInhibit = true;
						}
						inhibitAdd = !this.isWriting();
						insert = true;

					} else if (queueCommand.getRestoreChannel(solvis) != null) {

						if (this.description == queueCommand.description) {
							inQueueInhibit = true;
						}

					} else {
						inQueueInhibit = true;
					}
				}

				return new Handling(inQueueInhibit, inhibitAdd, insert, finished);
			}
		}

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Screen: ");
		builder.append(this.screen.getId());
		if (this.isWriting()) {
			builder.append(", channel: ");
			builder.append(this.description.getId());
			builder.append(", set value: ");
			builder.append(this.setValue.toString());
		}
		if (this.inhibit) {
			builder.append(", inhibited");
		}
		return builder.toString();
	}

	@Override
	protected boolean isWriting() {
		return this.setValue != null && !this.inhibit;
	}

	@Override
	protected void notExecuted() {
	}

	@Override
	protected boolean canBeIgnored(Command queueCommand) {
		if (!(queueCommand instanceof CommandControl)) {
			return false;
		}
		CommandControl control = (CommandControl) queueCommand;
		if (!control.isInhibit() && control.screen == this.screen) {
			return !this.isWriting() || control.isWriting() && control.description == this.description;
		} else {
			return false;
		}
	}

	public ChannelDescription getDescription() {
		return this.description;
	}

	public SingleData<?> getSetValue() {
		return this.setValue;
	}

	@Override
	Collection<ChannelDescription> getReadChannels() {
		return this.readChannels;
	}

	@Override
	ChannelDescription getRestoreChannel(Solvis solvis) {
		return this.description.getRestoreChannel(solvis);
	}

	@Override
	public Dependency getDependency(Solvis solvis) {
		return this.description.getDependency();
	}

	/**
	 * State machine for execution of a command including restore and dependency
	 * processing
	 * 
	 * @author stefa_000
	 *
	 */

	private enum StateEnum {
		NONE(new None()), //
		PREPARE_RESTORE(new PrepareRestore()), //
		PREPARE_DEPENDENCY(new PrepareDependency()), //
		EXECUTE_DEPENDENCY(new ExecuteDependency(false)), //
		WRITING(new Writing()), //
		READING(new Reading()), //
		RESTORE_DEPENDENCY(new ExecuteDependency(true)), //
		RESTORE(new Restore()), //
		FINISHED(new Finished());

		private final State state;

		private StateEnum(State state) {
			this.state = state;
		}

		public State getState() {
			return this.state;
		}
	}

	private static abstract class State {

		public abstract State next(CommandControl command, Solvis solvis);

		public abstract ResultStatus execute(CommandControl command) throws IOException, TerminationException,
				TypeException, NumberFormatException, PowerOnException, XmlException;

	}

	private static class None extends State {

		@Override
		public State next(CommandControl command, Solvis solvis) {
			if (command.description.getRestoreChannel(solvis) != null) {
				return StateEnum.PREPARE_RESTORE.getState();
			} else if (command.description.getDependency() != null) {
				return StateEnum.PREPARE_DEPENDENCY.getState();
			} else {
				return StateEnum.WRITING.getState();
			}
		}

		@Override
		public ResultStatus execute(CommandControl command) {
			return ResultStatus.CONTINUE;
		}

	}

	private static class PrepareRestore extends State {

		@Override
		public State next(CommandControl command, Solvis solvis) {
			if (command.description.getDependency() != null) {
				return StateEnum.PREPARE_DEPENDENCY.getState();
			} else {
				return StateEnum.WRITING.getState();
			}
		}

		@Override
		public ResultStatus execute(CommandControl command) throws NumberFormatException, IOException, PowerOnException,
				TerminationException, TypeException, XmlException {
			Solvis solvis = command.solvis;
			ChannelDescription restoreChannel = command.getRestoreChannel(command.solvis);
			SolvisData data = solvis.getAllSolvisData().get(restoreChannel);
			return restoreChannel.getValue(data, solvis) ? ResultStatus.SUCCESS : ResultStatus.NO_SUCCESS;
		}

	}

	private static class PrepareDependency extends State {

		@Override
		public State next(CommandControl command, Solvis solvis) {
			return StateEnum.EXECUTE_DEPENDENCY.getState();
		}

		@Override
		public ResultStatus execute(CommandControl command) throws NumberFormatException, IOException, PowerOnException,
				TerminationException, TypeException, XmlException {
			Solvis solvis = command.solvis;
			ChannelDescription dependencyChannel = command.getDependency(solvis).getChannelDescription(solvis);
			SolvisData data = solvis.getAllSolvisData().get(dependencyChannel);
			return dependencyChannel.getValue(data, solvis)?ResultStatus.SUCCESS:ResultStatus.NO_SUCCESS;
		}

	}

	private static class ExecuteDependency extends State {

		private final boolean restore;

		public ExecuteDependency(boolean restore) {
			this.restore = restore;
		}

		@Override
		public State next(CommandControl command, Solvis solvis) {
			return this.restore ? StateEnum.FINISHED.getState() : StateEnum.WRITING.getState();
		}

		@Override
		public ResultStatus execute(CommandControl command) throws IOException, TerminationException, TypeException {
			Solvis solvis = command.solvis;
			Dependency dependency = command.getDependency(solvis);
			ChannelDescription description = dependency.getChannelDescription(solvis);

			SingleData<?> data;

			SingleData<?> former = solvis.getAllSolvisData().get(description).getSingleData();

			if (this.restore) {
				data = former;
			} else {
				data = dependency.getData(solvis);
				if (data.equals(former)) {
					return ResultStatus.SUCCESS;
				}
			}

			command.mustBeFinished = true;
			ResultStatus status = command.write(solvis, description, data, command.dependencyFailCount, false);

			if (status == ResultStatus.VALUE_VIOLATION && !this.restore) {
				logger.error("Wrong dependency value definition of channel <" + command.description.getId()
						+ ">. Check control.xml. Error ignored.");
				status = ResultStatus.SUCCESS;
			}

			return status;
		}

	}

	private static class Writing extends State {

		@Override
		public State next(CommandControl command, Solvis solvis) {
			return StateEnum.READING.getState();
		}

		@Override
		public ResultStatus execute(CommandControl command) throws IOException, TerminationException {
			if (command.setValue == null) {
				return ResultStatus.CONTINUE;
			}

			Solvis solvis = command.solvis;

			ResultStatus status = command.write(solvis, command.description, command.setValue, command.writeFailCount,
					true);

			return status;
		}

	}

	private static class Reading extends State {

		@Override
		public State next(CommandControl command, Solvis solvis) {
			if (command.description.getDependency() != null) {
				return StateEnum.RESTORE_DEPENDENCY.getState();
			} else if (command.description.getRestoreChannel(solvis) != null) {
				return StateEnum.RESTORE.getState();
			} else {
				return null;
			}
		}

		@Override
		public ResultStatus execute(CommandControl command)
				throws NumberFormatException, IOException, PowerOnException, TerminationException, TypeException {

			Solvis solvis = command.solvis;

			if (command.readChannels == null) {

				Collection<ChannelDescription> readChannels = solvis.getSolvisDescription().getChannelDescriptions()
						.getChannelDescriptions(command.screen, solvis);

				ChannelDescription commandDependencyDescription = null;
				Dependency commandDependency = command.description.getDependency();

				Map<ChannelDescription, SingleData<?>> dependencyMap = new HashMap<>();

				if (commandDependency != null) {
					commandDependencyDescription = commandDependency.getChannelDescription(solvis);

					for (ChannelDescription description : readChannels) {
						Dependency dependency = description.getDependency();
						if (dependency != null) {
							dependencyMap.put(dependency.getChannelDescription(solvis), null);
						}
					}

					for (Map.Entry<ChannelDescription, SingleData<?>> entry : dependencyMap.entrySet()) {
						ChannelDescription description = entry.getKey();
						if (description == commandDependencyDescription) {
							entry.setValue(commandDependency.getData(solvis));
						} else {
							SolvisData data = solvis.getAllSolvisData().get(commandDependencyDescription);
							data = data.clone();
							boolean success = entry.getKey().getValue(data, solvis);
							if (success) {
								entry.setValue(data.getSingleData());
							} else {
								return ResultStatus.NO_SUCCESS;
							}
						}
					}
				}
				command.readChannels = new ArrayList<>();

				for (ChannelDescription description : readChannels) {
					Dependency dependency = description.getDependency();
					if (!dependencyMap.containsKey(description)) {
						if (dependency == null) {
							command.readChannels.add(description);
						} else if (dependency != null) {
							SingleData<?> current = dependencyMap.get(dependency.getChannelDescription(solvis));
							if (dependency.getData(solvis).equals(current)) {
								command.readChannels.add(description);
							}
						}
					}
				}
			}

			boolean readSuccess = true;

			for (Iterator<ChannelDescription> it = command.readChannels.iterator(); it.hasNext();) {
				ChannelDescription description = it.next();
				boolean success = description.getValue(solvis);
				if (success) {
					it.remove();
				}
				readSuccess &= success;
			}

			if (!readSuccess) {
				++command.readFailCount;
				if (command.readFailCount >= Constants.COMMAND_IGNORED_AFTER_N_FAILURES) {
					logger.error("Get channels of screen <" + command.screen.getId() + "> not successfull. Aborted.");
					readSuccess = true;
				}
			}

			return readSuccess ? ResultStatus.SUCCESS : ResultStatus.NO_SUCCESS;
		}

	}

	private static class Restore extends State {

		@Override
		public State next(CommandControl command, Solvis solvis) {
			return StateEnum.FINISHED.getState();
		}

		@Override
		public ResultStatus execute(CommandControl command) throws IOException, TerminationException, TypeException {
			Solvis solvis = command.solvis;
			ChannelDescription description = command.description.getRestoreChannel(solvis);

			SingleData<?> data = solvis.getAllSolvisData().get(description).getSingleData();

			ResultStatus status = command.write(solvis, description, data, command.dependencyFailCount, false);

			return status;
		}

	}

	private static class Finished extends State {

		@Override
		public State next(CommandControl command, Solvis solvis) {
			return null;
		}

		@Override
		public ResultStatus execute(CommandControl command) {
			return ResultStatus.SUCCESS;
		}

	}

	public boolean mustBeFinished() {
		return this.mustBeFinished;
	}
}
