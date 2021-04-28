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

	private ChannelInstance(final Solvis solvis, final ChannelDescription description, final ChannelAssignment assignment) {

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

	public static ChannelInstance create(ChannelDescription description, Solvis solvis) {
		ChannelAssignment assignment = solvis.getSolvisDescription().getChannelAssignments().get(description.getId(),
				solvis);
		return new ChannelInstance(solvis,description, assignment);
	}

	public String getName() {
		return this.assignment.getChannelName();
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
		String unit = this.assignment.getUnit();
		if (this.assignment.getUnit() == null) {
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
		return new MqttData(
				this.solvis, Mqtt.formatChannelMetaTopic(this.getName()), meta.getValue().toString(), 0, true);
	}

}
