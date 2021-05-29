package de.sgollmer.solvismax.model.command;

import java.io.IOException;

import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ResultStatus;
import de.sgollmer.xmllibrary.XmlException;

public class CommandSetQueuePriority extends Command {

	private final Integer priority;
	private final Object setObject;

	public CommandSetQueuePriority(final Integer priority, final Object setObject) {
		this.priority = priority;
		this.setObject = setObject;
	}

	@Override
	public ResultStatus execute(final Solvis solvis, final Handling.QueueStatus queueStatus) throws IOException,
			TerminationException, PowerOnException, NumberFormatException, TypeException, XmlException {

		queueStatus.setCurrentPriority(this.priority, this.setObject);

		return ResultStatus.SUCCESS;
	}

	@Override
	protected void notExecuted() {

	}

}
