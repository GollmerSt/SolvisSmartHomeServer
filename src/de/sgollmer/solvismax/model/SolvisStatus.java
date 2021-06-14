package de.sgollmer.solvismax.model;

import de.sgollmer.solvismax.connection.mqtt.TopicType;

public enum SolvisStatus {
	POWER_OFF(TopicType.UNIT_STATUS), //
	REMOTE_CONNECTED(TopicType.UNIT_STATUS), //
	SOLVIS_CONNECTED(TopicType.UNIT_STATUS), //
	SOLVIS_DISCONNECTED(TopicType.UNIT_STATUS), //
	ERROR(TopicType.UNIT_STATUS), //
	USER_ACCESS_DETECTED(TopicType.UNIT_HUMAN), //
	SERVICE_ACCESS_DETECTED(TopicType.UNIT_HUMAN), //
	HUMAN_ACCESS_FINISHED(TopicType.UNIT_HUMAN), //
	CONTROL_WRITE_ONGOING(TopicType.UNIT_CONTROL), //
	CONTROL_READ_ONGOING(TopicType.UNIT_CONTROL), //
	CONTROL_MONITORING(TopicType.UNIT_CONTROL), //
	CONTROL_FINISHED(TopicType.UNIT_CONTROL), //
	UNDEFINED(TopicType.UNIT_STATUS);

	private final TopicType topicType;

	private SolvisStatus(final TopicType topicType) {
		this.topicType = topicType;

	}

	public TopicType getTopicType() {
		return this.topicType;
	}

}