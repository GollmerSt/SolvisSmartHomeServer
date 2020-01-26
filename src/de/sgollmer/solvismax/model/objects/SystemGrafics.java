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
import de.sgollmer.solvismax.xml.XmlWriteable;

public class SystemGrafics implements XmlWriteable {

	private static final String XML_SCREEN_GRAFIC = "ScreenGrafic";

	private final String id;
	private int controlFileHashCode;
	private int configurationMask;
	private final Map<String, ScreenGraficData> graficDatas;

	public SystemGrafics(String id, int controllFileHashCode) {
		this(id, controllFileHashCode, 0, new HashMap<>());
	}

	private SystemGrafics(String id, int controllFileHashCode, int configurationMask,
			Map<String, ScreenGraficData> graficDatas) {
		this.id = id;
		this.controlFileHashCode = controllFileHashCode;
		this.configurationMask = configurationMask;
		this.graficDatas = graficDatas;
	}

	public String getId() {
		return id;
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
		writer.writeAttribute("controlFileHashCode", Integer.toString(this.controlFileHashCode));
		writer.writeAttribute("configurationMask", Integer.toString(this.configurationMask));
		for (ScreenGraficData data : this.graficDatas.values()) {
			writer.writeStartElement(XML_SCREEN_GRAFIC);
			data.writeXml(writer);
			writer.writeEndElement();
		}

	}

	public static class Creator extends CreatorByXML<SystemGrafics> {

		private String id;
		private int controlFileHashCode;
		private int configurationMask;
		private final Map<String, ScreenGraficData> graficDatas = new HashMap<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "controlFileHashCode":
					this.controlFileHashCode = Integer.parseInt(value);
					break;
				case "configurationMask":
					this.configurationMask = Integer.parseInt(value);
			}

		}

		@Override
		public SystemGrafics create() throws XmlError, IOException {

			return new SystemGrafics(id, controlFileHashCode, configurationMask, graficDatas);
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

	/**
	 * @return the controlFileHashCode
	 */
	public int getControlFileHashCode() {
		return controlFileHashCode;
	}

	/**
	 * @param controlFileHashCode the controlFileHashCode to set
	 */
	public void setControlFileHashCode(int controlFileHashCode) {
		this.controlFileHashCode = controlFileHashCode;
	}

	public int getConfigurationMask() {
		return configurationMask;
	}

	public void setConfigurationMask(int configurationMask) {
		this.configurationMask = configurationMask;
	}
}
