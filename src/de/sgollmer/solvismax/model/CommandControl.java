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
import java.util.Iterator;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.ModbusException;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.Status;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;

public class CommandControl extends Command {

	private static final ILogger logger = LogManager.getInstance().getLogger(CommandControl.class);

	private final ChannelDescription description;
	private SingleData<?> setValue;
	private final AbstractScreen screen;
	private final boolean modbus;
	private boolean inhibit = false;
	private int writeFailCount = 0;
	private int readFailCount = 0;
	private Collection<ChannelDescription> readChannels = null;

	CommandControl(ChannelDescription description, SingleData<?> setValue, Solvis solvis) {
		this.modbus = description.isModbus(solvis);
		this.setValue = setValue;
		this.description = description;
		if (this.modbus) {
			this.screen = null;
		} else {
			this.screen = description.getScreen(solvis.getConfigurationMask());
		}
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

	private Status write(Solvis solvis) throws IOException, TerminationException, ModbusException {
		SolvisData data = solvis.getAllSolvisData().get(this.description);
		SolvisData clone = data.clone();
		clone.setSingleData(this.setValue);
		SetResult setResult = this.description.setValue(solvis, clone);
		if (setResult == null) {
			++this.writeFailCount;
			if (this.writeFailCount < Constants.COMMAND_IGNORED_AFTER_N_FAILURES) {
				return Status.NO_SUCCESS;
			}
			logger.error("Set of channel <" + this.description.getId() + "> not successfull. Aborted.");
			return Status.SUCCESS;
		} else {
			switch (setResult.getStatus()) {
				case SUCCESS:
				case VALUE_VIOLATION:
					data.setSingleData(setResult);
					this.setValue = null;
					return Status.SUCCESS;
				case NO_SUCCESS:
					return Status.SUCCESS; // Scanned field not available, ignored.
				case INTERRUPTED:
					return Status.INTERRUPTED;
				default:
					return Status.UNKNOWN;
			}
		}
	}

	@Override
	public Status execute(Solvis solvis) throws IOException, PowerOnException, TerminationException, ModbusException, NumberFormatException {

		int maxFailCnt = Constants.COMMAND_IGNORED_AFTER_N_FAILURES;

		Status writeStatus = Status.SUCCESS;

		if (this.isWriting() && this.writeFailCount < maxFailCnt) {
			writeStatus = write(solvis);
		}

		boolean readSuccess = true;

		if (writeStatus != Status.NO_SUCCESS) {

			if (this.modbus) {
				readSuccess = this.description.getValue(solvis);

			} else {
				Collection<ChannelDescription> readChannels = solvis.getSolvisDescription().getChannelDescriptions()
						.getChannelDescriptions(this.screen, solvis);

				if (this.isWriting()) {
					this.readChannels = new ArrayList<>(readChannels.size() + 1);
					this.readChannels.add(this.description);
				} else {
					this.readChannels = new ArrayList<>(readChannels.size());
				}
				for (Iterator<ChannelDescription> it = readChannels.iterator(); it.hasNext();) {
					ChannelDescription description = it.next();
					boolean success = description.getValue(solvis);
					if (success) {
						this.readChannels.add(this.description);
						it.remove();
					}
					readSuccess &= success;
				}
			}
			if (!readSuccess) {
				++this.readFailCount;
				if (this.readFailCount >= maxFailCnt) {
					logger.error("Get channels of screen <" + this.screen.getId() + "> not successfull. Aborted.");
					readSuccess = true;
				}
			}
		}

		if ((writeStatus == Status.SUCCESS) && !readSuccess) {
			return Status.NO_SUCCESS;
		} else {
			return writeStatus;
		}
	}

	@Override
	protected synchronized boolean toEndOfQueue() {
		int cmp = Constants.COMMAND_TO_QUEUE_END_AFTER_N_FAILURES;
		return this.writeFailCount >= cmp || this.readFailCount >= cmp;
	}

	@Override
	protected Handling getHandling(Command queueEntry, Solvis solvis) {
		if (!(queueEntry instanceof CommandControl) || queueEntry.isInhibit()) {
			// new Handling(inQueueInhibit, inhibitAdd, insert)
			return new Handling(false, false, false);
		} else {
			boolean inQueueInhibit = false;
			boolean inhibitAdd = false;
			boolean insert = false; // hat niedrigere Prio als inhibitAdd
			boolean finished = false;
			synchronized (this) {

				if (queueEntry.isWriting()) {
					finished = true;
				}
				CommandControl qCmp = (CommandControl) queueEntry;
				boolean sameScreen = qCmp.getScreen(solvis) == this.getScreen(solvis);

				if (sameScreen) {

					if (queueEntry.isWriting()) {

						if (this.description == qCmp.description && this.isWriting()) {
							inQueueInhibit = true;
						}
						inhibitAdd = !this.isWriting();
						insert = true;

					} else if (queueEntry.getRestoreChannel(solvis) != null) {

						if (this.description == qCmp.description) {
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
	protected boolean isModbus() {
		return this.modbus;
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
		if (this.modbus) {
			return !this.isInhibit() && this.description == control.description
					&& (!this.isWriting() || control.isWriting());
		}
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

}
