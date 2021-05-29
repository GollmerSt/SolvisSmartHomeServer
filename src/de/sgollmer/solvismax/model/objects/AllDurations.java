/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class AllDurations {

	private static final ILogger logger = LogManager.getInstance().getLogger(AllPreparations.class);

	private final static String XML_DURATION = "Duration";

	private Map<String, Duration> durations = new HashMap<>();

	private AllDurations() {
	}

	private void add(final Duration duration) {
		Duration former = this.durations.put(duration.getId(), duration);
		if (former != null) {
			logger.error("Duration <" + duration.getId() + "> not unique.");
		}
	}

	public Duration get(final String id) {
		return this.durations.get(id);
	}

	public static class Creator extends CreatorByXML<AllDurations> {

		private AllDurations durations;

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
			this.durations = new AllDurations();
		}

		@Override
		public void setAttribute(final QName name, final String value) {

		}

		@Override
		public AllDurations create() throws XmlException {
			return this.durations;
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_DURATION:
					return new Duration.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case "Duration":
					this.durations.add((Duration) created);
					break;
			}

		}

	}

}
