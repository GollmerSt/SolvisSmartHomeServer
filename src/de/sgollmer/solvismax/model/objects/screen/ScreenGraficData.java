/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.screen;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Base64.Encoder;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.imagepatternrecognition.pattern.Pattern;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.XmlWriteable;

public class ScreenGraficData implements XmlWriteable {
	private final String id;
	private final MyImage image;

	public ScreenGraficData(String id, MyImage image) {
		this.id = id;
		this.image = image;
	}

	public String getId() {
		return id;
	}

	@Override
	public void writeXml(XMLStreamWriter writer) throws XMLStreamException, IOException {
		writer.writeAttribute("id", this.id);
		writer.writeAttribute("isPattern", Boolean.toString(this.image instanceof Pattern));
		BufferedImage image = this.image.createBufferdImage();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(image, "bmp", bos);
		Encoder base64 = Base64.getEncoder();
		writer.writeCharacters(new String(base64.encode(bos.toByteArray()), "UTF-8"));
	}

	/**
	 * @return the image
	 */
	public MyImage getImage() {
		return image;
	}

	public static class Creator extends CreatorByXML<ScreenGraficData> {

		private String id;
		private boolean isPattern = false;
		private MyImage image;
		private final StringBuilder base64 = new StringBuilder();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "isPattern":
					this.isPattern = Boolean.parseBoolean(value);
					break;
			}

		}

		@Override
		public ScreenGraficData create() throws XmlError, IOException {
			byte[] bytes = Base64.getDecoder().decode(this.base64.toString());
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			BufferedImage image = ImageIO.read(bis);
			this.image = new MyImage(image);
			if ( this.isPattern ) {
				this.image = new Pattern( this.image ) ;
			}
			return new ScreenGraficData(this.id, this.image);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {

		}

		@Override
		public void addCharacters(String data) {
			this.base64.append(data);
		}

	}

}
