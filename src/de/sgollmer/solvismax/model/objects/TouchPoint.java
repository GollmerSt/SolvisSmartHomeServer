/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentError;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class TouchPoint implements Assigner {

	private static final String XML_COORDINATE = "Coordinate";

	private final Coordinate coordinate;
	private final String pushTimeId;
	private final String releaseTimeId;

	private Integer pushTime = null;
	private Integer releaseTime = null;

	public TouchPoint(Coordinate coordinate, String pushTimeId, String releaseTimeId) {
		this.coordinate = coordinate;
		this.pushTimeId = pushTimeId;
		this.releaseTimeId = releaseTimeId;
	}

	public static class Creator extends CreatorByXML<TouchPoint> {

		private String pushTimeId;
		private String releaseTimeId;
		private Coordinate coordinate;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			switch (name.getLocalPart()) {
				case "pushTimeRefId":
					this.pushTimeId = value;
					break;
				case "releaseTimeRefId":
					this.releaseTimeId = value;
			}

		}

		@Override
		public TouchPoint create() throws XmlError {
			return new TouchPoint(this.coordinate, this.pushTimeId, this.releaseTimeId);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_COORDINATE:
					return new Coordinate.Creator(name.getLocalPart(), this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_COORDINATE:
					this.coordinate = (Coordinate) created;
			}

		}

	}

	@Override
	public void assign(SolvisDescription description) {
		Duration pushTimeDuration = description.getDurations().get(this.pushTimeId);
		Duration releaseTimeDuration = description.getDurations().get(this.releaseTimeId);

		if (pushTimeDuration == null || releaseTimeDuration == null) {
			throw new AssignmentError("Duration time not found");
		}
		this.pushTime = pushTimeDuration.getTime_ms();
		this.releaseTime = releaseTimeDuration.getTime_ms();
	}

	public Coordinate getCoordinate() {
		return this.coordinate;
	}

	public Integer getPushTime() {
		return this.pushTime;
	}

	public Integer getReleaseTime() {
		return this.releaseTime;
	}

	public int getSettingTime(Solvis solvis) {
		return this.pushTime + this.releaseTime + solvis.getMaxResponseTime();
	}

}
