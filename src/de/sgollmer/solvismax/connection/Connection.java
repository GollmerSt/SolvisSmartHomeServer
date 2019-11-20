package de.sgollmer.solvismax.connection;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
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

public class Connection {
	
	private class MyAuthenticator extends Authenticator
	{
		protected PasswordAuthentication getPasswordAuthentication() 
		{ 
			//System.out.println( "Hier erfolgt die Authentifizierung" ) ;
			//System.out.printf( "url=%s, host=%s, ip=%s, port=%s%n", 
	        //               getRequestingURL(), getRequestingHost(), 
	        //               getRequestingSite(), getRequestingPort() ); 
	 
			return new PasswordAuthentication( "SGollmer", "e5am1kro".toCharArray() ); 
		}
	}

	
	public InputStream connect() throws IOException {
		Authenticator.setDefault( new MyAuthenticator() ) ;
		URL url = new URL("http://192.168.1.40/display.bmp?");
		//URL url = new URL("http://192.168.1.40/sc2_val.xml");
		HttpURLConnection uc = (HttpURLConnection)  url.openConnection();
//		String userpass = "SGollmer" + ":" + "e5am1kro";
//		String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
//		uc.setRequestProperty ("Authorization", basicAuth);
		InputStream in = uc.getInputStream();
		
		return in ;
		
		
	}
	
	public static void main( String [] args ) throws IOException {
		InputStream in ;
		try {
			in = new Connection().connect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ;
		}
		ScreenSaver saver = new ScreenSaver(new Coordinate(75, 0), new Coordinate(75, 20),
				new Coordinate(75, 21), new Coordinate(75, 33));
		BufferedImage image = ImageIO.read( in );
		
		MyImage myImage = new MyImage(image) ;
		
		if ( saver.is(myImage)) {
			System.out.println("Ist screensaver");
		}
		
	}
	
}
