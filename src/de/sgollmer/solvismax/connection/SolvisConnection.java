/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.PasswordAuthentication;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.transfer.ConnectionState;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.model.SolvisState;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.objects.Coordinate;

public class SolvisConnection extends Observer.Observable<ConnectionState> {

	private static final Logger logger = LogManager.getLogger(SolvisConnection.class);
	private static final Coordinate RELEASE_COORDINATE = new Coordinate(260, 260);

	private final String urlBase;
	private final AccountInfo accountInfo;
	private final int connectionTimeout;
	private final int readTimeout;
	private final int powerOffDetectedAfterIoErrors;
	private final int powerOffDetectedAfterTimeout_ms;
	private final boolean fwLth2_21_02A;
	private SolvisState solvisState;
	private Integer maxResponseTime = null;
	private int errorCount = 0;;
	private long firstTimeout = 0;

	private long connectTime = -1;
	private HttpURLConnection urlConnection = null;

	private ConnectionState connectionState = new ConnectionState(ConnectionStatus.CONNECTION_NOT_POSSIBLE);

	public SolvisConnection(String urlBase, AccountInfo accountInfo, int connectionTimeout, int readTimeout,
			int powerOffDetectedAfterIoErrors, int powerOffDetectedAfterTimeout_ms, boolean fwLth2_21_02A) {
		this.urlBase = urlBase;
		this.accountInfo = accountInfo;
		this.connectionTimeout = connectionTimeout;
		this.readTimeout = readTimeout;
		this.powerOffDetectedAfterIoErrors = powerOffDetectedAfterIoErrors;
		this.powerOffDetectedAfterTimeout_ms = powerOffDetectedAfterTimeout_ms;
		this.solvisState = null;
		this.fwLth2_21_02A = fwLth2_21_02A;

	}

	private class MyAuthenticator extends Authenticator {
		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			// System.out.println( "Hier erfolgt die Authentifizierung" ) ;
			// System.out.printf( "url=%s, host=%s, ip=%s, port=%s%n",
			// getRequestingURL(), getRequestingHost(),
			// getRequestingSite(), getRequestingPort() );

			return new PasswordAuthentication(accountInfo.getAccount(), accountInfo.createPassword());
		}
	}

	public InputStream connect(String suffix) throws IOException {
		try {
			this.connectTime = System.currentTimeMillis();
			Authenticator.setDefault(new MyAuthenticator());
			URL url = new URL(this.urlBase + suffix);
			synchronized (this) {
				urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setConnectTimeout(this.connectionTimeout);
				urlConnection.setReadTimeout(this.readTimeout);
//				System.out.println(
//						"Connect-Timeout: " + uc.getConnectTimeout() + ", Read-Timeout: " + uc.getReadTimeout());
				InputStream in = urlConnection.getInputStream();
				this.setConnected();
				return in;
			}
		} catch (ConnectException | SocketTimeoutException | NoRouteToHostException e) {
			this.handleExceptionAndThrow(e);
			return null; // dummy
		}
	}

	public BufferedImage getScreen() throws IOException {
		BufferedImage image;
		try {
			synchronized (this) {
				InputStream in = this.connect("/display.bmp?");
				image = ImageIO.read(in);
				in.close();
			}
		} catch (IOException e) {
			this.handleExceptionAndThrow(e);
			throw e;
		}
		this.calculateMaxResponseTime();
		return image;
	}

	public static class SolvisMeasurements {
		private final long timeStamp;
		private final String hexString;

		public SolvisMeasurements(long timeStamp, String hexString) {
			this.timeStamp = timeStamp;
			this.hexString = hexString;
		}

		public long getTimeStamp() {
			return timeStamp;
		}

		public String getHexString() {
			return hexString;
		}

	}

	public SolvisMeasurements getMeasurements() throws IOException {
		StringBuilder builder = new StringBuilder();
		long timeStamp = 0;
		try {
			synchronized (this) {
				InputStream in = this.connect("/sc2_val.xml");
				InputStreamReader reader = new InputStreamReader(in);

				timeStamp = System.currentTimeMillis();

				boolean finished = false;
				int length = 1024;
				char[] array = new char[length];
				while (!finished) {
					int n = reader.read(array, 0, 1000);
					if (n < 0) {
						finished = true;
					} else {
						builder.append(array, 0, n);
						if (builder.substring(builder.length() - 5).equals("</xml>")) {
							finished = true;
						}
					}
				}
				in.close();
			}
			builder.delete(0, 11);
			builder.delete(builder.length() - 15, builder.length());

		} catch (IOException e) {
			handleExceptionAndThrow(e);
			throw e;
		}
		this.calculateMaxResponseTime();
		String hexString = builder.toString();
		// logger.debug("Hex string received from solvis: " + hexString);
		return new SolvisMeasurements(timeStamp, hexString);
	}

	public enum Button {
		BACK("links"), INFO("rechts");

		private final String buttonUrl;

		private Button(String button) {
			this.buttonUrl = button;
		}

		public String getButtonUrl() {
			return buttonUrl;
		}
	}

	public void sendButton(Button button) throws IOException {
		String buttonString = "/Taster.CGI?taste=" + button.getButtonUrl() + "&i="
				+ Math.round((Math.random() * 99999999));
		try {
			synchronized (this) {
				InputStream in = this.connect(buttonString);
				in.close();
			}
		} catch (IOException e) {
			this.handleExceptionAndThrow(e);
		}
		this.calculateMaxResponseTime();
	}

	public void sendTouch(Coordinate coord) throws IOException {
		int x = coord.getX() * 2;
		int y = coord.getY() * 2;
		String touchString = "/Touch.CGI?x=" + x + "&y=" + y;
		InputStream in = null;
		synchronized (this) {
			try {
				in = this.connect(touchString);
				in.close();
			} catch (IOException e) {
				if (this.fwLth2_21_02A) {
					if (this.urlConnection != null) {
						try {
							this.urlConnection.disconnect();
						} catch (Throwable e1) {
						}
					}
				} else {
					this.handleExceptionAndThrow(e);
				}
			}
		}
		this.calculateMaxResponseTime();
	}

	public void sendRelease() throws IOException {
		this.sendTouch(RELEASE_COORDINATE);
	}

	public ConnectionState getConnectionState() {
		return connectionState;
	}

	public void setConnectionState(ConnectionState connectionState) {
		this.connectionState = connectionState;
		this.notify(connectionState);
	}

	private void setConnected() {
		this.errorCount = 0;
		this.firstTimeout = 0;

		this.solvisState.connected();

		switch (this.solvisState.getState()) {
			case POWER_OFF:
			case REMOTE_CONNECTED:
			case SOLVIS_DISCONNECTED:
				logger.info("Connection to solvis <" + this.urlBase + "> successfull.");
				break;
		}
	}

	private void handleExceptionAndThrow(Throwable e) throws IOException {
		if (this.urlConnection != null) {
			try {
				this.urlConnection.disconnect();
			} catch (Throwable e1) {
			}
		}
		++this.errorCount;
		if (this.firstTimeout == 0) {
			this.firstTimeout = System.currentTimeMillis();
		}
		if (this.errorCount >= this.powerOffDetectedAfterIoErrors
				&& System.currentTimeMillis() > this.firstTimeout + this.powerOffDetectedAfterTimeout_ms) {
			this.solvisState.powerOff();
		} else {
			this.solvisState.disconnected();
			;
		}
		AbortHelper.getInstance().sleep(Constants.WAIT_TIME_AFTER_IO_ERROR);
		throw new IOException(e.getMessage());
	}

	private void calculateMaxResponseTime() {

		int connectionTime = (int) (System.currentTimeMillis() - connectTime);
		if (this.maxResponseTime == null || connectionTime < this.maxResponseTime * 2) {
			this.maxResponseTime = connectionTime;
		}
	}

	public int getMaxResponseTime() {
		if (this.maxResponseTime == null) {
			return 0;
		} else {
			return maxResponseTime;
		}
	}

	public void setSolvisState(SolvisState solvisState) {
		this.solvisState = solvisState;
	}

}
