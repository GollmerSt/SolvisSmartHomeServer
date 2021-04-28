package de.sgollmer.solvismax.model.objects.unit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.model.update.Correction;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class DefaultCorrections {

	private static final String XML_CORRECTION = "Correction";

	private final Collection<DefaultCorrection> corrections;

	private DefaultCorrections(Collection<DefaultCorrection> corrections) {
		this.corrections = corrections;
	}

	public void setCorrection(Correction correction) {
		for (DefaultCorrection defaultCorrection : this.corrections) {
			if (defaultCorrection.setCorrection(correction)) {
				return;
			}
		}
	}

	private static class DefaultCorrection {
		private final Pattern regEx;
		private final int correction;

		private DefaultCorrection(Pattern regEx, int correction) {
			this.regEx = regEx;
			this.correction = correction;
		}

		public boolean setCorrection(Correction correction) {
			if (this.regEx.matcher(correction.getId()).matches()) {
				if (!correction.valid()) {
					correction.modify(this.correction, 1000);
				}
				return true;
			} else {
				return false;
			}
		}
	}

	public static class Creator extends CreatorByXML<DefaultCorrections> {

		private final Collection<DefaultCorrection> corrections = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) throws XmlException {

		}

		@Override
		public DefaultCorrections create() throws XmlException, IOException {
			return new DefaultCorrections(this.corrections);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_CORRECTION:
					return new CorrectionCreator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {
			switch (creator.getId()) {
				case XML_CORRECTION:
					this.corrections.add((DefaultCorrection) created);
					break;
			}
		}

	}

	public static class CorrectionCreator extends CreatorByXML<DefaultCorrection> {

		private Pattern regEx = null;
		private int correction = 0;

		public CorrectionCreator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) throws XmlException {
			switch (name.getLocalPart()) {
				case "regEx":
					try {
						this.regEx = Pattern.compile(value);
					} catch (PatternSyntaxException e) {
						throw new XmlException("RegEx-Syntax-Exception on \"" + value + "\".");
					}
					break;
				case "value_us":
					this.correction = Integer.parseInt(value);
					break;
			}

		}

		@Override
		public DefaultCorrection create() throws XmlException, IOException {
			return new DefaultCorrection(this.regEx, this.correction);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) throws XmlException {

		}

	}
}
