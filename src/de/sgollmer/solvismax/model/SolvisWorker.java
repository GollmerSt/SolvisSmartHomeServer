package de.sgollmer.solvismax.model;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class SolvisWorker {

	private Queue<Command> queue = new ArrayDeque<>();
	private final Solvis solvis;

	public SolvisWorker(Solvis solvis) {
		this.solvis = solvis;
	}

	public class WorkerThread implements Runnable {

		private boolean terminate = false;

		@Override
		public void run() {
			boolean queueWasEmpty = false;
			int screenRestoreInhibitCnt = 0;
			boolean saveScreen = false;
			boolean restoreScreen = false;
			boolean executeWatchDog = false;
			while (!this.terminate) {
				Command command = null;
				synchronized (SolvisWorker.this.queue) {
					if (SolvisWorker.this.queue.isEmpty()) {
						if (!queueWasEmpty && screenRestoreInhibitCnt == 0) {
							restoreScreen = true;
							queueWasEmpty = true;
						} else {
							try {
								SolvisWorker.this.queue.wait(solvis.getDuration("WatchDogTime").getTime_ms());
							} catch (InterruptedException e) {
							}
							if (SolvisWorker.this.queue.isEmpty()) {
								executeWatchDog = true;
								screenRestoreInhibitCnt = 0;
							}
						}

					} else {
						command = SolvisWorker.this.queue.peek();
						if (command.isScreenRestoreOff()) {
							++screenRestoreInhibitCnt;
						} else if (command.isScreenRestoreOn()) {
							++screenRestoreInhibitCnt;
						}
						if (queueWasEmpty && screenRestoreInhibitCnt == 0) {
							saveScreen = true;
						}
					}
				}
				if (this.terminate) {
					return;
				}
				if (saveScreen) {
					SolvisWorker.this.solvis.saveScreen();
					saveScreen = false;
				} else if (restoreScreen) {
					SolvisWorker.this.solvis.restoreScreen();
					restoreScreen = false;
				}
				if (executeWatchDog) {
					SolvisWorker.this.solvis.getWatchDog().execute();
					executeWatchDog = false;
				}
				if (command != null && command.getDescription() != null) {
					SolvisWorker.this.solvis.getWatchDog().clear(); 
					// SolvisData solvisData = solvis.ge;
					SolvisData data = solvis.getAllSolvisData().get(command.getDescription());
					boolean success;
					if (command.getSetValue() == null) {
						success = command.getDescription().getValue(data, solvis);
					} else {
						SolvisData clone = data.clone();
						clone.setSingleData(command.getSetValue());
						success = command.getDescription().setValue(solvis, clone);
						if (success) {
							data.setSingleData(command.getSetValue());
						}
					}
					if (success) {
						queue.remove();
					} else {
						synchronized (SolvisWorker.this.queue) {
							try {
								SolvisWorker.this.queue.wait(solvis.getDuration("UnsuccessfullWaitTime").getTime_ms());
							} catch (InterruptedException e) {
							}
						}
					}
				}
			}
		}
	}

	public void push(Command command) {
		synchronized (this.queue) {
			for ( Iterator< Command > it = this.queue.iterator() ; it.hasNext();) {
				Command cmp = it.next() ;
				if ( command.getDescription() == cmp.getDescription() ) {
					it.remove();
				}
			}
			this.queue.add(command);
			this.queue.notifyAll();
		}
	}
}
