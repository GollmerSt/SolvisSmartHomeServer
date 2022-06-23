/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLStreamException;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.Constants.Debug;
import de.sgollmer.solvismax.connection.Distributor;
import de.sgollmer.solvismax.connection.SolvisConnection;
import de.sgollmer.solvismax.connection.SolvisConnection.Button;
import de.sgollmer.solvismax.connection.SolvisConnection.SolvisMeasurements;
import de.sgollmer.solvismax.connection.mqtt.Mqtt;
import de.sgollmer.solvismax.connection.transfer.SolvisStatePackage;
import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.ObserverException;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.model.HumanAccess.Status;
import de.sgollmer.solvismax.model.SolvisState.SolvisErrorInfo;
import de.sgollmer.solvismax.model.WatchDog.Event;
import de.sgollmer.solvismax.model.command.Command;
import de.sgollmer.solvismax.model.command.CommandControl;
import de.sgollmer.solvismax.model.command.CommandObserver;
import de.sgollmer.solvismax.model.objects.AllChannelDescriptions.MeasureMode;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.Duration;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.Standby;
import de.sgollmer.solvismax.model.objects.SystemGrafics;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.backup.BackupHandler;
import de.sgollmer.solvismax.model.objects.backup.SystemBackup;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.data.SolvisData.SmartHomeData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen.GotoStatus;
import de.sgollmer.solvismax.model.objects.screen.History;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.model.objects.unit.Feature;
import de.sgollmer.solvismax.model.objects.unit.Features;
import de.sgollmer.solvismax.model.objects.unit.Unit;
import de.sgollmer.solvismax.model.update.UpdateStrategies;
import de.sgollmer.solvismax.objects.Coordinate;

public class Solvis {

	private static final boolean DEBUG_POWER_ON = Constants.Debug.SOLVIS_RESULT_NULL;

	// private static final org.slf4j.Logger logger =
	// LoggerFactory.getLogger(Solvis.class);
	private static final ILogger logger = LogManager.getInstance().getLogger(Solvis.class);
	private static final Level LEARN = Level.getLevel("LEARN");
	private final SolvisState solvisState;
	private final HumanAccess humanAccess;
	private final SolvisDescription solvisDescription;
	private final AllSolvisData allSolvisData;
	private final SystemGrafics grafics;
	private int learningPictureIndex = 0;

	private SolvisWorkers worker;

	private final int echoInhibitTime_ms;
	private long configurationMask = 0;
	private SolvisScreen currentScreen = null;
	private AbstractScreen previousScreen = null;
	private AbstractScreen savedScreen = null;
	private AbstractScreen defaultScreen = null;
	private Screen home = null;
	private SolvisMeasurements measureData = null;
	private boolean screenSaverActive = false;
	private final TouchPoint resetSceenSaver;
	private final SolvisConnection connection;
	private final Mqtt mqtt;
	private final BackupHandler backupHandler;
	private final Distributor distributor;
	private final MeasurementUpdateThread measurementUpdateThread;
	private final Observable<Boolean> abortObservable = new Observable<>();
	private final Observable<SolvisErrorInfo> solvisErrorObservable = new Observable<>();
	private final String timeZone;
	private final boolean delayAfterSwitchingOnEnable;
	private final Unit unit;
	private final File writePath;
	private final History history = new History();
	private final boolean mustLearn;
	private boolean learning = false;
	private boolean initialized = false;
	private Object solvisMeasureObject = new Object();
	// private HumanAccess.Status humanAccess = HumanAccess.Status.NONE;
	private Map<String, Collection<UpdateStrategies.IExecutable>> updateStrategies = new HashMap<>();
	private Standby.Executable standby;
	private ZipOutputStream zipOutputStream = null;

	Solvis(final Unit unit, final SolvisDescription solvisDescription, final SystemGrafics grafics,
			final SolvisConnection connection, final Mqtt mqtt, final BackupHandler measurementsBackupHandler,
			final String timeZone, final int echoInhibitTime_ms, final File writePath, final boolean mustLearn) {
		this.unit = unit;
		this.humanAccess = new HumanAccess(this);
		this.allSolvisData = new AllSolvisData(this);
		this.solvisState = new SolvisState(this);
		this.echoInhibitTime_ms = echoInhibitTime_ms;
		this.solvisDescription = solvisDescription;
		this.resetSceenSaver = solvisDescription.getSaver().getResetScreenSaver();
		this.grafics = grafics;
		this.connection = connection;
		this.connection.setSolvisState(this.solvisState);
		this.mqtt = mqtt;
		this.allSolvisData.setAverageCount(unit.getDefaultAverageCount());
		this.allSolvisData.setMeasurementHysteresisFactor(unit.getMeasurementHysteresisFactor());
		this.worker = new SolvisWorkers(this);
		this.backupHandler = measurementsBackupHandler;
		this.backupHandler.register(this, unit.getId());
		this.distributor = new Distributor(this);
		this.measurementUpdateThread = new MeasurementUpdateThread(unit);
		this.timeZone = timeZone;
		this.delayAfterSwitchingOnEnable = unit.isDelayAfterSwitchingOnEnable();
		this.writePath = writePath;
		this.mustLearn = mustLearn;

		this.humanAccess.register(new IObserver<HumanAccess.Status>() {

			@Override
			public void update(Status data, Object source) {
				// this.humanAccess = humanAccess;
				switch (data) {
					case SERVICE:
					case USER:
						Solvis.this.previousScreen = null;
						break;
				}
			}

		});
	}

	void setCurrentScreen(final SolvisScreen screen) {
		this.currentScreen = screen;
	}

	public SolvisScreen getCurrentScreen() throws IOException, TerminationException {
		return this.getCurrentScreen(true);
	}

	SolvisScreen getCurrentScreen(final boolean screensaverOff) throws IOException, TerminationException {
		if (this.screenSaverActive && (screensaverOff || this.currentScreen == null)) {
			this.resetScreensaver();
		}
		if (this.currentScreen == null) {
			this.currentScreen = this.getRealScreen();
		}
		return this.currentScreen;
	}

	private SolvisScreen getRealScreen() throws IOException, TerminationException {
		SolvisScreen screen = new SolvisScreen(new MyImage(getConnection().getScreen()), this);
		return screen;
	}

	private boolean forceCurrentScreen(final Screen current) throws IOException, TerminationException {
		this.getCurrentScreen();
		this.currentScreen.forceScreen(current);
		return true;
	}

	public void clearCurrentScreen() {
		this.currentScreen = null;
	}

	public SolvisMeasurements getMeasureData() throws IOException, PowerOnException, TerminationException {
		SolvisMeasurements result = null;
		synchronized (this.solvisMeasureObject) {
			if (this.measureData == null) {
				SolvisMeasurements measurements = this.connection.getMeasurements();
				String hexString = measurements.getHexString();
				this.measureData = new SolvisMeasurements(measurements.getTimeStamp(), hexString.substring(12));
				if (hexString.substring(0, 6).equals("000000") || DEBUG_POWER_ON) {
					this.getSolvisState().setSolvisDataValid(false);
					throw new PowerOnException("Power on detected");
				} else {
					this.getSolvisState().setSolvisDataValid(true);
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

	private void resetScreensaver() throws IOException, TerminationException {
		this.setScreenSaverActive(false);
		this.send(this.resetSceenSaver);
	}

	public void send(final TouchPoint point) throws IOException, TerminationException {
		if (point == null) {
			logger.warn("TouchPoint is <null>, ignored");
			return;
		}
		this.send(point.getCoordinate(), point.getPushTime(this), point.getReleaseTime(this));
	}

	public void send(final Coordinate coord, final String pushTimeId, final String releaseTimeId)
			throws IOException, TerminationException {

		Duration push = this.getDuration(pushTimeId);
		Duration release = this.getDuration(releaseTimeId);

		if (push == null || release == null) {
			logger.error("Push time or release time isn't defined, ignored");
			return;
		}

		this.send(coord, push.getTime_ms(), release.getTime_ms());

	}

	private void send(final Coordinate coord, final Integer pushTime, final Integer releaseTime)
			throws IOException, TerminationException {

		if (pushTime == null || releaseTime == null) {
			logger.error("Push time or release time isn't defined, ignored");
			return;
		}

		this.getConnection().sendTouch(coord);
		try {
			Thread.sleep(Constants.MIN_TOUCH_TIME);
		} catch (InterruptedException e1) {
		}
		try {
			AbortHelper.getInstance().sleep(pushTime - Constants.MIN_TOUCH_TIME);
		} catch (TerminationException e) {
			this.getConnection().sendRelease();
			throw e;
		}
		this.getConnection().sendRelease();
		AbortHelper.getInstance().sleep(releaseTime);
		this.clearCurrentScreen();
	}

	public void sendBack() throws IOException, TerminationException {
		this.getConnection().sendButton(Button.BACK);
		AbortHelper.getInstance().sleep(this.getDuration("WindowChange").getTime_ms());
		this.clearCurrentScreen();
	}

	public void gotoHome() throws IOException, TerminationException {
		this.gotoHome(false);
		this.setPreviousScreen(this.getHomeScreen());
	}

	public void gotoHome(final boolean lastChance) throws IOException, TerminationException {
		this.getHistory().set(null);
		this.solvisDescription.getFallBack().execute(this, lastChance);
	}

	private class UpdateControlAfterPowerOn implements IObserver<SolvisStatePackage> {
		private boolean powerOff = true;

		@Override
		public void update(final SolvisStatePackage data, final Object source) {
			boolean update = false;
			synchronized (this) {
				SolvisStatus state = data.getState();
				if (this.powerOff && state == SolvisStatus.SOLVIS_CONNECTED) {
					this.powerOff = false;
					update = true;
				} else if (state == SolvisStatus.POWER_OFF) {
					this.powerOff = true;
				}
			}
			if (update) {
				Solvis.this.updateControlChannels();
			}
		}

	}

	void init() throws IOException, XMLStreamException, AssignmentException, AliasException, TypeException {
		Long forcedMask = this.getUnit().getForcedConfigMask();
		if (forcedMask == null) {
			this.configurationMask = this.getGrafics().getConfigurationMask();
		} else {
			this.configurationMask = forcedMask;
		}

		synchronized (this.solvisMeasureObject) {
			this.getSolvisDescription().getChannelDescriptions().init(this);
		}
		this.standby = this.getSolvisDescription().getStandby().instantiate(this);
		SystemBackup oldMeasurements = this.backupHandler.getSystemBackup(this.unit.getId());
		this.worker.init();
		this.getUnit().getChannelOptions().initialize(this);
		this.getAllSolvisData().restoreFromBackup(oldMeasurements);
		this.getUnit().getChannelOptions().setFixValues(this);
		this.getSolvisDescription().instantiate(this);
		this.initialized = true;
	}

	void start() {
		this.worker.start();
		this.getDistributor().register();
		UpdateControlAfterPowerOn updateControlAfterPowerOn = new UpdateControlAfterPowerOn();
		this.solvisState.register(updateControlAfterPowerOn);
		updateControlAfterPowerOn.update(this.solvisState.getSolvisStatePackage(), null);
		this.registerScreenChangedByHumanObserver(new IObserver<HumanAccess.Status>() {

			@Override
			public void update(HumanAccess.Status data, Object source) {
				if (data == HumanAccess.Status.NONE && Solvis.this.getUnit().getFeatures().isUpdateAfterUserAccess()) {
					Solvis.this.getSolvisDescription().getChannelDescriptions()
							.updateByHumanAccessFinished(Solvis.this);
				}
			}
		});
		this.measurementUpdateThread.start();
	}

	public void updateControlChannels() {
		this.getSolvisDescription().getChannelDescriptions().updateControlChannels(this);
	}

	public void updateReadOnlyControlChannels() {
		this.getSolvisDescription().getChannelDescriptions().updateReadOnlyControlChannels(this);
	}

	void measure(final MeasureMode mode)
			throws IOException, PowerOnException, TerminationException, NumberFormatException, TypeException {
		synchronized (this.solvisMeasureObject) {
			this.getSolvisDescription().getChannelDescriptions().measure(this, this.getAllSolvisData(), mode);
		}
	}

	public void commandOptimization(final boolean enable) {
		this.worker.commandOptimization(enable);
	}

	public interface CommandEnable {
		public void commandEnable(boolean enable);
	}

	public void controlEnable(final boolean enable) {
		this.worker.controlEnable(enable);
	}

	public void screenRestore(final boolean enable, final Object service) {
		this.worker.screenRestore(enable, service);
	}

	public void execute(final Command command) {
		this.worker.push(command);
	}

	/**
	 * 
	 * 
	 * @param description
	 * @param singleData
	 * 
	 * @return True: Command was ignored to prevent feedback loops
	 * @throws TypeException
	 */
	public boolean setFromExternal(final ChannelDescription description, final SingleData<?> singleData)
			throws TypeException {

		boolean ignored;

		if (singleData == null) {
			return true;
		}

		long receiveTimeStamp = singleData.getTimeStamp();

		if (receiveTimeStamp <= 0) {
			logger.error("Data received without timestamp, channel: " + description + ". Current time used.");
			receiveTimeStamp = System.currentTimeMillis();
		}

		SolvisData current = this.getAllSolvisData().get(description);
		SmartHomeData smartHomeData = current.getSmartHomeData();
		long currentTimeStamp;
		if (smartHomeData == null) {
			logger.error("SmartHomeData of channel <" + description + "> not defined.");
			currentTimeStamp = 0;
		} else {
			currentTimeStamp = smartHomeData.getTransmittedTimeStamp();
		}
		if (currentTimeStamp + this.getEchoInhibitTime_ms() < receiveTimeStamp
				|| !singleData.equals(current.getSingleData())) {
			this.execute(new CommandControl(description, singleData, this));
			ignored = false;
		} else {
			ignored = true;
		}
		return ignored;
	}

	public ChannelDescription getChannelDescription(final String id) {
		SolvisData data = this.allSolvisData.get(id);
		if (data == null) {
			return null;
		}
		return data.getDescription();
	}

	public Duration getDuration(final String id) {
		Duration duration = this.unit.getDuration(id);
		if (duration == null) {
			duration = this.solvisDescription.getDuration(id);
		}
		return duration;
	}

	public void registerScreenChangedByHumanObserver(final IObserver<HumanAccess.Status> observer) {
		this.humanAccess.register(observer);
	}

	public void registerAllSettingsDoneObserver(final IObserver<SolvisStatus> observer) {
		this.worker.registerAllSettingsDoneObserver(observer);
	}

	public HumanAccess getHumanAccess() {
		return this.humanAccess;
	}

	public SolvisStatePackage getHumanAccessPackage() {
		return new SolvisStatePackage(this.humanAccess.getStatus().getStatus(), this);
	}

	public SolvisStatePackage getSettingsPackage() {
		return new SolvisStatePackage(this.worker.getSettingStatus(), this);
	}

	public void registerSolvisErrorObserver(final IObserver<SolvisErrorInfo> observer) {
		this.solvisErrorObservable.register(observer);
	}

	public void registerControlExecutingObserver(final IObserver<Boolean> observer) {
		this.worker.registerControlExecutingObserver(observer);
	}

	public Collection<ObserverException> notifySolvisErrorObserver(final SolvisErrorInfo info, final Object source) {
		return this.solvisErrorObservable.notify(info, source);
	}

	void saveScreen() throws IOException, TerminationException {
		if (this.defaultScreen == null) {
			AbstractScreen current = SolvisScreen.get(this.getCurrentScreen(false));
			if (current == null) {
				current = SolvisScreen.get(this.getCurrentScreen());
			}
			if (current == null) {
				current = this.getHomeScreen();
			}
			if (current != this.savedScreen) {
				logger.info("Screen <" + current.getId() + "> saved");
				this.savedScreen = current;
			}
		}
	}

	void restoreScreen() throws IOException, TerminationException {

		AbstractScreen screen = this.defaultScreen == null ? this.savedScreen : this.defaultScreen;

		if (screen != null) {
			if (screen.isNoRestore()) {
				screen = this.defaultScreen == null ? this.getHomeScreen() : this.defaultScreen;
			}
			if (screen != SolvisScreen.get(this.getCurrentScreen(false))) {
				if (screen.goTo(this) == GotoStatus.CHANGED) {
					logger.info("Screen <" + screen.getId() + "> restored.");
				}
			}
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
	void setScreenSaverActive(final boolean screenSaverActive) {
		if (this.screenSaverActive != screenSaverActive) {
			if (screenSaverActive) {
				logger.debug("Screen saver detected");
			} else {
				logger.debug("Screen saver finished");
			}
		}
		this.screenSaverActive = screenSaverActive;
	}

	public SystemGrafics getGrafics() {
		return this.grafics;
	}

	void learning(final boolean force) throws IOException, LearningException, TerminationException {
		if (this.mustLearn || force) {
			this.learning = true;
			this.getGrafics().clear();
			logger.log(LEARN, "Learning initialized.");
			this.initConfigurationMask();
			logger.log(LEARN, "Configuration mask: 0x" + Long.toHexString(this.configurationMask));
			this.getGrafics().setConfigurationMask(this.configurationMask);
			this.getGrafics().setBaseConfigurationMask(
					this.getUnit().getConfiguration().getConfigurationMask(this.solvisDescription));
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
			boolean success = false;
			for (int cnt = 0; cnt < Constants.FAIL_REPEATS && !success; --cnt) {
				try {
					this.getHomeScreen().goTo(this);
					success = true;
				} catch (IOException e) {
				}
			}
			logger.log(LEARN, "Learning finished.");
			this.learning = false;

			this.closeZip();
		}
	}

	/**
	 * @return the solvisDescription
	 */
	public SolvisDescription getSolvisDescription() {
		return this.solvisDescription;
	}

	public void backupMeasurements(final SystemBackup system) {
		this.allSolvisData.saveToBackup(system);

	}

	public Unit getUnit() {
		return this.unit;
	}

	public Features getFeatures() {
		return this.unit.getFeatures();
	}

	public void registerSmartHomeObserver(final Observer.IObserver<SmartHomeData> observer) {
		this.allSolvisData.register(observer);
	}

	public Distributor getDistributor() {
		return this.distributor;
	}

	public SolvisState getSolvisState() {
		return this.solvisState;
	}

	public long getConfigurationMask() {
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

		private MeasurementUpdateThread(final Unit unit) {
			super("MeasurementUpdateThread");
			this.updateInterval = unit.getForcedUpdateInterval_ms();
			this.doubleUpdateInterval = unit.getDoubleUpdateInterval_ms();
			Solvis.this.solvisState.register(new PowerObserver());
		}

		private class PowerObserver implements IObserver<SolvisStatePackage> {

			@Override
			public void update(final SolvisStatePackage data, final Object source) {
				SolvisStatus state = data.getState();
				switch (state) {
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
								update = !this.abort && (this.power || this.singleUpdate);
								single = this.singleUpdate;
								this.singleUpdate = false;
							}
							if (update) {
								Solvis.this.distributor.sendCollection(getAllSolvisData().getMeasurementsForUpdate());
							}
						}
					}
				} catch (Throwable e) {
					logger.error("Error was thrown in measurement update thread. Cause: ", e);
					try {
						AbortHelper.getInstance().sleep(Constants.WAIT_TIME_AFTER_THROWABLE);
					} catch (TerminationException e1) {
						return;
					}
				}
			}
		}

		private synchronized void abort() {
			this.abort = true;
			this.notifyAll();
		}

		private synchronized void triggerUpdate() {
			this.singleUpdate = true;
			this.notifyAll();
		}

	}

	public int getMaxResponseTime() {
		return this.connection.getMaxResponseTime();
	}

	public void registerAbortObserver(final IObserver<Boolean> observer) {
		this.abortObservable.register(observer);
	}

	public void abort() {
		try {
			this.closeZip();
		} catch (IOException e) {
		}
		this.abortObservable.notify(true);
		this.measurementUpdateThread.abort();
	}

	@SuppressWarnings("unused")
	private String getTimeZone() {
		return this.timeZone;
	}

	private void initConfigurationMask() throws TerminationException, LearningException {
		boolean connected = false;
		this.configurationMask = 0; // must be zero! In case of learning HomeScreen and Info-Screen
		long time = -1;
		long lastErrorOutTime = -1;
		while (!connected) {
			time = System.currentTimeMillis();
			try {
				this.configurationMask = this.getSolvisDescription().getConfigurationFromGui(this);
				this.configurationMask |= this.getUnit().getConfiguration()
						.getConfigurationMask(this.solvisDescription);
				if (Debug.DEBUG_TWO_STATIONS) {
					this.configurationMask |= 0x00000003;
				}
				connected = true;
			} catch (IOException e) {
				time = System.currentTimeMillis();
				if (time - 120000 > lastErrorOutTime) {
					String message = e.getMessage();
					if (message.contains("timed")) {
						logger.error("Solvis not available. Powered down or wrong IP address." + " Will try again.");
						logger.info("Java error message: ", e);
					} else if (message.contains("too many")) {
						logger.error("Solvis not available. The password or account name"
								+ " may not be correct. Will try again.");
						logger.info("Java error message: ", e);
					} else {
						logger.error("Solvis not available. Powered down or wrong IP address."
								+ " Will try again. Java-Error-Message:\n", e);
					}
				}
				lastErrorOutTime = time;
			}
			if (!connected) {
				AbortHelper.getInstance().sleep(1000);
			}

		}

	}

	public int getTimeAfterLastSwitchingOn() {
		long timeOfLastSwitchingOn = this.solvisState.getTimeOfLastSwitchingOn();
		if (timeOfLastSwitchingOn > 0 && this.delayAfterSwitchingOnEnable) {
			long time = System.currentTimeMillis();
			return (int) (time - timeOfLastSwitchingOn);
		} else {
			return 0x7fffffff;
		}

	}

	public Screen getHomeScreen() {
		if (this.home == null) {
			String homeId = this.solvisDescription.getScreens().getHomeId();
			this.home = (Screen) this.solvisDescription.getScreens().get(homeId, this);
		}
		return this.home;
	}

	public void serviceAccess(Event event) throws IOException, TerminationException {
		this.humanAccess.serviceAccess(event);
	}

	public History getHistory() {
		return this.history;
	}

	private SolvisScreen lastRealScreen = null;

	static class SynchronizedScreenResult {
		private final boolean changed;
		private final SolvisScreen screen;

		static SolvisScreen getScreen(final SynchronizedScreenResult result) {
			if (result == null) {
				return null;
			} else {
				return result.screen;
			}
		}

		private SynchronizedScreenResult(final boolean changed, final SolvisScreen screen) {
			this.changed = changed;
			this.screen = screen;
		}

		boolean isChanged() {
			return this.changed;
		}

	}

	SynchronizedScreenResult getSyncronizedRealScreen() throws IOException, TerminationException {

		SolvisScreen lastRealScreen = this.lastRealScreen;
		boolean isSynchronized = false;
		boolean changed = false;
		for (int cnt = 0; !isSynchronized && cnt < 3; ++cnt) {
			SolvisScreen realScreen = this.getRealScreen();

			if (realScreen.imagesEquals(lastRealScreen)) {
				if (changed) {
					changed = !realScreen.imagesEquals(this.lastRealScreen);
				}
				if (changed) {
					this.lastRealScreen = lastRealScreen;
				}
				return new SynchronizedScreenResult(changed, this.lastRealScreen);
			} else {
				changed = true;
				lastRealScreen = realScreen;
			}
			AbortHelper.getInstance().sleep(Constants.WAIT_AFTER_FIRST_ASYNC_DETECTION);
		}
		return null;
	}

	private int getEchoInhibitTime_ms() {
		return this.echoInhibitTime_ms;
	}

	public File getWritePath() {
		return this.writePath;
	}

	public boolean willBeModified(final SolvisData data) {
		return this.worker.willBeModified(data);
	}

	public String getLearningPictureIndex() {
		return String.format("%03d", ++this.learningPictureIndex);
	}

	public void writeLearningImage(final SolvisScreen solvisScreen, final String id) {
		this.writeLearningImage(SolvisScreen.getImage(solvisScreen), id);
	}

	public void writeLearningImage(final MyImage image, final String id) {
		if (image == null) {
			return;
		}
		File parent = new File(this.getWritePath(), Constants.Files.RESOURCE_DESTINATION);
		parent = new File(parent, Constants.Files.LEARN_DESTINATION);
		String baseName = this.getLearningPictureIndex() + '_' + this.getUnit().getId() + "__" + id + "__";
		int cnt = 0;
		boolean found = true;
		File file = null;
		while (found) {
			String name = FileHelper.makeOSCompatible(baseName + Integer.toString(cnt) + ".png");
			file = new File(parent, name);
			found = file.exists();
			++cnt;
		}
		try {
			image.write(file);
			this.appendToZip(image, file);
		} catch (IOException e) {
			logger.error("Error on writing the image of the learned screen <" + id + ">.");
		}
	}

	private void appendToZip(MyImage image, File file) throws IOException {
		if (this.zipOutputStream == null) {
			File parent = new File(this.getWritePath(), Constants.Files.RESOURCE_DESTINATION);
			parent = new File(parent, Constants.Files.LEARN_DESTINATION);
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HHmmssSSS");
			String zipName = Constants.Files.ZIP_PREFIX + '_' + format.format(new Date(System.currentTimeMillis()))
					+ '_' + this.getUnit().getId() + ".zip";
			OutputStream out = new FileOutputStream(new File(parent, zipName));
			this.zipOutputStream = new ZipOutputStream(out);
		}
		ZipEntry zipEntry = new ZipEntry(file.getName());
		this.zipOutputStream.putNextEntry(zipEntry);
		image.write(this.zipOutputStream);
	}

	private void closeZip() throws IOException {
		if (this.zipOutputStream != null) {
			this.zipOutputStream.close();
			this.zipOutputStream = null;
		}
	}

	public boolean isAdmin() {
		return this.unit.isAdmin();
	}

	public boolean isFeature(final Feature feature) {
		Boolean value = this.unit.getFeatures().getFeature(feature.getId());
		if (value == null) {
			return false;
		}
		return value == feature.isSet();
	}

	public boolean isLearning() {
		return this.learning;
	}

	public void addFeatureDependency(final String id) {
		Boolean value = this.getFeatures().getFeature(id);
		value = value == null ? false : value;
		Feature feature = new Feature(id, value);
		this.getGrafics().add(feature);

	}

	public boolean isInitialized() {
		return this.initialized;
	}

	public void setDefaultScreen(final AbstractScreen defaultScreen) {
		this.defaultScreen = defaultScreen;
	}

	public AbstractScreen getPreviousScreen() {
		return this.previousScreen;
	}

	public void setPreviousScreen(final AbstractScreen previousScreen) {
		switch (this.humanAccess.getStatus()) {
			case UNKNOWN:
			case NONE:
				this.previousScreen = previousScreen;
				break;
		}
	}

	public void add(final UpdateStrategies.IExecutable executable) {
		String triggerId = executable.getTriggerId();

		Collection<UpdateStrategies.IExecutable> collection = this.updateStrategies.get(triggerId);
		if (collection == null) {
			collection = new ArrayList<>();
			this.updateStrategies.put(triggerId, collection);
		}
		collection.add(executable);
	}

	public Collection<UpdateStrategies.IExecutable> getUpdateStrategies(final String triggerId) {
		return this.updateStrategies.get(triggerId);
	}

	public boolean setStandby(final String standbyId)
			throws NumberFormatException, IOException, PowerOnException, TerminationException, TypeException {
		SolvisData data = this.getAllSolvisData().get(standbyId);
		if (data == null) {
			logger.error("Standby channel <" + standbyId + "> not definded. Setting ignored.");
			return false;
		}
		return this.standby.set(data);
	}

	public void resetStandby()
			throws NumberFormatException, IOException, PowerOnException, TerminationException, TypeException {
		this.standby.reset();
		;
	}

	public void updateByMonitoringTask(final CommandObserver.Status status, final Object source) {
		this.worker.updateByMonitoringTask(status, source);
	}

	public Mqtt getMqtt() {
		return this.mqtt;
	}

}
