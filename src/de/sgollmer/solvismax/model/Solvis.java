/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import javax.xml.stream.XMLStreamException;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.Distributor;
import de.sgollmer.solvismax.connection.SolvisConnection;
import de.sgollmer.solvismax.connection.SolvisConnection.Button;
import de.sgollmer.solvismax.connection.SolvisConnection.SolvisMeasurements;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.error.LearningException;
import de.sgollmer.solvismax.error.PowerOnException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.FileHelper;
import de.sgollmer.solvismax.helper.Helper.Reference;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.log.LogManager.Level;
import de.sgollmer.solvismax.model.SolvisState.SolvisErrorInfo;
import de.sgollmer.solvismax.model.WatchDog.HumanAccess;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.Duration;
import de.sgollmer.solvismax.model.objects.Feature;
import de.sgollmer.solvismax.model.objects.Features;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.IObserver;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.SystemGrafics;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.Units.Unit;
import de.sgollmer.solvismax.model.objects.backup.MeasurementsBackupHandler;
import de.sgollmer.solvismax.model.objects.backup.SystemMeasurements;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.History;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Coordinate;

public class Solvis {

	// private static final org.slf4j.Logger logger =
	// LoggerFactory.getLogger(Solvis.class);
	private static final ILogger logger = LogManager.getInstance().getLogger(Solvis.class);
	private static final Level LEARN = Level.getLevel("LEARN");
	private static final boolean DEBUG_TWO_STATIONS = false;

	private final SolvisState solvisState = new SolvisState(this);
	private final SolvisDescription solvisDescription;
	private final AllSolvisData allSolvisData;
	private final SystemGrafics grafics;
	private int learningPictureIndex = 0;

	private SolvisWorkers worker;

	private final String type;
	private final int defaultReadMeasurementsInterval_ms;
	private final int echoInhibitTime_ms;
	private int configurationMask = 0;
	private SolvisScreen currentScreen = null;
	private final Reference<AbstractScreen> previousScreen = new Reference<>(null);
	private AbstractScreen savedScreen = null;
	private AbstractScreen defaultScreen = null;
	private Screen home = null;
	private SolvisMeasurements measureData = null;
	private boolean screenSaverActive = false;
	private final TouchPoint resetSceenSaver;
	private final SolvisConnection connection;
	private final MeasurementsBackupHandler backupHandler;
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
	private Observer.Observable<HumanAccess> screenChangedByHumanObserable = new Observable<>();
	private Object solvisMeasureObject = new Object();
	private HumanAccess humanAccess = HumanAccess.NONE;

	Solvis(Unit unit, SolvisDescription solvisDescription, SystemGrafics grafics, SolvisConnection connection,
			MeasurementsBackupHandler measurementsBackupHandler, String timeZone, int echoInhibitTime_ms,
			File writePath, boolean mustLearn) {
		this.allSolvisData = new AllSolvisData(this);
		this.unit = unit;
		this.type = unit.getType();
		this.defaultReadMeasurementsInterval_ms = unit.getDefaultReadMeasurementsInterval_ms();
		this.echoInhibitTime_ms = echoInhibitTime_ms;
		this.solvisDescription = solvisDescription;
		this.resetSceenSaver = solvisDescription.getSaver().getResetScreenSaver();
		this.grafics = grafics;
		this.connection = connection;
		this.connection.setSolvisState(this.solvisState);
		this.allSolvisData.setAverageCount(unit.getDefaultAverageCount());
		this.allSolvisData.setMeasurementHysteresisFactor(unit.getMeasurementHysteresisFactor());
		this.worker = new SolvisWorkers(this);
		this.backupHandler = measurementsBackupHandler;
		this.backupHandler.register(this, unit.getId());
		this.distributor = new Distributor(unit);
		this.measurementUpdateThread = new MeasurementUpdateThread(unit);
		this.timeZone = timeZone;
		this.delayAfterSwitchingOnEnable = unit.isDelayAfterSwitchingOnEnable();
		this.writePath = writePath;
		this.mustLearn = mustLearn;
	}

	void setCurrentScreen(SolvisScreen screen) {
		this.currentScreen = screen;
	}

	public SolvisScreen getCurrentScreen() throws IOException, TerminationException {
		return this.getCurrentScreen(true);
	}

	SolvisScreen getCurrentScreen(boolean screensaverOff) throws IOException, TerminationException {
		if (this.screenSaverActive && screensaverOff) {
			this.resetSreensaver();
			this.screenSaverActive = false;
		}
		if (this.currentScreen == null) {
			this.currentScreen = this.getRealScreen();
		}
		return this.currentScreen;
	}

	private SolvisScreen getRealScreen() throws IOException, TerminationException {
		SolvisScreen screen = new SolvisScreen(new MyImage(getConnection().getScreen()), this, this.previousScreen);
		return screen;
	}

	private boolean forceCurrentScreen(Screen current) throws IOException, TerminationException {
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
				if (hexString.substring(0, 6).equals("000000")) {
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

	private void resetSreensaver() throws IOException, TerminationException {
		this.setScreenSaverActive(false);
		this.send(this.resetSceenSaver);
	}

	public void send(TouchPoint point) throws IOException, TerminationException {
		if (point == null) {
			logger.warn("TouchPoint is <null>, ignored");
			return;
		}
		this.send(point.getCoordinate(), point.getPushTime(), point.getReleaseTime());
	}

	public void send(Coordinate coord, int pushTime, int releaseTime) throws IOException, TerminationException {
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
		this.previousScreen.set(this.getHomeScreen());
	}

	public void gotoHome(boolean lastChance) throws IOException, TerminationException {
		this.getHistory().set(null);
		this.solvisDescription.getFallBack().execute(this, lastChance);
	}

	private class UpdateControlAfterPowerOn implements IObserver<SolvisState.State> {
		private boolean powerOff = true;

		@Override
		public void update(SolvisState.State data, Object source) {
			boolean update = false;
			synchronized (this) {
				SolvisState.State state = data;
				if (this.powerOff & state == SolvisState.State.SOLVIS_CONNECTED) {
					this.powerOff = false;
					update = true;
				} else if (state == SolvisState.State.POWER_OFF) {
					this.powerOff = true;
				}
			}
			if (update) {
				Solvis.this.updateControlChannels();
			}
		}

	}

	void init() throws IOException, XMLStreamException, AssignmentException, AliasException {
		this.configurationMask = this.getGrafics().getConfigurationMask();

		synchronized (this.solvisMeasureObject) {
			this.getSolvisDescription().getChannelDescriptions().init(this);
		}
		SystemMeasurements oldMeasurements = this.backupHandler.getSystemMeasurements(this.unit.getId());
		this.worker.init();
		this.getAllSolvisData().restoreSpecialMeasurements(oldMeasurements);
		this.worker.start();
		this.getDistributor().register(this);
		this.getSolvisDescription().instantiate(this);
		UpdateControlAfterPowerOn updateControlAfterPowerOn = new UpdateControlAfterPowerOn();
		this.solvisState.register(updateControlAfterPowerOn);
		updateControlAfterPowerOn.update(this.solvisState.getState(), null);
		this.registerScreenChangedByHumanObserver(new IObserver<WatchDog.HumanAccess>() {

			@Override
			public void update(HumanAccess data, Object source) {
				if (data == HumanAccess.NONE && Solvis.this.getUnit().getFeatures().isUpdateAfterUserAccess()) {
					Solvis.this.getSolvisDescription().getChannelDescriptions()
							.updateByHumanAccessFinished(Solvis.this);
				}
			}
		});
		this.measurementUpdateThread.start();
		this.initialized = true;
	}

	public void updateControlChannels() {
		this.getSolvisDescription().getChannelDescriptions().updateControlChannels(this);
	}

	public void updateReadOnlyControlChannels() {
		this.getSolvisDescription().getChannelDescriptions().updateReadOnlyControlChannels(this);
	}

	void measure() throws IOException, PowerOnException, TerminationException, NumberFormatException {
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

	void screenRestore(boolean enable) {
		this.worker.screenRestore(enable);
		;
	}

	public void execute(Command command) {
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
	public boolean setFromExternal(ChannelDescription description, SingleData<?> singleData) throws TypeException {

		boolean ignored;

		if (singleData == null) {
			return true;
		}

		SolvisData current = this.getAllSolvisData().get(description);
		if (current.getSentTimeStamp() + this.getEchoInhibitTime_ms() < System.currentTimeMillis()
				|| !singleData.equals(current.getSingleData())) {
			this.execute(new de.sgollmer.solvismax.model.CommandControl(description, singleData, this));
			ignored = false;
		} else {
			ignored = true;
		}
		return ignored;
	}

	public ChannelDescription getChannelDescription(String description) {
		return this.solvisDescription.getChannelDescriptions().get(description, this);
	}

	public Duration getDuration(String id) {
		return this.solvisDescription.getDuration(id);
	}

	public void registerScreenChangedByHumanObserver(IObserver<HumanAccess> observer) {
		this.screenChangedByHumanObserable.register(observer);
	}

	void notifyScreenChangedByHumanObserver(HumanAccess humanAccess) {
		this.humanAccess = humanAccess;
		this.screenChangedByHumanObserable.notify(humanAccess);
	}

	public HumanAccess getHumanAccess() {
		return this.humanAccess;
	}

	public void registerSolvisErrorObserver(IObserver<SolvisErrorInfo> observer) {
		this.solvisErrorObservable.register(observer);
	}

	public boolean notifySolvisErrorObserver(SolvisErrorInfo info, Object source) {
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
				screen = this.getHomeScreen();
			}
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
	private void setScreenSaverActive(boolean screenSaverActive) {
		if (this.screenSaverActive != screenSaverActive) {
			if (screenSaverActive) {
				logger.info("Screen saver detected");
			} else {
				logger.info("Screen saver finished");
			}
		}
		this.screenSaverActive = screenSaverActive;
	}

	public SystemGrafics getGrafics() {
		return this.grafics;
	}

	void learning(boolean force) throws IOException, LearningException, TerminationException {
		if (this.mustLearn || force) {
			this.learning = true;
			this.getGrafics().clear();
			logger.log(LEARN, "Learning started.");
			this.initConfigurationMask();
			logger.log(LEARN, "Configuration mask: 0x" + Integer.toHexString(this.configurationMask));
			this.getGrafics().setConfigurationMask(this.configurationMask);
			this.getGrafics().setBaseConfigurationMask(this.getUnit().getConfigOrMask());
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
		}
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

	public Features getFeatures() {
		return this.unit.getFeatures();
	}

	public void registerObserver(Observer.IObserver<SolvisData> observer) {
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

		private MeasurementUpdateThread(Unit unit) {
			super("MeasurementUpdateThread");
			this.updateInterval = unit.getForcedUpdateInterval_ms();
			this.doubleUpdateInterval = unit.getDoubleUpdateInterval_ms();
			Solvis.this.solvisState.register(new PowerObserver());
		}

		private class PowerObserver implements IObserver<SolvisState.State> {

			@Override
			public void update(SolvisState.State data, Object source) {
				switch (data) {
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

	public void registerAbortObserver(IObserver<Boolean> observer) {
		this.abortObservable.register(observer);
	}

	public void abort() {
		this.abortObservable.notify(true);
		this.measurementUpdateThread.abort();
	}

	@SuppressWarnings("unused")
	private String getTimeZone() {
		return this.timeZone;
	}

	public int getDefaultReadMeasurementsInterval_ms() {
		return this.defaultReadMeasurementsInterval_ms;
	}

	private void initConfigurationMask() throws TerminationException, LearningException {
		boolean connected = false;
		this.configurationMask = 0; // must be zero! In case of learning HomeScreen and Info-Screen
		long time = -1;
		long lastErrorOutTime = -1;
		while (!connected) {
			time = System.currentTimeMillis();
			try {
				this.configurationMask = this.getSolvisDescription().getConfigurations(this);
				this.configurationMask |= this.getUnit().getConfigOrMask();
				if (DEBUG_TWO_STATIONS) {
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
			AbortHelper.getInstance().sleep(1000);

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

	public void serviceReset() {
		this.worker.serviceReset();
	}

	public History getHistory() {
		return this.history;
	}

	private SolvisScreen lastRealScreen = null;

	static class SynchronizedScreenResult {
		private final boolean changed;
		private final SolvisScreen screen;

		static SolvisScreen getScreen(SynchronizedScreenResult result) {
			if (result == null) {
				return null;
			} else {
				return result.screen;
			}
		}

		private SynchronizedScreenResult(boolean changed, SolvisScreen screen) {
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

	public boolean willBeModified(SolvisData data) {
		return this.worker.willBeModified(data);
	}

	public String getLearningPictureIndex() {
		return String.format("%03d", ++this.learningPictureIndex);
	}

	public void writeLearningImage(SolvisScreen solvisScreen, String id) {
		this.writeLearningImage(SolvisScreen.getImage(solvisScreen), id);
	}

	public void writeLearningImage(MyImage image, String id) {
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
		} catch (IOException e) {
			logger.error("Error on writing the image of the learned screen <" + id + ">.");
		}
	}

	public boolean isAdmin() {
		return this.unit.getFeatures().isAdmin();
	}

	public boolean isFeature(Feature feature) {
		Boolean value = this.unit.getFeatures().getFeature(feature.getId());
		if (value == null) {
			return false;
		}
		return value == feature.isSet();
	}

	public boolean isLearning() {
		return this.learning;
	}

	public void addFeatureDependency(String id) {
		Boolean value = this.getFeatures().getFeature(id);
		value = value == null ? false : value;
		Feature feature = new Feature(id, value);
		this.getGrafics().add(feature);

	}

	public boolean isInitialized() {
		return this.initialized;
	}

	public void setDefaultScreen(AbstractScreen defaultScreen) {
		this.defaultScreen = defaultScreen;
	}

}
