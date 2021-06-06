package de.sgollmer.solvismax.model.command;

import java.util.ArrayList;
import java.util.Collection;

import de.sgollmer.solvismax.model.SolvisStatus;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;

public class CommandObserver extends Observable<SolvisStatus> implements IObserver<CommandObserver.Status> {

	private Collection<Object> monitoringTasks = new ArrayList<>(3);
	private boolean queue = false;

	public enum Status {
		QUEUE_READ(SolvisStatus.CONTROL_READ_ONGOING), //
		QUEUE_WRITE(SolvisStatus.CONTROL_WRITE_ONGOING), //
		QUEUE_FINISHED(null), //
		MONITORING_STARTED(null), //
		MONITORING_FINISHED(null);

		private final SolvisStatus solvisStatus;

		private Status(SolvisStatus status) {
			this.solvisStatus = status;
		}

	}

	@Override
	public void update(final Status status, final Object source) {

		switch (status) {
			case QUEUE_WRITE:
			case QUEUE_READ:
				this.queue = true;
				this.notify(status.solvisStatus);
				break;
			case QUEUE_FINISHED:
				this.queue = false;
				if ( this.monitoringTasks.isEmpty() ) {
					this.notify(SolvisStatus.CONTROL_FINISHED);
				} else {
					this.notify(SolvisStatus.CONTROL_MONITORING);
				}
				break;
			case MONITORING_STARTED:
				this.monitoringTasks.add(source);
				if ( !this.queue ) {
					this.notify(SolvisStatus.CONTROL_MONITORING);
				}
				break;
			case MONITORING_FINISHED:
				this.monitoringTasks.remove(source);
				if ( !this.queue && this.monitoringTasks.isEmpty()) {
					this.notify(SolvisStatus.CONTROL_FINISHED);
				}
					
			

		}
	}
	
}
