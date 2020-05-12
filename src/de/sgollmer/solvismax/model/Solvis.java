/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.Calendar;

import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.logging.slf4j.Log4jLogger;
//import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.Distributor;
import de.sgollmer.solvismax.connection.SolvisConnection;
import de.sgollmer.solvismax.connection.SolvisConnection.Button;
import de.sgollmer.solvismax.connection.SolvisConnection.SolvisMeasurements;
import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.Reference;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.modbus.ModbusAccess;
import de.sgollmer.solvismax.model.WatchDog.HumanAccess;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.Duration;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.SystemGrafics;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.Units.Unit;
import de.sgollmer.solvismax.model.objects.backup.MeasurementsBackupHandler;
import de.sgollmer.solvismax.model.objects.backup.SystemMeasurements;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.History;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;

public class Solvis {

	// private static final org.slf4j.Logger logger =
	// LoggerFactory.getLogger(Solvis.class);
	private static final Logger logger = LogManager.getLogger(Solvis.class);
	private static final Level LEARN = Level.getLevel("LEARN");
	private static final boolean DEBUG_TWO_STATIONS = false;

	private final SolvisState solvisState = new SolvisState(this);
	private final SolvisDescription solvisDescription;
	private final AllSolvisData allSolvisData = new AllSolvisData(this);
	private SystemGrafics grafics;

	private SolvisWorkers worker;

	private final String type;
	private final int defaultReadMeasurementsInterval_ms;
	private int configurationMask = 0;
	private SolvisScreen currentScreen = null;
	private SolvisScreen savedScreen = null;
	private Screen home = null;
	private SolvisMeasurements measureData = null;
	private boolean screenSaverActive = false;
	private final TouchPoint resetSceenSaver;
	private final SolvisConnection connection;
	private final MeasurementsBackupHandler backupHandler;
	private final Distributor distributor;
	private final MeasurementUpdateThread measurementUpdateThread;
	private final Observable<Boolean> abortObservable = new Observable<>();
	private final String timeZone;
	private final boolean delayAfterSwitchingOnEnable;
	private final Unit unit;
	private final History history = new History();

	public Solvis(Unit unit, SolvisDescription solvisDescription, SystemGrafics grafics, SolvisConnection connection,
			MeasurementsBackupHandler measurementsBackupHandler, String timeZone) {
		this.unit = unit;
		this.type = unit.getType();
		this.defaultReadMeasurementsInterval_ms = unit.getDefaultReadMeasurementsInterval_ms();
		this.solvisDescription = solvisDescription;
		this.resetSceenSaver = solvisDescription.getSaver().getResetScreenSaver();
		this.grafics = grafics;
		this.connection = connection;
		this.connection.setSolvisState(this.solvisState);
		this.allSolvisData.setAverageCount(unit.getDefaultAverageCount());
		this.allSolvisData.setMeasurementHysteresisFactor(unit.getMeasurementHysteresisFactor());
		this.allSolvisData.setReadMeasurementInterval(unit.getDefaultReadMeasurementsInterval_ms());
		this.worker = new SolvisWorkers(this);
		this.backupHandler = measurementsBackupHandler;
		this.backupHandler.register(this, unit.getId());
		this.distributor = new Distributor(unit);
		this.measurementUpdateThread = new MeasurementUpdateThread(unit);
		this.timeZone = timeZone;
		this.delayAfterSwitchingOnEnable = unit.isDelayAfterSwitchingOnEnable();
	}

	private Observer.Observable<HumanAccess> screenChangedByHumanObserable = new Observable<>();

	private Object solvisMeasureObject = new Object();

	public boolean isCurrentImageValid() {
		return this.currentScreen != null;
	}

	public void setCurrentScreen(SolvisScreen screen) {
		this.currentScreen = screen;
	}
	
	public SolvisScreen getCurrentScreen() throws IOException {
		return this.getCurrentScreen(true);
	}

	public SolvisScreen getCurrentScreen(boolean screensaverOff) throws IOException {
		if (this.screenSaverActive && screensaverOff ) {
			this.resetSreensaver();
			this.screenSaverActive = false;
		}
		if (this.currentScreen == null) {
			this.currentScreen = this.getRealScreen();
		}
		return this.currentScreen;
	}

	public SolvisScreen getRealScreen() throws IOException {
		SolvisScreen screen = new SolvisScreen(new MyImage(getConnection().getScreen()), this);
		return screen;
	}

	public boolean forceCurrentScreen(Screen current) throws IOException {
		this.getCurrentScreen();
		this.currentScreen.forceScreen(current);
		return true;
	}

	public void clearCurrentScreen() {
		this.currentScreen = null;
	}

	public SolvisMeasurements getMeasureData() throws IOException, ErrorPowerOn {
		SolvisMeasurements result = null;
		synchronized (this.solvisMeasureObject) {
			if (this.measureData == null) {
				SolvisMeasurements measurements = this.connection.getMeasurements();
				String hexString = measurements.getHexString();
				this.measureData = new SolvisMeasurements(measurements.getTimeStamp(), hexString.substring(12));
				if (hexString.substring(0, 6).equals("000000")) {
					this.getSolvisState().remoteConnected();
					throw new ErrorPowerOn("Power on detected");
				}
			}
			result = this.measureData;
		}
		return result;
	}

	public void clearMeasuredData() {
		synchronized (this.solvisMeasureObject) {
			this.measureData = null;
		}
	}

	public Integer readUnsignedShortModbusData(ModbusAccess access) throws IOException {
		int[] result = this.connection.readModbus(access, 1);
		if (result == null) {
			return null;
		} else {
			return result[0];
		}
	}

	public boolean writeUnsignedShortModbusData(ModbusAccess access, int data) throws IOException {
		return this.connection.writeModbus(access, new int[] { data });
	}

	public Long readUnsignedIntegerModbusData(ModbusAccess access) throws IOException {
		int[] result = this.connection.readModbus(access, 2);
		if (result == null) {
			return null;
		} else {
			return (long) result[0] << 16L | (long) result[1];
		}
	}

	public boolean writeUnsignedIntegerModbusData(ModbusAccess access, long data) throws IOException {
		int[] writeData = new int[] { (int) (data >> 16L), (int) (data & 0xffffL) };
		return this.connection.writeModbus(access, writeData);
	}

	public void resetSreensaver() throws IOException, TerminationException {
		this.setScreenSaverActive(false);
		this.send(this.resetSceenSaver);
	}

	public void send(TouchPoint point) throws IOException, TerminationException {
		if (point == null) {
			logger.warn("TouchPoint is <null>, ignored");
		}
		this.getConnection().sendTouch(point.getCoordinate());
		AbortHelper.getInstance().sleep(point.getPushTime());
		this.getConnection().sendRelease();
		AbortHelper.getInstance().sleep(point.getReleaseTime());
		this.clearCurrentScreen();
	}

	public void sendBack() throws IOException, TerminationException {
		this.getConnection().sendButton(Button.BACK);
		AbortHelper.getInstance().sleep(this.solvisDescription.getDurations().get("WindowChange").getTime_ms());
		this.clearCurrentScreen();
	}

	public void gotoHome() throws IOException {
		this.getHistory().set(null);
		this.solvisDescription.getFallBack().execute(this);
	}

	public void init() throws IOException, XmlError, XMLStreamException {
		this.configurationMask = this.getGrafics().getConfigurationMask();

		synchronized (this.solvisMeasureObject) {
			this.getSolvisDescription().getChannelDescriptions().init(this);
		}
		SystemMeasurements oldMeasurements = this.backupHandler.getSystemMeasurements(this.unit.getId());
		this.getAllSolvisData().restoreSpecialMeasurements(oldMeasurements);
		this.worker.init();
		this.updateControlChannels();
		this.worker.start();
		this.getDistributor().register(this);
		this.getSolvisDescription().instantiate(this);
		this.registerScreenChangedByHumanObserver(new ObserverI<WatchDog.HumanAccess>() {
			
			@Override
			public void update(HumanAccess data, Object source) {
				Solvis.this.updateByScreenChange();
			}
		});
		this.measurementUpdateThread.start();
	}
	
	public void updateControlChannels() {
		this.getSolvisDescription().getChannelDescriptions().updateControlChannels(this);
	}

	public void updateByScreenChange() {
		this.getSolvisDescription().getChannelDescriptions().updateByScreenChange(this);
	}

	public void measure() throws IOException, ErrorPowerOn {
		synchronized (this.solvisMeasureObject) {
			this.getSolvisDescription().getChannelDescriptions().measure(this, this.getAllSolvisData());
		}
	}

	public void commandOptimization(boolean enable) {
		this.worker.commandOptimization(enable);
	}

	public void commandEnable(boolean enable) {
		this.worker.commandEnable(enable);
	}

	public void screenRestore(boolean enable) {
		this.worker.screenRestore(enable);
		;
	}

	public void execute(Command command) {
		this.worker.push(command);
	}

	public ChannelDescription getChannelDescription(String description) {
		return this.solvisDescription.getChannelDescriptions().get(description, this.getConfigurationMask());
	}

	public Duration getDuration(String id) {
		return this.solvisDescription.getDurations().get(id);
	}

	public void registerScreenChangedByHumanObserver(ObserverI<HumanAccess> observer) {
		this.screenChangedByHumanObserable.register(observer);
	}

	public void notifyScreenChangedByHumanObserver(HumanAccess humanAccess) {
		this.screenChangedByHumanObserable.notify(humanAccess);
	}

	public void saveScreen() throws IOException {
		Screen current = this.getCurrentScreen(false).get();
		if (current != null && !current.equals(SolvisScreen.get(this.savedScreen))) {
			logger.info("Screen <" + current.getId() + "> saved");
			this.savedScreen = this.getCurrentScreen();
		}

	}

	public void restoreScreen() throws IOException {
		Screen screen = SolvisScreen.get(this.savedScreen);
		if (screen != null) {
			screen.goTo(this);
			logger.info("Screen <" + screen.getId() + "> restored");
		}
	}

	/**
	 * @return the allSolvisData
	 */
	public AllSolvisData getAllSolvisData() {
		return this.allSolvisData;
	}

	/**
	 * @param screenSaver the screenSaver to set
	 */
	public void setScreenSaverActive(boolean screenSaverActive) {
		if (this.screenSaverActive != screenSaverActive) {
			if (screenSaverActive) {
				logger.info("Screen saver detected");
			} else {
				logger.info("Screen saver finished");
			}
		}
		this.screenSaverActive = screenSaverActive;
	}

	public boolean isScreenSaverActive() {
		return this.screenSaverActive;
	}

	public SystemGrafics getGrafics() {
		return this.grafics;
	}

	public void setGrafics(SystemGrafics grafics) {
		this.grafics = grafics;
	}

	public void learning() throws IOException, LearningError {
		Reference<Screen> currentRef = new Reference<Screen>();
		this.getGrafics().clear();
		logger.log(LEARN, "Learning started.");
		this.initConfigurationMask(currentRef);
		logger.log(LEARN, "Configuration mask: 0x" + Integer.toHexString(this.configurationMask));
		this.getGrafics().setConfigurationMask(this.configurationMask);
		Screen home = this.getHomeScreen();
		if (home == null) {
			throw new AssertionError("Assign error: Screen description of <"
					+ this.solvisDescription.getScreens().getHomeId() + "> not found.");
		}
		if (!home.isLearned(this)) {
			this.gotoHome();
			this.forceCurrentScreen(home);
		} else {
			home.goTo(this);
		}
		Screen.learnScreens(this);
		this.solvisDescription.getClock().learn(this);
		this.solvisDescription.getChannelDescriptions().learn(this);
		this.getHomeScreen().goTo(this);
		logger.log(LEARN, "Learning finished.");
	}

	/**
	 * @return the solvisDescription
	 */
	public SolvisDescription getSolvisDescription() {
		return this.solvisDescription;
	}

	public void backupMeasurements(SystemMeasurements system) {
		this.allSolvisData.backupSpecialMeasurements(system);

	}

	public String getType() {
		return this.type;
	}

	public Unit getUnit() {
		return this.unit;
	}

	public void registerObserver(Observer.ObserverI<SolvisData> observer) {
		this.allSolvisData.registerObserver(observer);
	}

	public Distributor getDistributor() {
		return this.distributor;
	}

	public SolvisState getSolvisState() {
		return this.solvisState;
	}

	public int getConfigurationMask() {
		return this.configurationMask;
	}

	public SolvisConnection getConnection() {
		return this.connection;
	}

	private class MeasurementUpdateThread extends Thread {

		private int updateInterval;
		private int doubleUpdateInterval;
		private boolean abort = false;
		private boolean power = false;
		private boolean singleUpdate = false;

		public MeasurementUpdateThread(Unit unit) {
			super("MeasurementUpdateThread");
			this.updateInterval = unit.getForcedUpdateInterval_ms();
			this.doubleUpdateInterval = unit.getDoubleUpdateInterval_ms();
			Solvis.this.solvisState.register(new PowerObserver());
		}

		private class PowerObserver implements ObserverI<SolvisState> {

			@Override
			public void update(SolvisState data, Object source) {
				switch (data.getState()) {
					case SOLVIS_CONNECTED:
						MeasurementUpdateThread.this.power = true;
						break;
					case POWER_OFF:
						MeasurementUpdateThread.this.power = false;
						MeasurementUpdateThread.this.triggerUpdate();
				}

			}

		}

		@Override
		public void run() {

			while (!this.abort && this.updateInterval != 0) {

				try {

					Calendar midNight = Calendar.getInstance();
					long now = System.currentTimeMillis();
					midNight.setTimeInMillis(now - this.doubleUpdateInterval / 2);
					midNight.set(Calendar.HOUR_OF_DAY, 0);
					midNight.set(Calendar.MINUTE, 0);
					midNight.set(Calendar.SECOND, 0);
					midNight.set(Calendar.MILLISECOND, 0);

					long midNightLong = midNight.getTimeInMillis();
					long nextUpdate = (now - midNightLong) / this.updateInterval * this.updateInterval + midNightLong
							+ this.updateInterval - this.doubleUpdateInterval / 2;

					int[] waitTimes = new int[2];
					waitTimes[0] = (int) (nextUpdate - now);
					waitTimes[1] = this.doubleUpdateInterval;

					boolean single = false;
					for (int waitTime : waitTimes) {
						if (waitTime > 0 && !single) {
							boolean update = false;
							synchronized (this) {
								try {
									this.wait(waitTime);
								} catch (InterruptedException e) {
								}
								update = !this.abort && this.power || this.singleUpdate;
								single = this.singleUpdate;
								this.singleUpdate = false;
							}
							if (update) {
								Solvis.this.distributor.sendCollection(getAllSolvisData().getMeasurements());
							}
						}
					}
				} catch (Throwable e) {
					logger.error("Error was thrown in measurement update thread. Cause: ", e);
					AbortHelper.getInstance().sleep(Constants.WAIT_TIME_AFTER_THROWABLE);
				}
			}
		}

		public synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}

		public synchronized void triggerUpdate() {
			this.singleUpdate = true;
			this.notifyAll();
		}

	}

	public int getMaxResponseTime() {
		return this.connection.getMaxResponseTime();
	}

	public void registerAbortObserver(ObserverI<Boolean> observer) {
		this.abortObservable.register(observer);
	}

	public void abort() {
		this.abortObservable.notify(true);
		this.measurementUpdateThread.abort();
	}

	public String getTimeZone() {
		return this.timeZone;
	}

	public int getDefaultReadMeasurementsInterval_ms() {
		return this.defaultReadMeasurementsInterval_ms;
	}

	private void initConfigurationMask(Reference<Screen> current) throws TerminationException {
		boolean connected = false;
		this.configurationMask = 0;
		long time = -1;
		long lastErrorOutTime = -1;
		while (!connected) {
			time = System.currentTimeMillis();
			try {
				this.configurationMask = this.getSolvisDescription().getConfigurations(this, current);
				if (DEBUG_TWO_STATIONS) {
					this.configurationMask |= 0x00000003;
				}
				connected = true;
			} catch (IOException e) {
				time = System.currentTimeMillis();
				if (time - 120000 > lastErrorOutTime) {
					String message = e.getMessage();
					if (message.contains("timed")) {
						logger.error("Solvis not available. Powered down or wrong IP address. Will try again.");
					} else if (message.contains("too many")) {
						logger.error(
								"Solvis not available. The password or account name may not be correct. Will try again.");
					} else {
						logger.error(
								"Solvis not available. Powered down or wrong IP address. Will try again. Java-Error-Message:\n",
								e);
					}
				}
				lastErrorOutTime = time;
			}
			AbortHelper.getInstance().sleep(1000);

		}

	}

	public int getTimeAfterLastSwitchingOn() {
		long time = System.currentTimeMillis();
		long timeOfLastSwitchingOn = this.solvisState.getTimeOfLastSwitchingOn();
		if (timeOfLastSwitchingOn > 0 && this.delayAfterSwitchingOnEnable) {
			return (int) (time - timeOfLastSwitchingOn);
		} else {
			return 0x7fffffff;
		}

	}

	public Screen getHomeScreen() {
		if (this.home == null) {
			String homeId = this.solvisDescription.getScreens().getHomeId();
			this.home = this.solvisDescription.getScreens().get(homeId, this.getConfigurationMask());
		}
		return this.home;
	}

	public void serviceReset() {
		this.worker.serviceReset();
	}

	public History getHistory() {
		return this.history;
	}

}
