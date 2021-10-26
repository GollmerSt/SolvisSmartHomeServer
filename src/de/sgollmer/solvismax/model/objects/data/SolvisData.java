/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.data;

import java.util.Calendar;

import de.sgollmer.solvismax.Constants.Csv;
import de.sgollmer.solvismax.connection.mqtt.MqttData;
import de.sgollmer.solvismax.connection.mqtt.TopicType;
import de.sgollmer.solvismax.connection.transfer.SingleValue;
import de.sgollmer.solvismax.connection.transfer.SolvisStatePackage;
import de.sgollmer.solvismax.error.FatalError;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.Helper;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.SolvisStatus;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.ChannelInstance;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.unit.AllModifiedChannelValues.ChannelValue;
import de.sgollmer.solvismax.model.objects.unit.AllModifiedChannelValues.ModifyType;

public class SolvisData extends Observer.Observable<SolvisData> implements IObserver<SolvisStatePackage> {

	private static final ILogger logger = LogManager.getInstance().getLogger(SolvisData.class);

	private ChannelInstance channelInstance;
	private final AllSolvisData datas;

	private final Average average;
	private SingleData<?> data = null;
	private SingleData<?> pendingData = null;
	private SingleData<?> sentData = null;
	private SingleData<?> debugData = null;
	private final boolean dontSend;
	private SmartHomeData smartHomeData;
	private int executionTime;
	private boolean fix = false;
	private ChannelValue channelValue = null;

	private Observer.Observable<SolvisData> continousObservable = null;

	public SolvisData(final ChannelDescription description, final AllSolvisData datas, final boolean ignore) {
		this.datas = datas;
		this.dontSend = ignore;
		this.smartHomeData = null;
		if (description.isAverage()) {
			this.average = new Average(this.datas.getAverageCount(), datas.getMeasurementHysteresisFactor());
		} else {
			this.average = null;
		}
		this.channelInstance = ChannelInstance.create(description, this.getSolvis());
	}

	public void setAsSmartHomedata() {
		this.smartHomeData = new SmartHomeData(this);
	}

	private SolvisData(final SolvisData data) {
		this.channelInstance = data.channelInstance;
		this.datas = data.datas;
		if (data.average != null) {
			this.average = new Average(data.average);
		} else {
			this.average = null;
		}
		this.sentData = data.sentData;
		this.dontSend = data.dontSend;
		this.smartHomeData = null;
	}

	public SolvisData(final SingleData<?> data) {
		this.data = data;
		this.channelInstance = null;
		this.average = null;
		this.datas = null;
		this.dontSend = false;
		this.smartHomeData = null;
	}

	/**
	 * This method duplicates the SolvisData without the observers
	 * 
	 * @return duplicate
	 */
	public SolvisData duplicate() {
		return new SolvisData(this);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof SolvisData) {
			return ((SolvisData) obj).getSingleData().equals(this.getSingleData());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		if (this.getSingleData() == null) {
			return 103;
		} else {
			return this.getSingleData().hashCode();
		}
	}

	public String getId() {
		return this.getDescription().getId();
	}

	public String getName() {
		return this.getChannelInstance().getName();
	}

	public void setSingleDataDebug(final SingleData<?> debugData) throws TypeException {
		this.debugData = debugData;
		SingleData<?> data;
		if (debugData == null) {
			data = this.data;
		} else {
			data = debugData;
		}
		this.sendProcessing(data, true, false, false, null);
	}

	private void setData(final SingleData<?> data) throws TypeException {
		this.setData(data, this, false, -1L);
	}

	private synchronized void setData(final SingleData<?> setData, final Object source, final boolean forceTransmit,
			long executionStartTime) throws TypeException {

		if (this.debugData != null) {
			this.debugData = this.debugData.clone(setData.getTimeStamp());
			this.sendProcessing(this.debugData, false, false, forceTransmit, source);
			return;
		}

		if (setData == null || setData.get() == null) {
			this.data = null;
			return;
		}

		if (this.fix) {
			logger.info("Warning: Fix value of <" + this.getDescription().getId() + "> is overwritten ");
		}

		boolean fastChange = false;
		if (executionStartTime > 0L && setData.getTimeStamp() > 0L) {
			this.executionTime = (int) (setData.getTimeStamp() - executionStartTime);
		}

		SingleData<?> sendData = setData;

		if (this.getDescription().isAverage()) {
			this.average.add(setData);
			Average.Result average = this.average.getAverage(setData);
			if (average == null) {
				return;
			}
			sendData = average.getData();
			fastChange = average.isFastChange();
		}

		sendData = this.glitchProcessing(sendData);

		if (sendData == null) {
			return;
		}

		boolean changed = !sendData.equals(this.data);

		this.data = sendData;

		this.sendProcessing(sendData, changed, fastChange, forceTransmit, source);

	}

	private SingleData<?> glitchProcessing(final SingleData<?> data) {

		if (data == null || data.getTimeStamp() <= 0) {
			return data;
		}

		int glitchInterval = this.getDescription().glitchInhibitScanIntervals();

		if (glitchInterval <= 0) {
			return data;
		}

		int glitchInterval_ms = glitchInterval * this.getDescription().getScanInterval_ms(this.getSolvis());

		SingleData<?> result = null;

		if (this.pendingData != null) {

			if (data.equals(this.pendingData)) {

				if (data.getTimeStamp() > this.pendingData.getTimeStamp() + glitchInterval_ms) {

					this.pendingData = null;
					result = data;
				}
			} else {
				this.pendingData = data;
			}
		} else if (!data.equals(this.getSingleData())) {

			this.pendingData = data;
		}

		return result;

	}

	private void sendProcessing(final SingleData<?> data, final boolean changed, final boolean fastChange,
			final boolean forceTransmit, final Object source) throws TypeException {

		if (!this.isEmpty() && (changed || forceTransmit)) {

			if (!this.getDescription().isWriteable() || !this.datas.getSolvis().willBeModified(this) || forceTransmit) {

				this.notify(this, source);
			}
			logger.debug("Channel: " + this.getId() + ", value: " + data.toString());
		}

		if (this.smartHomeData != null) {
			this.smartHomeData.notify(changed, fastChange, source, forceTransmit);
		}
		if (this.continousObservable != null) {

			this.continousObservable.notify(this, source);
		}
	}

	/**
	 * @return the description
	 */
	public ChannelDescription getDescription() {
		return this.channelInstance.getDescription();
	}

	public void setInteger(final Integer integer, final long timeStamp, final Object source) throws TypeException {
		this.setData(new IntegerValue(integer, timeStamp), source, false, -1L);
	}

	public void setInteger(final Integer integer, final long timeStamp) throws TypeException {
		this.setData(new IntegerValue(integer, timeStamp));
	}

	public Integer getInteger() throws TypeException {
		SingleData<?> data = this.getSingleData();
		if (data == null) {
			return null;
		} else {
			Integer i = data.getInt();
			if (i == null) {
				throw new TypeException(
						"TypeException: Must be convertable to Integer, current type: <" + data.getClass() + ">.");
			}
			return i;
		}
	}

	public Double getDouble() throws TypeException {
		SingleData<?> data = this.getSingleData();
		if (data == null) {
			return null;
		} else {
			Double d = data.getDouble();
			if (d == null) {
				throw new TypeException(
						"TypeException: Must be convertable to Double, current type: <" + data.getClass() + ">.");
			}
			return d;
		}
	}

	public int getInt() throws TypeException {
		Integer value = this.getInteger();
		if (value == null) {
			value = 0;
		}
		return value;
	}

	public void setDate(final Calendar calendar, final long timeStamp) throws TypeException {
		this.setData(new DateValue(calendar, timeStamp));

	}

	public void setBoolean(final Boolean bool, final long timeStamp) throws TypeException {
		this.setData(new BooleanValue(bool, timeStamp));

	}

	public Helper.Boolean getBoolean() throws TypeException {
		SingleData<?> data = this.getSingleData();
		if (data == null) {
			return Helper.Boolean.UNDEFINED;
		}
		Helper.Boolean bool = data.getBoolean();
		if (bool == Helper.Boolean.UNDEFINED) {
			throw new TypeException("TypeException: Type actual: <" + data.getClass() + ">, target: <BooleanValue>");
		}
		return bool;
	}

	public boolean getBool() throws TypeException {
		Helper.Boolean bool = this.getBoolean();
		return bool.result();
	}

	public void setMode(final IMode<?> mode, final long timeStamp) throws TypeException {
		this.setData(mode.create(timeStamp));
	}

	public ModeValue<?> getMode() {
		SingleData<?> data = this.getSingleData();
		if (data instanceof ModeValue<?>) {
			return (ModeValue<?>) data;
		} else {
			return null;
		}
	}

	public void setMofifiedChannelValue(final ChannelValue channelValue) throws TypeException {
		this.channelValue = channelValue;
		if (channelValue.getType() == ModifyType.FIX) {
			DoubleValue doubleValue = new DoubleValue(this.channelValue.getValue(), -1L);
			this.data = this.getDescription().interpretSetData(doubleValue, false);
			this.fix = true;
			logger.info("The channel <" + this.getId() + "> was set to the fix value \""
					+ this.getSingleData().toString() + "\".");
		}
	}

	public SingleData<?> getCorrectedData() throws TypeException {
		if (this.data == null) {
			return null;
		}
		if ( this.channelValue == null ) {
			return this.getSingleData();
		}
		DoubleValue doubleValue = new DoubleValue(this.channelValue.getValue(), -1L);
		SingleData<?> data = this.getDescription().interpretSetData(doubleValue, true);
		switch (this.channelValue.getType()) {
			case ADD:
				return this.getSingleData().add(data);
			case MULT:
				return this.getSingleData().mult(data);
			case FIX:
				return this.getSingleData();
			default:
				throw new FatalError("Channel value type <" + this.channelValue.getType().name() + "> not supported");
		}
	}

	public void setSingleData(final SetResult data) throws TypeException {
		this.setData(data.getData(), this, data.isForceTransmit(), -1L);
	}

	public void setSingleDataWithTransmit(final SingleData<?> data, final long executionStartTime)
			throws TypeException {
		this.setData(data, this, true, executionStartTime);
	}

	public void setSingleData(final SingleData<?> data, final Object source) throws TypeException {
		this.setData(data, source, false, -1L);
	}

	public void setSingleData(final SingleData<?> data) throws TypeException {
		this.setSingleData(data, this);
	}

	public void registerContinuousObserver(final Observer.IObserver<SolvisData> observer) {
		if (this.continousObservable == null) {
			this.continousObservable = new Observable<>();
		}
		this.continousObservable.register(observer);
	}

	public void unregisterContinuousObserver(final Observer.IObserver<SolvisData> observer) {
		if (this.continousObservable != null) {
			this.continousObservable.unregister(observer);
		}
	}

	@Override
	public String toString() {
		return this.getSingleData().toString();
	}

	public SingleData<?> getSingleData() {
		if (this.debugData == null) {
			return this.data;
		} else {
			return this.debugData;
		}
	}

	public SingleData<?> normalize() throws TypeException {
		return this.getDescription().normalize(this.getSingleData());
	}

	@Override
	public void update(final SolvisStatePackage data, final Object source) {
		SolvisStatus state = data.getState();
		if (state == SolvisStatus.POWER_OFF && this.average != null) {
			this.average.clear();
		}
	}

	/**
	 * Get the time stamp of the last measurement
	 * 
	 * @return
	 */
	public long getTimeStamp() {
		SingleData<?> data = this.getSingleData();
		if (data != null) {
			return data.getTimeStamp();
		} else {
			return -1;
		}
	}

	public synchronized final SingleData<?> getSentData() {
		if (this.sentData != null) {
			return this.sentData;
		} else {
			return this.getSingleData();
		}
	}

	public boolean isFastChange() {
		return this.getSingleData().isFastChange();
	}

	public Solvis getSolvis() {
		return this.datas.getSolvis();
	}

//	public MqttData getMqttData() {
//		if (this.data == null || this.dontSend) {
//			return null;
//		}
//		String value = this.normalize().toString();
//		return new MqttData(this.getSolvis(), Mqtt.formatChannelOutTopic(this.getId()), value, 0, true);
//	}
//
	public boolean isValid() {
		if (this.getSingleData() == null) {
			return false;
		} else if (this.getDescription().isWriteable()) {
			return this.fix || this.datas.getLastHumanAccess() < this.getSingleData().getTimeStamp();
		} else {
			return true;
		}

	}

	public Calendar getDate() {
		if (!(this.getSingleData() instanceof DateValue)) {
			return null;
		}
		return ((DateValue) this.getSingleData()).get();
	}

	public boolean isDontSend() {
		return this.dontSend;
	}

	public static class SmartHomeData {
		private final Solvis solvis;
		private final SolvisData solvisData;
		private SingleData<?> current = null;
		private SingleData<?> lastTransmittedData = null;
		private long transmittedTimeStamp = 0;
		private int forceCnt = 0;

		public SmartHomeData(final SolvisData solvisData) {
			this.solvis = solvisData.datas.getSolvis();
			this.solvisData = solvisData;
		}

		private void notify(final boolean changed, final boolean fastChange, final Object source,
				final boolean forceTransmit) throws TypeException {

			if (this.solvisData.dontSend) {
				return;
			}

			long currentTimeStamp = 0;

			synchronized (this.solvisData) {

				this.current = this.solvisData.getCorrectedData();
				currentTimeStamp = this.solvisData.getTimeStamp();
			}

			if (this.forceCnt > 0) {
				--this.forceCnt;
			}

			boolean notify = false;

			if (changed && fastChange && this.forceCnt == 0) {

				boolean buffered = this.solvisData.getDescription().isBuffered() && this.solvis.getUnit().isBuffered();

				long intervall = buffered ? this.solvis.getUnit().getBufferedInterval_ms()
						: this.solvis.getUnit().getMeasurementsInterval_ms();

				long forceUpdateAfterFastChangingIntervals = this.solvis.getUnit()
						.getForceUpdateAfterFastChangingIntervals();

				if (forceUpdateAfterFastChangingIntervals != 0 && currentTimeStamp
						- this.transmittedTimeStamp > intervall * forceUpdateAfterFastChangingIntervals) {
					this.forceCnt = 2;
					logger.debug("Quick change after a long constant period of channel <"
							+ this.solvisData.getDescription() + "> detected.");
				}
				notify = true;
			} else if (this.forceCnt > 0) {
				notify = true;
			} else {
				notify = changed || forceTransmit;
			}

			if (notify) {
				this.solvis.getAllSolvisData().notifySmartHome(this, source);
			}
		}

		public Solvis getSolvis() {
			return this.solvis;
		}

		public boolean isForce() {
			return this.forceCnt > 0;
		}

		public SingleData<?> getData() {
			if (this.forceCnt > 1) {
				return this.lastTransmittedData;
			} else {
				return this.current;

			}
		}

		public ChannelDescription getDescription() {
			return this.solvisData.getDescription();
		}

		public String getName() {
			return this.solvisData.getName();
		}

		public long getTransmittedTimeStamp() {
			return this.transmittedTimeStamp;
		}

		public void setTransmitted(final long transmittedTimeStamp) {
			this.transmittedTimeStamp = transmittedTimeStamp;
			if (this.forceCnt <= 1) {
				this.lastTransmittedData = this.current;
			}
		}

		public MqttData getMqttData() throws TypeException {
			if (this.getData() == null) {
				return null;
			}
			String value = this.getDescription().normalize(this.getData()).toString();
			return new MqttData(TopicType.UNIT_CHANNEL_DATA, this.solvis, this.getName(), value, 0, true);
		}

		public SingleValue toSingleValue(final SingleData<?> data) throws TypeException {
			return new SingleValue(this.getDescription().normalize(data));
		}

		@Override
		public String toString() {
			return "Name: " + this.solvisData.getDescription() + ", Value: " + this.getData();
		}

	}

	public SmartHomeData getSmartHomeData() {
		if (this.dontSend) {
			return null;
		} else {
			return this.smartHomeData;
		}
	}

	public Integer getScanInterval_ms() {
		return this.getDescription().getScanInterval_ms(this.getSolvis());
	}

	public ChannelInstance getChannelInstance() {
		return this.channelInstance;
	}

	public int getExecutionTime() {
		return this.executionTime;
	}

	public String getCsvMeta(final String column, final boolean semicolon) {
		if (this.smartHomeData == null || this.getChannelInstance() == null) {
			return null;
		}
		String csv = null;
		switch (column) {
			case Csv.NAME:
				csv = this.getChannelInstance().getName();
				break;
			case Csv.ALIAS:
				csv = this.getChannelInstance().getAlias();
				break;
			case Csv.MQTT:
				String name = this.channelInstance.getName();
				csv = TopicType.formatChannelPublish(name);
				break;
		}
		if (csv == null) {
			csv = this.getDescription().getCsvMeta(column, semicolon);
		}
		return csv;
	}

	public boolean isFix() {
		return this.fix;
	}

}
