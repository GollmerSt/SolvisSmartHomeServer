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

	public enum Type {
		CONTROL_READ, CONTROL_WRITE, CONTROL_UPDATE, OTHER
	}

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
	public AbstractScreen getScreen(final Solvis solvis) {
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

	protected void setInhibit(final boolean inhibit) {
	};

	public Handling handle(final Command queueEntry, final Solvis solvis) {
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
	public boolean isWriting() {
		return false;
	}

	/**
	 * 
	 * @param command
	 * @return true if both commands write to the same channel or the command read a
	 *         channel, which will be written
	 */
	public boolean canBeIgnored(final Command queueCommand) {
		return false;
	}

	ChannelDescription getRestoreChannel(final Solvis solvis) {
		return null;
	}

	Collection<ChannelDescription> getReadChannels() {
		return null;
	}

	public boolean isDependencyPrepared() {
		return true;
	}

	public void setDependencyPrepared(final boolean dependencyPrepared) {
	}

	public abstract ResultStatus execute(final Solvis solvis, final QueueStatus queueStatus) throws IOException,
			TerminationException, PowerOnException, NumberFormatException, TypeException, XmlException;

	public Type getType() {
		return Type.OTHER;
	}

	public ResultStatus preExecute(Solvis solvis, QueueStatus queueStatus)
			throws IOException, TerminationException, TypeException {
		return null;
	}
}
