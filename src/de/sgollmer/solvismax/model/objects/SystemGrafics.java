/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficData;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.IXmlWriteable;

public class SystemGrafics implements IXmlWriteable {

	private static final String XML_SCREEN_GRAFIC = "ScreenGrafic";

	private final String id;
	private int configurationMask;
	private final Map<String, ScreenGraficData> graficDatas;

	SystemGrafics(String id) {
		this(id, 0, new HashMap<>());
	}

	private SystemGrafics(String id, int configurationMask, Map<String, ScreenGraficData> graficDatas) {
		this.id = id;
		this.configurationMask = configurationMask;
		this.graficDatas = graficDatas;
	}

	String getId() {
		return this.id;
	}

	public void clear() {
		this.graficDatas.clear();
	}

	public ScreenGraficData get(String id) {
		return this.graficDatas.get(id);
	}

	public ScreenGraficData remove(String id) {
		return this.graficDatas.remove(id);
	}

	public void put(String id, MyImage image) {
		ScreenGraficData data = new ScreenGraficData(id, image);
		this.graficDatas.put(id, data);
	}

	@Override
	public void writeXml(XMLStreamWriter writer) throws XMLStreamException, IOException {
		writer.writeAttribute("id", this.id);
		writer.writeAttribute("configurationMask", Integer.toString(this.configurationMask));
		for (ScreenGraficData data : this.graficDatas.values()) {
			writer.writeStartElement(XML_SCREEN_GRAFIC);
			data.writeXml(writer);
			writer.writeEndElement();
		}

	}

	static class Creator extends CreatorByXML<SystemGrafics> {

		private String id;
		private int configurationMask;
		private final Map<String, ScreenGraficData> graficDatas = new HashMap<>();

		Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "configurationMask":
					this.configurationMask = Integer.parseInt(value);
			}

		}

		@Override
		public SystemGrafics create() throws XmlError, IOException {

			return new SystemGrafics(this.id, this.configurationMask, this.graficDatas);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_SCREEN_GRAFIC:
					return new ScreenGraficData.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_SCREEN_GRAFIC:
					ScreenGraficData data = (ScreenGraficData) created;
					this.graficDatas.put(data.getId(), data);
					break;
			}
		}

	}

	public boolean isEmpty() {
		return this.graficDatas.isEmpty();
	}

	public int getConfigurationMask() {
		return this.configurationMask;
	}

	public void setConfigurationMask(int configurationMask) {
		this.configurationMask = configurationMask;
	}

}
