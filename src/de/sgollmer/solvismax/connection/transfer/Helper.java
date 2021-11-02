/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.io.IOException;
import java.io.InputStream;

import de.sgollmer.solvismax.error.ConnectionClosedException;
import de.sgollmer.solvismax.error.JsonException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.AbortHelper.Abortable;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.SolvisWorkers;

public class Helper {

	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisWorkers.class);

	static char charAt(final String json, final int position) throws JsonException {
		try {
			return json.charAt(position);
		} catch (IndexOutOfBoundsException e) {
			throw new JsonException("Unexpected end of json string reached");
		}
	}

	static void read(final InputStream in, final byte[] bytes, final int timeout) throws IOException {
		int transfered = 0;
		TimeoutThread runnable = null;
		while (transfered < bytes.length) {

			int cnt;
			try {
				cnt = in.read(bytes, transfered, bytes.length - transfered);
			} catch (IOException e) {
				if (AbortHelper.getInstance().isAbort()) {
					return;
				} else {
					throw e;
				}
			}
			if (cnt < 0) {
				if (transfered > 0) {
					throw new IOException("Not enough bytes received (transfered: " + transfered + ", target: "
							+ bytes.length + "). Connection closed?");
				} else {
					throw new ConnectionClosedException("Connection closed by client.");
				}
			}
			transfered += cnt;
			if (transfered < bytes.length && timeout > 0) {
				runnable = new TimeoutThread(in, timeout);
				runnable.submit();
			}
		}
		if (runnable != null) {
			runnable.abort();
		}
	}

	private static class TimeoutThread extends de.sgollmer.solvismax.helper.Helper.Runnable implements Abortable {

		private final int timeout;
		private final InputStream inputStream;
		private boolean abort = false;

		private TimeoutThread(final InputStream stream, final int timeout) {
			super("ReadTimeout");
			this.timeout = timeout;
			this.inputStream = stream;
		}

		@Override
		public void run() {
			synchronized (this) {
				try {
					AbortHelper.getInstance().sleepAndLock(this.timeout, this);
					if (!this.abort) {
						try {
							this.inputStream.close();
							logger.error("Timeout while client to server transfer");
						} catch (IOException e) {
						}
					}
				} catch (TerminationException e1) {
				}
			}
		}

		@Override
		public synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}

	}

}
