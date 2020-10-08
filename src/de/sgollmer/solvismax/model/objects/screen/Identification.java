package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.WhiteGraficRectangle;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Identification {

	private static final String XML_GRAFICS = "Grafic";
	private static final String XML_GRAFICS_REF = "GraficRef";
	private static final String XML_OCR = "Ocr";
	private static final String XML_MUST_BE_WHITE = "MustBeWhite";

	private final Collection<IScreenPartCompare> screenCompares;
	private final Collection<String> screenGraficRefs;

	public Identification(Collection<IScreenPartCompare> screenCompares, Collection<String> screenGraficRefs) {
		this.screenCompares = screenCompares;
		this.screenGraficRefs = screenGraficRefs;
	}

	public static class Creator extends CreatorByXML<Identification> {

		private final Collection<IScreenPartCompare> screenCompares = new ArrayList<>();
		private final Collection<String> screenGraficRefs = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public Identification create() throws XmlException, IOException, AssignmentException, ReferenceException {
			return new Identification(this.screenCompares, this.screenGraficRefs);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_GRAFICS:
					return new ScreenGraficDescription.Creator(id, this.getBaseCreator());
				case XML_GRAFICS_REF:
					return new ScreenGraficRef.Creator(id, this.getBaseCreator());
				case XML_MUST_BE_WHITE:
					return new WhiteGraficRectangle.Creator(id, this.getBaseCreator());
				case XML_OCR:
					return new ScreenOcr.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_GRAFICS:
					this.screenCompares.add((ScreenGraficDescription) created);
					break;
//				case XML_GRAFICS:
//					ScreenGraficDescription grafic = (ScreenGraficDescription) created;
//					this.screenGraficRefs.add(grafic.getId());
//					((SolvisDescription.Creator) this.getBaseCreator()).getScreenGraficDescriptions().add(grafic);
//					break;
				case XML_GRAFICS_REF:
					this.screenGraficRefs.add(((ScreenGraficRef) created).getRefId());
					break;
				case XML_MUST_BE_WHITE:
					this.screenCompares.add((WhiteGraficRectangle) created);
					break;
				case XML_OCR:
					this.screenCompares.add((ScreenOcr) created);
					break;
			}

		}

	}

	public void assign(SolvisDescription description, Screen parent)
			throws XmlException, AssignmentException, ReferenceException {
		for (String id : this.screenGraficRefs) {
			this.screenCompares.add(description.getScreenGrafics().get(id));
		}
		if (this.screenCompares.isEmpty()) {
			throw new XmlException(
					"Error in XML definition: Grafic information of screen <" + parent.getId() + "> is missing.");
		}
		for (IScreenPartCompare screenPartCompare : this.screenCompares) {
			screenPartCompare.assign(description);
		}
	}

	public boolean isMatchingScreen(MyImage image, Solvis solvis) {
		for (IScreenPartCompare screenPart : this.screenCompares) {
			if (!screenPart.isElementOf(image, solvis)) {
				return false;
			}
		}
		return true;
	}

	public boolean isMatchingWOGrafics(MyImage image, Solvis solvis) {
		for (IScreenPartCompare screenPart : this.screenCompares) {
			if (!(screenPart instanceof ScreenGraficDescription) && !screenPart.isElementOf(image, solvis)) {
				return false;
			}
		}
		return true;
	}

	public boolean isLearned(Solvis solvis) {

		for (IScreenPartCompare cmp : this.screenCompares) {
			if (!cmp.isLearned(solvis)) {
				return false;
			}
		}
		return true;
	}

	public void addLearnScreenGrafics(Collection<IScreenPartCompare> descriptions, Solvis solvis) {
		for (IScreenPartCompare cmp : this.screenCompares) {
			if (!cmp.isLearned(solvis) && !descriptions.contains(cmp)) {
				descriptions.add(cmp);
			}
		}
	}

	public boolean learn(Solvis solvis) throws IOException, TerminationException {
		boolean learned = false;
		for (IScreenPartCompare screenPartCompare : this.screenCompares) {
			if (!screenPartCompare.isLearned(solvis)) {
				screenPartCompare.learn(solvis);
				learned = true;
			}
		}
		return learned;
	}


}
