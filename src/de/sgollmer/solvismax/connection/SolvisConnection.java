package de.sgollmer.solvismax.connection;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.PasswordAuthentication;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Constants.Solvis;
import de.sgollmer.solvismax.connection.transfer.ConnectionState;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.SolvisState;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.objects.Coordinate;

public class SolvisConnection extends Observer.Observable<ConnectionState> {

	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisConnection.class);

	private final Collection<UrlBase> urlBases;
	private UrlBase urlBase = null;
	private int urlBaseFailedCnt = 0;

	private final IAccountInfo accountInfo;
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

	public SolvisConnection(final Collection<String> urlBases, final String urlBase, final IAccountInfo accountInfo,
			final int connectionTimeout, final int readTimeout, final int powerOffDetectedAfterIoErrors,
			final int powerOffDetectedAfterTimeout_ms, final boolean fwLth2_21_02A) {

		this.urlBases = new ArrayList<>();
		if (urlBases != null) {
			for (String urlO : urlBases) {
				for (String urlI : Helper.getNetworkUrls(urlO)) {
					this.urlBases.add(new UrlBase(urlI));
				}
			}
			if (urlBase != null) {
				logger.warn("base.xml error, only one of urlBases or urlbase should be defined! urlbase ignored.");
			}
		} else {
			this.urlBases.add(new UrlBase(urlBase));
		}

		this.accountInfo = accountInfo;
		this.connectionTimeout = connectionTimeout;
		this.readTimeout = readTimeout;
		this.powerOffDetectedAfterIoErrors = powerOffDetectedAfterIoErrors;
		this.powerOffDetectedAfterTimeout_ms = powerOffDetectedAfterTimeout_ms;
		this.solvisState = null;
		this.fwLth2_21_02A = fwLth2_21_02A;

		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

	}

	private class MyAuthenticator extends Authenticator {

		private PasswordAuthentication passwordAuthentication = null;

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			// System.out.println( "Hier erfolgt die Authentifizierung" ) ;
			// System.out.printf( "url=%s, host=%s, ip=%s, port=%s%n",
			// getRequestingURL(), getRequestingHost(),
			// getRequestingSite(), getRequestingPort() );

			if (this.passwordAuthentication == null) {

				char[] p = SolvisConnection.this.accountInfo.cP();

				this.passwordAuthentication = new PasswordAuthentication(SolvisConnection.this.accountInfo.getAccount(),
						p);

				for (int i = 0; i < p.length; ++i) {
					p[i] = 0;
				}
			}
			return this.passwordAuthentication;
		}

		private void connected() {

//			if (this.passwordAuthentication != null) {
//				char[] p = this.passwordAuthentication.getPassword();
//				for (int i = 0; i < p.length; ++i) {
//					p[i] = '\0';
//				}
//				this.passwordAuthentication = null;
//			}
		}

	}

	private class UrlBase {
		private final String urlBase;
		private boolean xmlValid = false;
		private int failedCnt = -1;

		public UrlBase(final String urlBase) {
			this.urlBase = urlBase;
		}

		public void setXmlValid() {
			this.xmlValid = true;
		}

		public void setXmlFailed() {
			if (!this.xmlValid) {
				this.failedCnt = SolvisConnection.this.urlBaseFailedCnt;
			}
		}

		public void setFailed() {
			this.failedCnt = SolvisConnection.this.urlBaseFailedCnt;
		}

		public boolean isFailed() {
			return this.failedCnt == SolvisConnection.this.urlBaseFailedCnt;
		}

		@Override
		public String toString() {
			return this.urlBase;
		}

	}

	private InputStream connect(final String suffix) throws IOException, TerminationException {
		try {
			this.connectTime = System.currentTimeMillis();
			MyAuthenticator authenticator = new MyAuthenticator();
			Authenticator.setDefault(authenticator);

			InputStream in = null;

			if (this.urlBase != null && !this.urlBase.isFailed()) {
				URL url = new URL("http://" + this.urlBase + suffix);
				in = this.connectToOne(url, authenticator);
			} else {
				IOException exception = null;
				this.urlBase = null;
				for (UrlBase urlBase : this.urlBases) {
					if (!urlBase.isFailed()) {
						try {
							exception = null;
							URL url = new URL("http://" + urlBase + suffix);
							in = this.connectToOne(url, authenticator);
							this.urlBase = urlBase;
							logger.info("Unit found at url <" + urlBase + ">.");
						} catch (ConnectException | SocketTimeoutException | NoRouteToHostException e) {
							urlBase.setFailed();
							exception = e;
						}
					}
				}
				if (this.urlBase == null) {

					++this.urlBaseFailedCnt;

					if (exception != null) {
						throw exception;
					} else {
						throw new ConnectException("No valid url found (result format  failed).");
					}
				}
			}
			this.setConnected();
			return in;

		} catch (ConnectException | SocketTimeoutException | NoRouteToHostException e) {
			this.handleExceptionAndThrow(e);
			return null; // dummy
		}
	}

	private InputStream connectToOne(final URL url, final MyAuthenticator authenticator) throws IOException {
		synchronized (this) {
			this.urlConnection = (HttpURLConnection) url.openConnection();
			this.urlConnection.setConnectTimeout(this.connectionTimeout);
			this.urlConnection.setReadTimeout(this.readTimeout);
//				System.out.println(
//						"Connect-Timeout: " + uc.getConnectTimeout() + ", Read-Timeout: " + uc.getReadTimeout());
			this.urlConnection.setUseCaches(false);
			InputStream in = this.urlConnection.getInputStream();
			authenticator.connected();
			return in;
		}
	}

	public BufferedImage getScreen() throws IOException, TerminationException {
		BufferedImage image;
		try {
			synchronized (this) {
				InputStream in = this.connect(Constants.Solvis.DISPLAY);
				image = ImageIO.read(in);
				in.close();
				if (image == null) {
					throw new IOException("Unknown image format. Transmitting error?");
				}
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

		public SolvisMeasurements(final long timeStamp, final String hexString) {
			this.timeStamp = timeStamp;
			this.hexString = hexString;
		}

		public long getTimeStamp() {
			return this.timeStamp;
		}

		public String getHexString() {
			return this.hexString;
		}

	}

	public SolvisMeasurements getMeasurements() throws IOException, TerminationException {
		StringBuilder builder = new StringBuilder();
		long timeStamp = 0;
		try {
			synchronized (this) {
				InputStream in = this.connect(Constants.Solvis.XML);
				InputStreamReader reader = new InputStreamReader(in);

				timeStamp = System.currentTimeMillis();

				boolean finished = false;
				int length = Constants.Solvis.INPUT_BUFFER_SIZE;
				char[] array = new char[length];
				while (!finished) {
					int n = reader.read(array, 0, length);
					if (n < 0) {
						finished = true;
					} else {
						builder.append(array, 0, n);
						if (builder.length() >= 6 && builder.substring(builder.length() - 6).equals("</xml>")) {
							finished = true;
						}
					}
				}
				in.close();
			}
			if (builder.length() < 6 || !builder.substring(builder.length() - 6).equals("</xml>")) {
				this.urlBase.setXmlFailed();
				IOException ex = new IOException("Solvis XML string not complete.");
				logger.error(ex.getMessage());
				throw ex;
			}
			this.urlBase.setXmlValid();
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

		private Button(final String button) {
			this.buttonUrl = button;
		}

		private String getButtonUrl() {
			return this.buttonUrl;
		}
	}

	public void sendButton(final Button button) throws IOException, TerminationException {
		String buttonString = Constants.Solvis.TASTER + button.getButtonUrl() + '&' + Constants.Solvis.ID
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

	public void sendTouch(final Coordinate coord) throws IOException, TerminationException {
		int x = coord.getX() * 2;
		int y = coord.getY() * 2;
		String touchString = Constants.Solvis.TOUCH + '?' + Constants.Solvis.X + x + '&' + Constants.Solvis.Y + y;
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

	public void sendRelease() throws IOException, TerminationException {
		this.sendTouch(Solvis.RELEASE_COORDINATE);
	}

	private void setConnected() {
		this.errorCount = 0;
		this.firstTimeout = 0;

		this.solvisState.connectionSuccessfull(this.urlBase.toString());

	}

	private void handleExceptionAndThrow(final IOException e) throws IOException, TerminationException {
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
			this.solvisState.setPowerOff();
		} else {
			this.solvisState.setDisconnected();
		}
		AbortHelper.getInstance().sleep(Constants.WAIT_TIME_AFTER_IO_ERROR);
		throw e;
	}

	private void calculateMaxResponseTime() {

		int connectionTime = (int) (System.currentTimeMillis() - this.connectTime);
		if (this.maxResponseTime == null || connectionTime < this.maxResponseTime * 2) {
			this.maxResponseTime = connectionTime;
		}
	}

	public int getMaxResponseTime() {
		if (this.maxResponseTime == null) {
			return 0;
		} else {
			return this.maxResponseTime;
		}
	}

	public void setSolvisState(final SolvisState solvisState) {
		this.solvisState = solvisState;
	}

}
