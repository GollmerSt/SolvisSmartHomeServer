/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.update;

import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.objects.backup.SystemBackup;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Correction implements SystemBackup.IValue {

	private static final ILogger logger = LogManager.getInstance().getLogger(Correction.class);

	public static final String XML_CORRECTION = "Correction";

	private final String id;
	private long data;
	private long cnt;

	public Correction(String id, long data, long cnt) {
		this.id = id;
		this.data = data;
		this.cnt = cnt;
	}

	public Correction(String id) {
		this(id, 0, 0);
	}
	
	public void set( Correction correction ) {
		this.data = correction.data;
		this.cnt=correction.cnt;
	}

	public static class Creator extends CreatorByXML<Correction> {

		private String id;
		private long data;
		private long cnt;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "value":
					this.data = Long.parseLong(value);
					break;
				case "count":
					this.cnt = Long.parseLong(value);
					break;
			}

		}

		@Override
		public Correction create() throws XmlException, IOException {
			return new Correction(this.id, this.data, this.cnt);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {

		}

	}

	@Override
	public void writeXml(XMLStreamWriter writer) throws XMLStreamException {

		writer.writeStartElement(XML_CORRECTION);
		writer.writeAttribute("id", this.id);
		writer.writeAttribute("value", Long.toString(this.data));
		writer.writeAttribute("count", Long.toString(this.cnt));
		writer.writeEndElement();

	}

	@Override
	public String getId() {
		return this.id;
	}

	public int get(int factor) {
		if (this.cnt == 0) {
			return 0;
		}
		return (int) (this.data * (long) factor / this.cnt);
	}

	public double getDouble() {
		if (this.cnt == 0) {
			return 0;
		}
		return (double) this.data / (double) this.cnt;
	}

	public void modify(int data, int cnt) {

		if (data != (this.data + data) - this.data) {
			logger.error("Correction overflow, new correction ignored");
			return;
		}

		this.cnt += cnt;
		this.data += data;

		logger.debug(Constants.Debug.CORRECTION,
				"Correction (data/cnt): (" + data + "/" + cnt + "), current correction value: " + this.get(1));
	}

}
