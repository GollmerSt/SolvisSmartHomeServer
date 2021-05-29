/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.Arrays;
import java.util.Collection;

import de.sgollmer.solvismax.connection.ISendData;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.SolvisStatus;

public class SolvisStatePackage implements ISendData {

	private final SolvisStatus state;
	private final Solvis solvis;

	public SolvisStatePackage(final SolvisStatus state, final Solvis solvis) {
		this.state = state;
		this.solvis = solvis;
	}

	@Override
	public JsonPackage createJsonPackage() {

		Frame frame = new Frame();
		Element element = new Element("SolvisState", new SingleValue(this.state.name()));
		frame.add(element);

		return new JsonPackage(Command.SOLVIS_STATE, frame);
	}

	@Override
	public Collection<MqttData> createMqttData() {
		return Arrays.asList(new MqttData(this.solvis, this.state.getMqttPrefix(), this.state.name(), 0, true));
	}

	public SolvisStatus getState() {
		return this.state;
	}
}
