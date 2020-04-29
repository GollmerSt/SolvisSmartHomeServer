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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public class CommandControl extends Command {

	private static final Logger logger = LogManager.getLogger(CommandControl.class);

	private final Collection<SubCommand> subCommands = new ArrayList<>();
	private final Screen screen;
	private boolean writing;
	private final boolean modbus;
	private boolean inhibit = false;
	private boolean toEndOfQueue = false;

	private static class SubCommand extends Command {
		private final ChannelDescription description;
		private final SingleData<?> setValue;
		private int failCount = 0;

		public SubCommand(ChannelDescription description, SingleData<?> setValue) {
			this.description = description;
			this.setValue = setValue;
		}

		@Override
		public boolean execute(Solvis solvis) throws IOException, TerminationException, ErrorPowerOn {
			boolean success = false;
			SolvisData data = solvis.getAllSolvisData().get(this.description);
			if (this.setValue == null) {
				success = this.description.getValue(solvis, solvis.getTimeAfterLastSwitchingOn());
			} else {
				SolvisData clone = data.clone();
				clone.setSingleData(this.setValue);
				SingleData<?> setData = this.description.setValue(solvis, clone);
				if (setData != null) {
					data.setSingleData(setData);
					success = true;
				}
			}
			if (!success) {
				++this.failCount;
			}
			return success;
		}

		public boolean toRemove() {
			return this.failCount >= Constants.COMMAND_IGNORED_AFTER_N_FAILURES;
		}

		@Override
		public boolean toEndOfQueue() {
			return this.failCount >= Constants.COMMAND_TO_QUEUE_END_AFTER_N_FAILURES;
		}

		@Override
		public void notExecuted() {
		}

		@Override
		public boolean isWriting() {
			return this.setValue != null;
		}

		@Override
		public String toString() {
			String out = "Id: " + this.description.getId();
			if (this.setValue != null) {
				out += ", set value: " + this.setValue.toString();
			}
			return out;
		}
	}

	public CommandControl(ChannelDescription description, SingleData<?> setValue, Solvis solvis) {
		this.subCommands.add(new SubCommand(description, setValue));
		this.writing = setValue != null;
		this.modbus = description.isModbus(solvis);
		this.screen = description.getScreen(solvis.getConfigurationMask());
		if (!this.modbus) {
			for (ChannelDescription channelDescription : solvis.getSolvisDescription().getChannelDescriptions()
					.getChannelDescriptions(this.screen, solvis)) {
				if (!channelDescription.isModbus(solvis) && channelDescription != description) {
					this.subCommands.add(new SubCommand(channelDescription, null));
				}
			}
		}
	}

	public CommandControl(ChannelDescription description, Solvis solvis) {
		this(description, null, solvis);
	}

	@Override
	public boolean isInhibit() {
		return this.inhibit;
	}

	@Override
	public void setInhibit(boolean inhibit) {
		this.inhibit = inhibit;
	}

	@Override
	public Screen getScreen(Solvis solvis) {
		return this.screen;
	}

	@Override
	public boolean execute(Solvis solvis) throws IOException, ErrorPowerOn {

		boolean success = true;
		boolean written = false;
		this.toEndOfQueue = false;

		for (Iterator<SubCommand> it = this.subCommands.iterator(); it.hasNext();) {
			SubCommand subCommand = it.next();
			if (written) {
				AbortHelper.getInstance().sleep(1000);
				solvis.clearCurrentScreen();
			}
			boolean successI = subCommand.execute(solvis);
			synchronized (this) {
				if (successI) {
					it.remove();
					written = subCommand.isWriting();
					if (written) {
						this.writing = false;
					}
				} else {
					written = false;
					if (subCommand.toRemove()) {
						it.remove();
						logger.error("Command <" + subCommand.toString() + "> couldn't executed. Is ignored.");
					} else {
						this.toEndOfQueue |= subCommand.toEndOfQueue();
					}
				}
			}
		}
		return success;
	}

	@Override
	public synchronized boolean toEndOfQueue() {
		return this.toEndOfQueue;
	}

	@Override
	public Handling getHandling(Command queueEntry, Solvis solvis) {
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
				ChannelDescription description = this.subCommands.iterator().next().description;
				ChannelDescription cmpDescription = null;
				for (SubCommand subCommand : qCmp.subCommands) {
					if (description.equals(subCommand.description)) {
						cmpDescription = subCommand.description;
					}
				}
				boolean sameScreen = qCmp.getScreen(solvis) == this.getScreen(solvis);

				if (cmpDescription == null && sameScreen) {
					insert = true;
				} else if (cmpDescription != null) {
					inQueueInhibit = this.isWriting() || !queueEntry.isWriting();
					inhibitAdd = queueEntry.isWriting() && !this.isWriting();
					insert = queueEntry.isWriting();
				}
				return new Handling(inQueueInhibit, inhibitAdd, insert, finished);
			}
		}

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		String prefix = "";
		if (this.subCommands.size() > 1) {
			prefix = "    ";
			builder.append("\n  SubCommands: \n");
		}
		boolean first = true;
		for (SubCommand subCommand : this.subCommands) {
			if (first) {
				first = false;
			} else {
				builder.append('\n');
			}
			builder.append(prefix);
			builder.append(subCommand.toString());
		}
		if (this.subCommands.size() > 1) {
			builder.append('\n');
		}
		return builder.toString();
	}

	@Override
	public boolean isModbus() {
		return this.modbus;
	}

	@Override
	public boolean isWriting() {
		return this.writing;
	}

	@Override
	public void notExecuted() {
	}
}
