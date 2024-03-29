/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.screen.ScreenGraficData;
import de.sgollmer.solvismax.model.objects.unit.Feature;
import de.sgollmer.solvismax.model.objects.unit.Features;
import de.sgollmer.xmllibrary.ArrayXml;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.IXmlWriteable;
import de.sgollmer.xmllibrary.XmlException;

public class SystemGrafics implements IXmlWriteable {

	private static final String XML_SCREEN_GRAFIC = "ScreenGrafic";
	private static final String XML_FEATURES = "Features";
	private static final String XML_FEATURE = "Feature";

	private final String id;
	private long configurationMask;
	private long baseConfigurationMask = 0;
	private final Map<String, ScreenGraficData> graficDatas;
	private final Map<String, Boolean> features;

	SystemGrafics(String id) {
		this(id, 0, 0, new HashMap<>(), new HashMap<>());
	}

	private SystemGrafics(final String id, final long configurationMask, final int baseConfigurationMask,
			final Map<String, ScreenGraficData> graficDatas, final Map<String, Boolean> features) {
		this.id = id;
		this.configurationMask = configurationMask;
		this.baseConfigurationMask = baseConfigurationMask;
		;
		this.graficDatas = graficDatas;
		this.features = features;
	}

	String getId() {
		return this.id;
	}

	public void clear() {
		this.graficDatas.clear();
	}

	public ScreenGraficData get(final String id) {
		return this.graficDatas.get(id);
	}

	public ScreenGraficData remove(final String id) {
		return this.graficDatas.remove(id);
	}

	public void put(final String id, final MyImage image) {
		ScreenGraficData data = new ScreenGraficData(id, image);
		this.graficDatas.put(id, data);
	}

	@Override
	public void writeXml(final XMLStreamWriter writer) throws XMLStreamException, IOException {
		writer.writeAttribute("id", this.id);
		writer.writeAttribute("configurationMask", Long.toString(this.configurationMask));
		writer.writeAttribute("baseConfigurationMask", Long.toString(this.baseConfigurationMask));
		writer.writeStartElement(XML_FEATURES);
		for (Map.Entry<String, Boolean> entry : this.features.entrySet()) {
			writer.writeStartElement(XML_FEATURE);
			Feature feature = new Feature(entry.getKey(), entry.getValue());
			feature.writeXml(writer);
			writer.writeEndElement();
		}
		writer.writeEndElement();
		for (ScreenGraficData data : this.graficDatas.values()) {
			writer.writeStartElement(XML_SCREEN_GRAFIC);
			data.writeXml(writer);
			writer.writeEndElement();
		}

	}

	static class Creator extends CreatorByXML<SystemGrafics> {

		private String id;
		private int configurationMask;
		private int baseConfigurationMask;
		private final Map<String, ScreenGraficData> graficDatas = new HashMap<>();
		private final Map<String, Boolean> features = new HashMap<>();

		Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "id":
					this.id = value;
					break;
				case "configurationMask":
					this.configurationMask = Integer.parseInt(value);
					break;
				case "baseConfigurationMask":
					this.baseConfigurationMask = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public SystemGrafics create() throws XmlException, IOException {

			return new SystemGrafics(this.id, this.configurationMask, this.baseConfigurationMask, this.graficDatas,
					this.features);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_SCREEN_GRAFIC:
					return new ScreenGraficData.Creator(id, this.getBaseCreator());
				case XML_FEATURES:
					return new ArrayXml.Creator<Feature, Feature>(id, this.getBaseCreator(), new Feature(),
							XML_FEATURE);
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_SCREEN_GRAFIC:
					ScreenGraficData data = (ScreenGraficData) created;
					this.graficDatas.put(data.getId(), data);
					break;
				case XML_FEATURES:
					@SuppressWarnings("unchecked")
					Collection<Feature> features = ((ArrayXml<Feature, Feature>) created).getArray();
					for (Feature feature : features) {
						this.features.put(feature.getId(), feature.isSet());
					}
			}
		}

	}

	public boolean isEmpty() {
		return this.graficDatas.isEmpty();
	}

	public long getConfigurationMask() {
		return this.configurationMask;
	}

	public void setConfigurationMask(final long configurationMask) {
		this.configurationMask = configurationMask;
	}

//	public boolean isAdmin() {
//		Boolean admin = this.features.get(Features.XML_ADMIN);
//		return admin == null ? false : admin;
//	}
//
//	public void setAdmin(boolean admin) {
//		this.features.put(Features.XML_ADMIN, admin);
//	}
//
	public long getBaseConfigurationMask() {
		return this.baseConfigurationMask;
	}

	public void setBaseConfigurationMask(final long baseConfigurationMask) {
		this.baseConfigurationMask = baseConfigurationMask;
	}

	public void add(final Feature feature) {
		this.features.put(feature.getId(), feature.isSet());

	}

	public boolean areRelevantFeaturesEqual(final Map<String, Boolean> features) {
		for (Map.Entry<String, Boolean> entry : this.features.entrySet()) {
			if (Features.getAdminKey().equals(entry.getKey())) {
				if (!entry.getValue() && features.get(Features.getAdminKey())) {
					return false;
				}
			} else {
				Boolean cmp = features.get(entry.getKey());
				cmp = cmp == null ? false : cmp;
				if (entry.getValue() != cmp) {
					return false;
				}
			}
		}
		return true;
	}

}
