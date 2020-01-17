/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.Helper.Format;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.ocr.OcrRectangle;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.configuration.Configurations.Configuration;
import de.sgollmer.solvismax.objects.Rectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Solar implements Configuration {

	private static final String XML_RETURN_TEMPERATURE = "ReturnTemperature";
	private static final String XML_OUTGOING_TEMPERATURE = "OutgoingTemperature";

	private final String screenRef;
	private final Format format;
	private final int maxTemperatureX10;
	private final Collection< Rectangle > rectangles = new ArrayList<>() ;

	public Solar(String screenRef, String format, int maxTemperatureX10, Rectangle returnTemperature,
			Rectangle outgoingTemperature) {
		this.screenRef = screenRef;
		this.format = new Format(format) ;
		this.maxTemperatureX10 = maxTemperatureX10 ;
		this.rectangles.add(outgoingTemperature);
		this.rectangles.add(returnTemperature);
	}
	
	

	public static class Creator extends CreatorByXML<Solar> {

		private  String screenRef;
		private  String format;
		private  int maxTemperatureX10;
		private  Rectangle returnTemperature;
		private  Rectangle outgoingTemperature;
		
		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch( name.getLocalPart() ) {
				case "screenRef":
					this.screenRef = value ;
					break ;
				case "format":
					this.format = value ;
					break ;
				case "maxTemperatureX10":
					this.maxTemperatureX10 = Integer.parseInt(value) ;
					break ;
			}
			
		}

		@Override
		public Solar create() throws XmlError, IOException {
			return new Solar(screenRef, format, maxTemperatureX10, returnTemperature, outgoingTemperature);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart() ;
			switch (id) {
				case XML_OUTGOING_TEMPERATURE:
				case XML_RETURN_TEMPERATURE:
					return new Rectangle.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch( creator.getId() ) {
				case XML_OUTGOING_TEMPERATURE:
					this.outgoingTemperature = (Rectangle) created ;
					break ;
				case XML_RETURN_TEMPERATURE:
					this.returnTemperature = (Rectangle) created ;
					break ;
			}
			
		}
		
	}

	@Override
	public int getConfiguration(Solvis solvis) throws IOException {
		MyImage image = solvis.getCurrentImage() ;
		for ( Rectangle rectangle:this. rectangles) {
			OcrRectangle ocr = new OcrRectangle(image, rectangle);
			String scanned = ocr.getString() ;
			String intString = this.format.getString(scanned) ;
			if ( Integer.parseInt(intString) <= this.maxTemperatureX10 ) {
				return 0x08 ;
			}
		}
		return 0x00 ;
	}

	@Override
	public Screen getScreen(Solvis solvis) {
		return solvis.getSolvisDescription().getScreens().get(this.screenRef, 0) ;
	}

}
