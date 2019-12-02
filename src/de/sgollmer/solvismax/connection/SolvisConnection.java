package de.sgollmer.solvismax.connection;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import javax.imageio.ImageIO;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.screen.ScreenSaver;
import de.sgollmer.solvismax.objects.Coordinate;

public class SolvisConnection {

	private final String urlBase;
	private final AccountInfo accountInfo;

	public SolvisConnection(String urlBase, AccountInfo accountInfo) {
		this.urlBase = urlBase;
		this.accountInfo = accountInfo;
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
			Authenticator.setDefault(new MyAuthenticator());
			URL url = new URL(this.urlBase + suffix);
			synchronized (this) {
				HttpURLConnection uc = (HttpURLConnection) url.openConnection();
				InputStream in = uc.getInputStream();
				return in;
			}
		} catch (ConnectException e) {
			throw new IOException(e.getMessage());
		}
	}

	public synchronized BufferedImage getScreen() throws IOException {
		InputStream in = this.connect("/display.bmp?");
		BufferedImage image = ImageIO.read(in);
		in.close();
		return image;
	}

	public String getMeasurements() throws IOException {
		StringBuilder builder = new StringBuilder();
		synchronized (this) {
			InputStream in = this.connect("/sc2_val.xml");
			InputStreamReader reader = new InputStreamReader(in);

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

		return builder.toString();
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
		synchronized (this) {
			InputStream in = this.connect(buttonString);
			in.close();
		}
	}

	public void sendTouch(Coordinate coord) throws IOException {
		int x = coord.getX() * 2;
		int y = coord.getY() * 2;
		String touchString = "/Touch.CGI?x=" + x + "&y=" + y;
		synchronized (this) {
			InputStream in = this.connect(touchString);
			in.close();
		}
	}

	public void sendRelease() throws IOException {
		String touchString = "/Touch.CGI?x=510&y=510";
		synchronized (this) {
			InputStream in = this.connect(touchString);
			in.close();
		}
	}

	public static void main(String[] args) throws IOException {

		ScreenSaver saver = new ScreenSaver(new Coordinate(75, 0), new Coordinate(75, 20), new Coordinate(75, 21),
				new Coordinate(75, 33), null);

		SolvisConnection solvisConnection = new SolvisConnection("http://192.168.1.40", new AccountInfo() {

			@Override
			public String getAccount() {
				return "SGollmer";
			}

			@Override
			public char[] createPassword() {
				return "e5am1kro".toCharArray();
			}
		});

		BufferedImage image = solvisConnection.getScreen();

		MyImage myImage = new MyImage(image);

		if (saver.is(myImage)) {
			System.out.println("Ist screensaver");
		} else {
			System.out.println("KEIN screensaver");
		}

		String xml = solvisConnection.getMeasurements();

		System.out.println(xml);

		solvisConnection.sendButton(Button.BACK);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		solvisConnection.sendTouch(new Coordinate(20, 40));
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
		solvisConnection.sendRelease();
	}

}
