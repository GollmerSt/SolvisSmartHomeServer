/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.command;

import java.io.IOException;
import java.util.Collection;

import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.command.Handling.QueueStatus;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.xmllibrary.XmlException;

public abstract class Command {

	/**
	 * Executes the command
	 * 
	 * @param solvis
	 * @return true if successfull
	 * @throws IOException
	 * @throws TerminationException
	 * @throws PowerOnException
	 * @throws TypeException
	 * @throws NumberFormatException
	 * @throws XmlException
	 * @throws FieldException
	 * @throws ModbusException
	 */
	protected abstract void notExecuted();

	/**
	 * Get the necessary screen for executing
	 * 
	 * @param solvis
	 * @return screen
	 */
	public AbstractScreen getScreen(Solvis solvis) {
		return null;
	}

	/**
	 * 
	 * @return true if command is inhibited
	 */
	public boolean isInhibit() {
		return false;
	}

	/**
	 * Set the inhibit status
	 * 
	 * @param inhibit true if the command should be inhibited
	 */

	protected void setInhibit(boolean inhibit) {
	};

	public Handling handle(Command queueEntry, Solvis solvis) {
		return new Handling(false, false, false);
	}

	/**
	 * 
	 * @return true if the command should be at the first position of the queue
	 */
	public boolean first() {
		return false;
	}

	/**
	 * 
	 * @return true if the command should be moved to the end of the queue
	 */
	public boolean toEndOfQueue() {
		return false;
	}

	/**
	 * 
	 * @return true if the command is changed a solvis parameter
	 */
	protected boolean isWriting() {
		return false;
	}

	/**
	 * 
	 * @param command
	 * @return true both commands write to the same channel
	 */
	public boolean canBeIgnored(Command queueCommand) {
		return false;
	}

	ChannelDescription getRestoreChannel(Solvis solvis) {
		return null;
	}

	Collection<ChannelDescription> getReadChannels() {
		return null;
	}

	public boolean isDependencyPrepared() {
		return true;
	}

	public void setDependencyPrepared(boolean dependencyPrepared) {
	}

	public abstract ResultStatus execute(Solvis solvis, QueueStatus queueStatus) throws IOException,
			TerminationException, PowerOnException, NumberFormatException, TypeException, XmlException;
}
