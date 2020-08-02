/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.ModbusException;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;

public class CommandControl extends Command {

	private static final ILogger logger = LogManager.getInstance().getLogger(CommandControl.class);

	private final ChannelDescription description;
	private final SingleData<?> setValue;
	private final AbstractScreen screen;
	private final boolean modbus;
	private boolean inhibit = false;
	private int writeFailCount = 0;
	private int readFailCount = 0;

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

	private boolean save(Solvis solvis) throws IOException, PowerOnException, TerminationException, ModbusException {
		ChannelDescription restoreChannel = this.description.getRestoreChannel(solvis);
		if (restoreChannel == null || this.isModbus()) {
			return true;
		}
		boolean success = false;
		CommandControl save = new CommandControl(restoreChannel, solvis);
		for (int cnt = 0; !success && cnt < Constants.COMMAND_IGNORED_AFTER_N_FAILURES; ++cnt) {
			success = save.execute(solvis);
		}
		return success;
	}

	private boolean restore(Solvis solvis) throws IOException, PowerOnException, TerminationException, ModbusException {
		ChannelDescription restoreChannel = this.description.getRestoreChannel(solvis);
		if (restoreChannel == null || this.isModbus()) {
			return true;
		}
		boolean success = false;
		SolvisData data = solvis.getAllSolvisData().get(restoreChannel);
		CommandControl restore = new CommandControl(restoreChannel, data.getSingleData(), solvis);
		for (int cnt = 0; !success && cnt < Constants.COMMAND_IGNORED_AFTER_N_FAILURES; ++cnt) {
			success = restore.execute(solvis);
		}
		return success;
	}

	private boolean write(Solvis solvis) throws IOException, TerminationException, ModbusException {
		SolvisData data = solvis.getAllSolvisData().get(this.description);
		SolvisData clone = data.clone();
		clone.setSingleData(this.setValue);
		SingleData<?> setData = this.description.setValue(solvis, clone);
		if (setData != null) {
			data.setSingleData(setData);
		} else {
			++this.writeFailCount;
			if (this.writeFailCount < Constants.COMMAND_IGNORED_AFTER_N_FAILURES) {
				return false;
			}
			logger.error("Set of channel <" + this.description.getId() + "> not successfull. Aborted.");
		}
		return true;
	}

	@Override
	public boolean execute(Solvis solvis) throws IOException, PowerOnException, TerminationException, ModbusException {

		int maxFailCnt = Constants.COMMAND_IGNORED_AFTER_N_FAILURES;

		if (!this.save(solvis)) {
			this.writeFailCount = maxFailCnt;
			logger.error("Save of channel <" + this.description.getRestoreChannel(solvis).getId()
					+ "> not successfull. Aborted.");
			return false;
		}

		boolean success = true;

		if (this.isWriting() && this.writeFailCount < maxFailCnt) {
			success = write(solvis);
		}

		if (success) {

			if (this.modbus) {
				success = this.description.getValue(solvis);

			} else {
				for (ChannelDescription description : solvis.getSolvisDescription().getChannelDescriptions()
						.getChannelDescriptions(this.screen, solvis)) {
					success |= description.getValue(solvis);
				}
			}
			if (!success) {
				++this.readFailCount;
				if (this.readFailCount >= maxFailCnt) {
					logger.error("Get channels of screen <" + this.screen.getId() + "> not successfull. Aborted.");
					success = true;
				}
			}
		}
		if (!this.restore(solvis)) {
			this.writeFailCount = maxFailCnt;
			logger.error("Restore of channel <" + this.description.getRestoreChannel(solvis).getId()
					+ "> not successfull. Aborted.");
			success = false;
		}

		return success;
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
}
