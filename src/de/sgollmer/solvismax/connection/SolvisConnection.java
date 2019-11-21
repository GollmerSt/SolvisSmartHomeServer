package de.sgollmer.solvismax.connection;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

import javax.imageio.ImageIO;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.ScreenSaver;
import de.sgollmer.solvismax.objects.Coordinate;

public class SolvisConnection {

	private final String urlBase;

	public SolvisConnection(String urlBase) {
		this.urlBase = urlBase;
	}

	private class MyAuthenticator extends Authenticator {
		protected PasswordAuthentication getPasswordAuthentication() {
			// System.out.println( "Hier erfolgt die Authentifizierung" ) ;
			// System.out.printf( "url=%s, host=%s, ip=%s, port=%s%n",
			// getRequestingURL(), getRequestingHost(),
			// getRequestingSite(), getRequestingPort() );

			return new PasswordAuthentication("SGollmer", "e5am1kro".toCharArray());
		}
	}

	public InputStream connect(String suffix) throws IOException {
		Authenticator.setDefault(new MyAuthenticator());
		URL url = new URL(this.urlBase + suffix);
		// URL url = new URL("http://192.168.1.40/sc2_val.xml");
		HttpURLConnection uc = (HttpURLConnection) url.openConnection();
		// String userpass = "SGollmer" + ":" + "e5am1kro";
		// String basicAuth = "Basic " + new
		// String(Base64.getEncoder().encode(userpass.getBytes()));
		// uc.setRequestProperty ("Authorization", basicAuth);
		InputStream in = uc.getInputStream();

		return in;

	}

	public BufferedImage getScreen() throws IOException {
		InputStream in = this.connect("/display.bmp?");

		BufferedImage image = ImageIO.read(in);

		in.close();
		return image;
	}

	public String getMeasurements() throws IOException {
		InputStream in = this.connect("/sc2_val.xml");
		InputStreamReader reader = new InputStreamReader(in);

		StringBuilder builder = new StringBuilder();
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
		InputStream in = this.connect(buttonString);
		in.close();
	}

	public void sendTouch(Coordinate coord) throws IOException {
		int x = coord.getX() * 2;
		int y = coord.getY() * 2;
		String touchString = "/Touch.CGI?x=" + x + "&y=" + y;
		InputStream in = this.connect(touchString);
		in.close();
	}

	public void sendRelease() throws IOException {
		String touchString = "/Touch.CGI?x=510&y=510";
		InputStream in = this.connect(touchString);
		in.close();
	}

	public static void main(String[] args) throws IOException {

		ScreenSaver saver = new ScreenSaver(new Coordinate(75, 0), new Coordinate(75, 20), new Coordinate(75, 21),
				new Coordinate(75, 33));

		SolvisConnection solvisConnection = new SolvisConnection("http://192.168.1.40");

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
		
		solvisConnection.sendTouch(new Coordinate(20,40) ) ;
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
		solvisConnection.sendRelease() ;
	}

}
