package de.sgollmer.solvismax.model.objects;

import java.util.Collection;

import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IChannelSource.Type;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.data.IMode;

public class ChannelInstance implements IInstance {

	private final ChannelDescription description;
	private final ChannelAssignment assignment;
	private final Solvis solvis;
	private String channelName = null;

	private ChannelInstance(final Solvis solvis, final ChannelDescription description,
			final ChannelAssignment assignment) {

		this.assignment = assignment;
		this.description = description;
		this.solvis = solvis;
	}

	public ChannelDescription getDescription() {
		return this.description;
	}

	public ChannelAssignment getAssignment() {
		return this.assignment;
	}

	public static ChannelInstance create(final ChannelDescription description, final Solvis solvis) {
		ChannelAssignment assignment = solvis.getSolvisDescription().getChannelAssignments().get(description.getId(),
				solvis);
		return new ChannelInstance(solvis, description, assignment);
	}

	private ChannelAssignment getFunctionalAssignment() {
		ChannelAssignment assignment = this.solvis.getUnit().getChannelAssignment(this.assignment.getName());
		if (assignment == null) {
			assignment = this.assignment;
		}
		return assignment;
	}

	public String getName() {
		if (this.channelName == null) {
			this.channelName = this.getFunctionalAssignment().getChannelName();
		}
		return this.channelName;
	}

	public String getAlias() {
		return this.assignment.getAlias();
	}

	public boolean isWriteable() {
		return this.description.isWriteable();
	}

	public Type getType() {
		return this.description.getType();
	}

	public String getUnit() {
		String unit = this.getFunctionalAssignment().getUnit();
		if (unit == null) {
			unit = this.getDescription().getUnit();
		}
		return unit;
	}

	public Double getAccuracy() {
		return this.description.getAccuracy();
	}

	public boolean isBoolean() {
		if (this.assignment.getBooleanValue() != null) {
			return true;
		} else {
			return this.description.isBoolean();
		}
	}

	public UpperLowerStep getUpperLowerStep() {
		if (!isBoolean()) {
			return this.description.getUpperLowerStep();
		} else {
			return null;
		}
	}

	public Collection<? extends IMode<?>> getModes() {
		return this.description.getModes();
	}

	MqttData getMqttMeta() {
		de.sgollmer.solvismax.connection.transfer.ChannelDescription meta = new de.sgollmer.solvismax.connection.transfer.ChannelDescription(
				this);
		return new MqttData(this.solvis, Mqtt.formatChannelMetaTopic(this.getName()), meta.getValue().toString(), 0,
				true);
	}

}
