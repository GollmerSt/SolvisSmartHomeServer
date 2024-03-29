/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.SolvisErrorException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.Helper.Reference;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.command.Handling.QueueStatus;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.control.Dependency;
import de.sgollmer.solvismax.model.objects.control.DependencyGroup;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.xmllibrary.XmlException;

public class CommandControl extends Command {

	private static final ILogger logger = LogManager.getInstance().getLogger(CommandControl.class);

	private final ChannelDescription description;
	private SingleData<?> setValue;
	private boolean write;
	private boolean inhibitRead;
	private final AbstractScreen screen;
	private boolean inhibit = false;
	private Reference<Integer> writeFailCount = new Reference<Integer>(0);
	private Reference<Integer> dependencyFailCount = new Reference<Integer>(0);
	private int readFailCount = 0;
	private Collection<ChannelDescription> readChannels = null;
	private State state = StateEnum.START.getState();
	private final Solvis solvis;
	private boolean mustBeFinished = false;
	private ResultStatus finalStatus = ResultStatus.SUCCESS;
	private long executionStartTime = -1;
	private final Integer monitoringPriority;

	private Map<ChannelDescription, Dependency> toRestore = new HashMap<>(3);

	private MySet dependencyGroupsToExecute = new MySet();
	private List<Dependency> dependenciesToExecute = null;
	private DependencyGroup currentDependencyGroup = null;
	private final DependencyCache dependencyCache;

	/**
	 * 
	 * @param description
	 * @param setRealValue Wert vom SmartHome-System (kein interner Wert)
	 * @param solvis
	 * @throws TypeException
	 */
	public CommandControl(final ChannelDescription description, final SingleData<?> setRealValue, final Solvis solvis)
			throws TypeException {
		this(description, solvis, null);
		this.setValue = description.interpretSetData(setRealValue, false);
		if (this.setValue == null) {
			logger.warn("Write value is null, writing ignored");
		}
		this.inhibitRead = description.inhibitGuiReadAfterWrite();
		this.write = true;
	}

	public CommandControl(final ChannelDescription description, final Solvis solvis) {
		this(description, solvis, null);
	}

	public CommandControl(final ChannelDescription description, final Solvis solvis, final Integer monitoringPriority) {
		this.setValue = null;
		this.inhibitRead = false;
		this.description = description;
		this.dependencyGroupsToExecute.add(description.getDependencyGroup().clone(), solvis);
		this.screen = description.getScreen(solvis);
		this.solvis = solvis;
		this.dependencyCache = new DependencyCache(solvis);
		this.monitoringPriority = monitoringPriority;
	}

	@Override
	public Handling handle(final Command queueEntry, final Solvis solvis) {
		if (!(queueEntry instanceof CommandControl)) {

			return new Handling( //
					false, // inQueueInhibited
					false, // inhibitAppend
					false // insert
			);
		}

		CommandControl queueCommand = (CommandControl) queueEntry;
		if (queueEntry.isInhibit() || queueCommand.mustBeFinished) {
			return new Handling( //
					false, // inQueueInhibited
					false, // inhibitAppend
					false // insert
			);
		} else {
			boolean inQueueInhibited = false;
			boolean inhibitAdd = false;
			boolean insert = false; // hat niedrigere Prio als inhibitAdd
			boolean mustFinished;
			synchronized (queueCommand) {

				mustFinished = queueCommand.mustBeFinished;

				if (queueCommand.isWriting()) {
					mustFinished = true;
				}

				if (queueCommand.getScreen(solvis) == this.getScreen(solvis)) {

					if (queueCommand.isWriting()) {

						if (this.description == queueCommand.description && this.isWriting()) {
							inQueueInhibited = true;
						}
						inhibitAdd = !this.isWriting() && this.monitoringPriority == null;
						insert = true;

					} else if (queueCommand.getRestoreChannel(solvis) != null) {

						if (this.description == queueCommand.description) {
							inQueueInhibited = true;
						}

					} else {
						inQueueInhibited = true;
					}

				}
				if (inQueueInhibited) {
					queueCommand.inhibit = true;
					this.dependencyGroupsToExecute.addAll(queueCommand.dependencyGroupsToExecute, solvis);
				}
			}
			return new Handling(inQueueInhibited, inhibitAdd, insert, mustFinished);
		}
	}

	@Override
	public boolean isInhibit() {
		return this.inhibit;
	}

	@Override
	protected void setInhibit(final boolean inhibit) {
		this.inhibit = inhibit;
	}

	@Override
	public AbstractScreen getScreen(final Solvis solvis) {
		return this.screen;
	}

	/**
	 * 
	 * @param solvis
	 * @param description
	 * @param data         value which should be written
	 * @param failCountRef Reference to the failCount
	 * @param overwrite    true, if the write value of the stored data should be
	 *                     overwritten after success
	 * @return ResultStatus
	 * @throws IOException
	 * @throws TerminationException
	 * @throws TypeException
	 * @throws SolvisErrorException
	 */

	private ResultStatus write(final Solvis solvis, final ChannelDescription description, final SingleData<?> setValue,
			final Reference<Integer> failCountRef, final boolean overwrite)
			throws IOException, TerminationException, TypeException, SolvisErrorException {
		SolvisData data = solvis.getAllSolvisData().get(description);
		SolvisData clone = data.duplicate();
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
			if (setResult.getStatus().isExecuted()) {
				if (overwrite) {
					data.setSingleData(setResult);
					this.setValue = null;
				}
				return ResultStatus.SUCCESS;
			} else {
				return setResult.getStatus();
			}
		}
	}

	@Override
	public ResultStatus preExecute(Solvis solvis, QueueStatus queueStatus)
			throws IOException, TerminationException, TypeException {

		if (queueStatus.getCurrentPriority() != null && this.isMonitoring()
				&& queueStatus.getCurrentPriority() > this.monitoringPriority) {
			return ResultStatus.INHIBITED;
		}

		if (this.setValue != null) {
			SolvisData data = solvis.getAllSolvisData().get(this.description);
			SolvisData clone = data.duplicate();
			clone.setSingleData(this.setValue);
			SetResult setResult = this.description.setValueFast(solvis, clone);
			if (setResult != null) {
				data.setSingleData(setResult);
				return setResult.getStatus();
			}
		}

		return null;
	}

	@Override
	public ResultStatus execute(final Solvis solvis, final Handling.QueueStatus queueStatus)
			throws IOException, PowerOnException, TerminationException, NumberFormatException, XmlException,
			TypeException, SolvisErrorException {

		boolean finished = false;
		while (!finished && !this.inhibit) {
			ResultStatus status = this.state.execute(this);
			switch (status) {
				case SUCCESS:
				case CONTINUE:
					this.state = this.state.next(this);
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
					this.state = this.state.next(this);
					break;
			}
			if (this.state == null || this.inhibit && !this.mustBeFinished) {
				finished = true;
			}
		}
		return this.finalStatus;
	}

	@Override
	public synchronized boolean toEndOfQueue() {
		int cmp = Constants.COMMAND_TO_QUEUE_END_AFTER_N_FAILURES;

		if (this.mustBeFinished()) {
			logger.error("Fatal error, dependencies couldn't restored");
			// TODO hier muss noch eine Smart-Home-Meldung generiert werden.
		}
		return this.writeFailCount.get() >= cmp || this.readFailCount >= cmp || this.dependencyFailCount.get() >= cmp;
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
			try {
				String out;
				SingleData<?> data = this.description.normalize(this.setValue);
				if (data == null) {
					out = "<normalize_null>";
				} else {
					out = data.toString();
					if (out == null) {
						out = "<data_null>";
					}
				}
				builder.append(out);
			} catch (TypeException e) {
				builder.append("Type exception <" + this.setValue + ">");
				e.printStackTrace();
			}
		}
		if (this.inhibit) {
			builder.append(", inhibited");
		}
		return builder.toString();
	}

	@Override
	public boolean isWriting() {
		return this.write && !this.inhibit;
	}

	@Override
	protected void notExecuted() {
	}

	@Override
	public boolean canBeIgnored(final Command queueCommand) {
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
	ChannelDescription getRestoreChannel(final Solvis solvis) {
		return this.description.getRestoreChannel(solvis);
	}

	/**
	 * SolvisStatus machine for execution of a command including restore and
	 * dependency processing
	 * 
	 * @author stefa_000
	 *
	 */

	private enum StateEnum {
		START(new Start()), //
		STANDBY_REQUEST(new StandbyRequest()), //
		EXECUTE_DEPENDENCY(new ExecuteDependency()), //
		WRITING(new Writing()), //
		READING(new Reading()), //
		RESTORE_DEPENDENCY(new RestoreDependency()), //
		FINISHED(new Finished());

		private final State state;

		private StateEnum(State state) {
			this.state = state;
			state.setStateEnum(this);
		}

		public State getState() {
			return this.state;
		}
	}

	private static abstract class State {

		private StateEnum stateEnum;

		public abstract State next(final CommandControl command);

		public abstract ResultStatus execute(final CommandControl command) throws IOException, TerminationException,
				NumberFormatException, PowerOnException, XmlException, TypeException, SolvisErrorException;

		@Override
		public String toString() {
			return this.stateEnum.name();
		}

		public void setStateEnum(final StateEnum stateEnum) {
			this.stateEnum = stateEnum;
		}
	}

	private static class Start extends State {

		@Override
		public State next(final CommandControl command) {
			return StateEnum.STANDBY_REQUEST.getState();
		}

		@Override
		public ResultStatus execute(final CommandControl command) {
			command.executionStartTime = System.currentTimeMillis();
			return ResultStatus.CONTINUE;
		}

	}

	private Iterator<Dependency> getNextDependencyIterator() throws IOException, TerminationException {

		Iterator<Dependency> itResult = null;

		AbstractScreen current = SolvisScreen.get(this.solvis.getCurrentScreen());

		for (ListIterator<Dependency> it = this.dependenciesToExecute.listIterator(); it.hasNext();) {
			if (itResult == null) {
				itResult = this.dependenciesToExecute.listIterator(it.nextIndex());
				;
			}
			Dependency dependency = it.next();
			if (dependency.getChannelDescription(this.solvis).getScreen(this.solvis) == current) {
				itResult = this.dependenciesToExecute.listIterator(it.previousIndex());
				break;
			}
		}

		return itResult;
	}

	private static class StandbyRequest extends State {

		@Override
		public State next(final CommandControl command) {
			return StateEnum.EXECUTE_DEPENDENCY.getState();
		}

		@Override
		public ResultStatus execute(final CommandControl command) throws IOException, TerminationException,
				NumberFormatException, PowerOnException, XmlException, TypeException, SolvisErrorException {

			Map<String, SingleData<?>> map = new HashMap<>(3);

			Set<String> standbyIds = new HashSet<>(3);

			for (Iterator<DependencyGroup> it = command.dependencyGroupsToExecute.iterator(); it.hasNext();) {
				DependencyGroup group = it.next();

				for (Dependency dependency : group.get()) {
					String standbyId = dependency.getStandbyId();
					if (standbyId != null) {
						SingleData<?> data = dependency.getData(command.solvis);
						SingleData<?> former = map.put(dependency.getId(), data);
						if (former != null && !former.equals(data)) {
							standbyIds.add(standbyId);
						}
					}

				}
			}

			for (String standbyId : standbyIds) {
				command.solvis.setStandby(standbyId);
			}

			return ResultStatus.CONTINUE;
		}
	}

	private static class ExecuteDependency extends State {

		@Override
		public State next(final CommandControl command) {
			if (command.dependenciesToExecute.isEmpty()) {
				command.dependenciesToExecute = null;
				return StateEnum.WRITING.getState();
			} else {
				return this;
			}
		}

		@Override
		public ResultStatus execute(final CommandControl command) throws IOException, TerminationException,
				NumberFormatException, PowerOnException, TypeException, SolvisErrorException {

			if (command.dependenciesToExecute == null) {
				command.currentDependencyGroup = command.dependencyGroupsToExecute.iterator().next();
				command.dependenciesToExecute = new ArrayList<>(command.currentDependencyGroup.get());
			}

			Iterator<Dependency> it = command.getNextDependencyIterator();
			if (it == null) {
				return ResultStatus.CONTINUE;
			}

			Solvis solvis = command.solvis;

			Dependency dependency = it.next();

			ChannelDescription dependencyChannel = dependency.getChannelDescription(solvis);
			SolvisData solvisData = solvis.getAllSolvisData().get(dependencyChannel);

			if (!solvisData.isValid()) {
				boolean success = dependencyChannel.getValue(solvisData, solvis, command.executionStartTime);
				if (!success) {
					return ResultStatus.NO_SUCCESS;
				}
			}

			SingleData<?> data = dependency.getData(solvis);
			SingleData<?> now = command.dependencyCache.get(dependencyChannel);

			if (data == null) {
				command.toRestore.put(dependencyChannel, dependency);
				data = solvisData.getSingleData();
				it.remove();
				return ResultStatus.SUCCESS;
			}

			ResultStatus status = ResultStatus.SUCCESS;

			if (!data.equals(now)) {

				synchronized (command) {
					if (command.inhibit) {
						return ResultStatus.SUCCESS;
					}
					command.mustBeFinished = true;
				}

				String standbyId = dependency.getStandbyId();

				if (standbyId != null) {
					command.mustBeFinished |= solvis.setStandby(standbyId);
				}

				command.toRestore.put(dependencyChannel, dependency);

				status = command.write(solvis, dependencyChannel, data, command.dependencyFailCount, false);

				command.dependencyCache.put(dependencyChannel, data);

				if (data.equals(solvisData.getSingleData())) {
					command.toRestore.remove(dependencyChannel);
				}

				if (status == ResultStatus.VALUE_VIOLATION) {
					logger.error("Wrong dependency value definition of channel <" + command.description.getId()
							+ ">. Check control.xml. Error ignored.");
					status = ResultStatus.SUCCESS;
				}
			}
			if (status == ResultStatus.SUCCESS) {
				it.remove();
			}

			return status;
		}

	}

	private static class Writing extends State {

		@Override
		public State next(final CommandControl command) {
			if (command.isInhibitRead()) {
				return StateEnum.RESTORE_DEPENDENCY.getState();
			} else {
				return StateEnum.READING.getState();
			}
		}

		@Override
		public ResultStatus execute(final CommandControl command)
				throws IOException, TerminationException, TypeException, SolvisErrorException {
			if (command.setValue == null) {
				return ResultStatus.CONTINUE;
			}

			Solvis solvis = command.solvis;

			ResultStatus status = command.write(solvis, command.description, command.setValue, command.writeFailCount,
					true);
			if (status.isExecuted()) {
				command.setValue = null;
			}
			return status;
		}

	}

	private static class Reading extends State {

		@Override
		public State next(final CommandControl command) {
			return StateEnum.RESTORE_DEPENDENCY.getState();
		}

		@Override
		public ResultStatus execute(final CommandControl command) throws NumberFormatException, IOException,
				PowerOnException, TerminationException, TypeException, SolvisErrorException {

			Solvis solvis = command.solvis;

			if (command.readChannels == null) {

				Collection<ChannelDescription> readChannels = solvis.getSolvisDescription().getChannelDescriptions()
						.getChannelDescriptions(command.screen, solvis);

				command.readChannels = new ArrayList<>();

				for (ChannelDescription description : readChannels) {
					if (!(command.isMonitoring() && description.isWriteable()
							&& description != command.getDescription())) {
						for (Iterator<DependencyGroup> it = command.dependencyGroupsToExecute.iterator(); it
								.hasNext();) {
							DependencyGroup group = it.next();
							if (DependencyGroup.equals(description.getDependencyGroup(), group, solvis)) {
								command.readChannels.add(description);
							}
						}
					}
				}
			}

			boolean readSuccess = true;

			for (Iterator<ChannelDescription> it = command.readChannels.iterator(); it.hasNext();) {
				ChannelDescription description = it.next();
				if (DependencyGroup.equals(description.getDependencyGroup(), command.currentDependencyGroup, solvis)
						&& !command.toRestore.containsKey(description)) {
					boolean success = description.getValue(solvis, command.executionStartTime);
					if (success) {
						it.remove();
					}
					readSuccess &= success;
				}
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

	private static class RestoreDependency extends State {

		@Override
		public State next(final CommandControl command) {

			if (!command.dependencyGroupsToExecute.isEmpty()) {
				return StateEnum.EXECUTE_DEPENDENCY.getState();
			} else if (!command.toRestore.isEmpty()) {
				return this;
			} else {
				return StateEnum.FINISHED.getState();
			}
		}

		@Override
		public ResultStatus execute(final CommandControl command) throws IOException, TerminationException,
				NumberFormatException, PowerOnException, TypeException, SolvisErrorException {

			if (command.currentDependencyGroup != null) {
				command.dependencyGroupsToExecute.remove(command.currentDependencyGroup);
			}

			if (!command.dependencyGroupsToExecute.isEmpty()) {
				return ResultStatus.CONTINUE;
			}

			if (command.toRestore.isEmpty()) {
				return ResultStatus.CONTINUE;
			}

			Solvis solvis = command.solvis;
			AbstractScreen current = SolvisScreen.get(solvis.getCurrentScreen());

			Iterator<Map.Entry<ChannelDescription, Dependency>> it;

			Map.Entry<ChannelDescription, Dependency> entry = null;
			ChannelDescription description = null;

			for (it = command.toRestore.entrySet().iterator(); it.hasNext();) {
				entry = it.next();
				description = entry.getKey();
				if (description.getScreen(solvis).equals(current)) {
					break;
				}
			}

			SingleData<?> data = solvis.getAllSolvisData().get(description).getSingleData();

			ResultStatus status = command.write(solvis, description, data, command.dependencyFailCount, false);
			command.dependencyCache.put(description, data);

			if (status == ResultStatus.SUCCESS) {
				command.toRestore.remove(description);
			}
			return status;
		}

	}

	private static class Finished extends State {

		@Override
		public State next(final CommandControl command) {
			return null;
		}

		@Override
		public ResultStatus execute(final CommandControl command) throws NumberFormatException, IOException,
				PowerOnException, TerminationException, TypeException, SolvisErrorException {
			command.solvis.resetStandby();
			return ResultStatus.SUCCESS;
		}

	}

	public boolean mustBeFinished() {
		return this.mustBeFinished;
	}

	public boolean isMonitoring() {
		return this.monitoringPriority != null;
	}

	private static class DependencyCache {

		private final Solvis solvis;
		private final Map<ChannelDescription, SingleData<?>> map = new HashMap<>();

		public DependencyCache(final Solvis solvis) {
			this.solvis = solvis;
		}

		public SingleData<?> get(final ChannelDescription description) {
			SingleData<?> result = this.map.get(description);
			if (result == null) {
				SolvisData solvisData = this.solvis.getAllSolvisData().get(description);
				result = solvisData.isValid() ? solvisData.getSingleData() : null;
			}
			return result;
		}

		public void put(final ChannelDescription description, final SingleData<?> data) {
			this.map.put(description, data);
		}

	}

	private class MySet {

		private List<DependencyGroup> list = new ArrayList<>();

		public boolean isEmpty() {
			return this.list.isEmpty();
		}

		public Iterator<DependencyGroup> iterator() {
			return this.list.iterator();
		}

		public boolean contains(final DependencyGroup group, final Solvis solvis) {
			for (DependencyGroup g : this.list) {
				if (DependencyGroup.equals(group, g, solvis)) {
					return true;
				}
			}
			return false;
		}

		public boolean add(final DependencyGroup e, final Solvis solvis) {
			if (this.contains(e, solvis)) {
				return false;
			} else {
				boolean added = false;
				for (ListIterator<DependencyGroup> it = this.list.listIterator(); it.hasNext();) {
					DependencyGroup entry = it.next();
					if (entry.get().size() < e.get().size()) {
						it.previous();
						it.add(e);
						added = true;
						break;
					}
				}
				if (!added) {
					this.list.add(e);
				}
				return true;
			}
		}

		public boolean addAll(final MySet c, final Solvis solvis) {
			boolean result = false;
			for (DependencyGroup group : c.list) {
				result |= this.add(group, solvis);
			}
			return result;
		}

		public void remove(final DependencyGroup toRemove) {
			for (Iterator<DependencyGroup> it = this.list.iterator(); it.hasNext();) {
				DependencyGroup group = it.next();
				if (group.equals(toRemove)) {
					it.remove();
					break;
				}
			}
		}

	}

	@Override
	public Type getType() {
		if (this.write) {
			return Type.CONTROL_WRITE;
		} else if (!this.isMonitoring()) {
			return Type.CONTROL_READ;
		} else {
			return Type.CONTROL_UPDATE;
		}
	}

	public boolean isInhibitRead() {
		return this.inhibitRead;
	}

}
