/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.error.JsonError;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.model.SolvisWorkers;

public class Helper {

	private static final Logger logger = LogManager.getLogger(SolvisWorkers.class);

	public static char charAt(String json, int position) throws JsonError {
		try {
			return json.charAt(position);
		} catch (IndexOutOfBoundsException e) {
			throw new JsonError("Unexpected end of json string reached");
		}
	}

	public static void read(InputStream in, byte[] bytes, int timeout) throws IOException {
		int transfered = 0;
		Runnable runnable = null;
		while (transfered < bytes.length) {
			int cnt = in.read(bytes, transfered, bytes.length - transfered);
			if (cnt < 0) {
				throw new IOException("Not enough bytes received (transfered: " + transfered + ", target: "
						+ bytes.length + "). Connection closed?");
			}
			transfered += cnt;
			if (transfered < bytes.length && timeout > 0) {
				runnable = new Runnable(in, timeout);
				runnable.submit();
			}
		}
		if (runnable != null) {
			runnable.abort();
		}
	}

	private static class Runnable extends de.sgollmer.solvismax.helper.Helper.Runnable {

		private final int timeout;
		private final InputStream inputStream;
		private boolean abort = false;

		public Runnable(InputStream stream, int timeout) {
			super("ReadTimeout");
			this.timeout = timeout;
			this.inputStream = stream;
		}

		@Override
		public void run() {
			synchronized (this) {
				AbortHelper.getInstance().sleepAndLock(this.timeout, this);
				if (!this.abort) {
					try {
						this.inputStream.close();
						logger.error("Timeout while client to server transfer");
					} catch (IOException e) {
					}
				}
			}
		}

		public synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}

	}

}
