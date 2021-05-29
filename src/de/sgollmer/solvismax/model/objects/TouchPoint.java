/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class TouchPoint implements IAssigner {

	private static final String XML_COORDINATE = "Coordinate";

	private final Coordinate coordinate;
	private final String pushTimeId;
	private final String releaseTimeId;

	private TouchPoint(final Coordinate coordinate, final String pushTimeId, final String releaseTimeId) {
		this.coordinate = coordinate;
		this.pushTimeId = pushTimeId;
		this.releaseTimeId = releaseTimeId;
	}

	public static class Creator extends CreatorByXML<TouchPoint> {

		private String pushTimeId;
		private String releaseTimeId;
		private Coordinate coordinate;

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			switch (name.getLocalPart()) {
				case "pushTimeRefId":
					this.pushTimeId = value;
					break;
				case "releaseTimeRefId":
					this.releaseTimeId = value;
			}

		}

		@Override
		public TouchPoint create() throws XmlException {
			return new TouchPoint(this.coordinate, this.pushTimeId, this.releaseTimeId);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_COORDINATE:
					return new Coordinate.Creator(name.getLocalPart(), this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_COORDINATE:
					this.coordinate = (Coordinate) created;
			}

		}

	}

	@Override
	public void assign(final SolvisDescription description) throws AssignmentException {
		Duration pushTimeDuration = description.getDuration(this.pushTimeId);
		Duration releaseTimeDuration = description.getDuration(this.releaseTimeId);

		if (pushTimeDuration == null || releaseTimeDuration == null) {
			throw new AssignmentException("Duration time not found");
		}
	}

	public Coordinate getCoordinate() {
		return this.coordinate;
	}

	public int getSettingTime(final Solvis solvis) {
		return solvis.getDuration(this.pushTimeId).getTime_ms() + solvis.getDuration(this.releaseTimeId).getTime_ms()
				+ solvis.getMaxResponseTime();
	}

	public boolean execute(final Solvis solvis, final AbstractScreen startingScreen)
			throws IOException, TerminationException {
		solvis.send(this);
		return true;
	}

	public String getPushTimeId() {
		return this.pushTimeId;
	}

	public String getReleaseTimeId() {
		return this.releaseTimeId;
	}

}
