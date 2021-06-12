/************************************************************************
 * 
 * $Id: Mqtt.java 277 2020-07-19 16:00:49Z stefa_000 $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.mqtt;

import de.sgollmer.solvismax.connection.IReceivedData;
import de.sgollmer.solvismax.connection.transfer.Command;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SingleData;

class SubscribeData implements IReceivedData {
	private final String clientId;
	private final String unitId;
	final TopicType type;
	private final String channelId;
	private Solvis solvis;
	private SingleData<?> value;

	SubscribeData(final String clientId, final String unitId, final String channelId, final TopicType type) {
		this.clientId = clientId;
		this.unitId = unitId;
		this.type = type;
		this.channelId = channelId;
	}

	@Override
	public String getClientId() {
		return this.clientId;
	}

	String getUnitId() {
		return this.unitId;
	}

	@Override
	public Solvis getSolvis() {
		return this.solvis;
	}

	@Override
	public Command getCommand() {
		return this.type.getCommand();
	}

	@Override
	public String getChannelId() {
		return this.channelId;
	}

	@Override
	public SingleData<?> getSingleData() {
		return this.value;
	}

	void setValue(final SingleData<?> value) {
		this.value = value;
	}

	@Override
	public void setSolvis(final Solvis solvis) {
		this.solvis = solvis;
	}
}