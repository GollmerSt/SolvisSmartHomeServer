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
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.PasswordAuthentication;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.transfer.ConnectionState;
import de.sgollmer.solvismax.error.ModbusError;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Logger;
import de.sgollmer.solvismax.modbus.ModbusAccess;
import de.sgollmer.solvismax.model.SolvisState;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.objects.Coordinate;

public class SolvisConnection extends Observer.Observable<ConnectionState> {

	private static final Logger logger = LogManager.getInstance().getLogger(SolvisConnection.class);
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
	private ModbusMaster modbusMaster = null;

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

		public void connected() {

//			if (this.passwordAuthentication != null) {
//				char[] p = this.passwordAuthentication.getPassword();
//				for (int i = 0; i < p.length; ++i) {
//					p[i] = '\0';
//				}
//				this.passwordAuthentication = null;
//			}
		}

	}

	public InputStream connect(String suffix) throws IOException {
		try {
			this.connectTime = System.currentTimeMillis();
			MyAuthenticator authenticator = new MyAuthenticator();
			Authenticator.setDefault(authenticator);
			URL url = new URL("http://" + this.urlBase + suffix);
			synchronized (this) {
				this.urlConnection = (HttpURLConnection) url.openConnection();
				this.urlConnection.setConnectTimeout(this.connectionTimeout);
				this.urlConnection.setReadTimeout(this.readTimeout);
//				System.out.println(
//						"Connect-Timeout: " + uc.getConnectTimeout() + ", Read-Timeout: " + uc.getReadTimeout());
				InputStream in = this.urlConnection.getInputStream();
				authenticator.connected();
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
			return this.timeStamp;
		}

		public String getHexString() {
			return this.hexString;
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
			return this.buttonUrl;
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
		return this.connectionState;
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

	public void setSolvisState(SolvisState solvisState) {
		this.solvisState = solvisState;
	}

	private boolean modbusConnect() throws UnknownHostException, ModbusIOException {

		if (this.modbusMaster == null) {

			TcpParameters tcpParameters = new TcpParameters();

			// tcp parameters have already set by default as in example
			tcpParameters.setHost(InetAddress.getByName(this.urlBase));
			tcpParameters.setKeepAlive(true);
			tcpParameters.setPort(Modbus.TCP_PORT);

			this.modbusMaster = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
		}
		if (!this.modbusMaster.isConnected()) {
			this.modbusMaster.connect();
		}

		return this.modbusMaster.isConnected();
	}

	public int[] readModbus(ModbusAccess modbusAccess, int quantity) throws IOException {
		try {
			if (this.modbusConnect()) {
				int[] registers = null;
				switch (modbusAccess.getType()) {
					case HOLDING:
						registers = this.modbusMaster.readHoldingRegisters(Constants.MODBUS_SLAVE_ID,
								modbusAccess.getAddress(), quantity);
						break;
					case INPUT:
						registers = this.modbusMaster.readInputRegisters(Constants.MODBUS_SLAVE_ID,
								modbusAccess.getAddress(), quantity);
						break;
				}
				return registers;
			}
		} catch (Throwable e) {
			throw new ModbusError("Modbus error " + e.getClass() + ":" + e.getMessage(), e);
		}
		return null;
	}

	public boolean writeModbus(ModbusAccess modbusAccess, int[] datas) throws IOException {
		try {
			if (this.modbusConnect()) {
				switch (modbusAccess.getType()) {
					case HOLDING:
						this.modbusMaster.writeMultipleRegisters(Constants.MODBUS_SLAVE_ID, modbusAccess.getAddress(),
								datas);
						return true;
					case INPUT:
						return false;
				}
			}
			return false;
		} catch (Throwable e) {
			throw new ModbusError("Modbus error " + e.getClass() + ":" + e.getMessage(), e);
		}
	}

}
