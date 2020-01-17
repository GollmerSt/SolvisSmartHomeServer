/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.logging.slf4j.Log4jLogger;
//import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.connection.Distributor;
import de.sgollmer.solvismax.connection.SolvisConnection;
import de.sgollmer.solvismax.connection.SolvisConnection.Button;
import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.helper.Reference;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.Duration;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.ScreenLearnable.LearnScreen;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.SystemGrafics;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.Units.Unit;
import de.sgollmer.solvismax.model.objects.backup.MeasurementsBackupHandler;
import de.sgollmer.solvismax.model.objects.backup.SystemMeasurements;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class Solvis {

	// private static final org.slf4j.Logger logger =
	// LoggerFactory.getLogger(Solvis.class);
	private static final Logger logger = LogManager.getLogger(Solvis.class);
	private static final Level LEARN = Level.getLevel("LEARN");

	private final SolvisState solvisState = new SolvisState();
	private final SolvisDescription solvisDescription;
	private final AllSolvisData allSolvisData = new AllSolvisData(this);
	private SystemGrafics grafics;

	private SolvisWorkers worker;

	private final String id;
	private final String type;
	private final int defaultReadMeasurementsIntervall_ms;
	private int configurationMask = 0;
	private MyImage currentImage = null;
	private Screen currentScreen = null;
	private Screen savedScreen = null;
	private Screen home = null;
	private String measureData = null;
	private boolean screenSaverActive = false;
	private final TouchPoint resetSceenSaver;
	private final SolvisConnection connection;
	private final MeasurementsBackupHandler backupHandler;
	private final Distributor distributor;
	private final MeasurementUpdateThread measurementUpdateThread;
	private final Observable<Boolean> abortObservable = new Observable<>();
	private final String timeZone;
	private final boolean delayAfterSwitchingOnEnable;

	public Solvis(Unit unit, SolvisDescription solvisDescription, SystemGrafics grafics, SolvisConnection connection,
			MeasurementsBackupHandler measurementsBackupHandler, String timeZone) {
		this.id = unit.getId();
		this.type = unit.getType();
		this.defaultReadMeasurementsIntervall_ms = unit.getDefaultReadMeasurementsIntervall_ms();
		this.solvisDescription = solvisDescription;
		this.resetSceenSaver = solvisDescription.getSaver().getResetScreenSaver();
		this.grafics = grafics;
		this.connection = connection;
		this.connection.setSolvisState(this.solvisState);
		this.allSolvisData.setAverageCount(unit.getDefaultAverageCount());
		this.allSolvisData.setMeasurementHysteresisFactor(unit.getMeasurementHysteresisFactor());
		this.allSolvisData.setReadMeasurementInterval(unit.getDefaultReadMeasurementsIntervall_ms());
		this.worker = new SolvisWorkers(this);
		this.backupHandler = measurementsBackupHandler;
		this.backupHandler.register(this, id);
		this.distributor = new Distributor(unit.getBufferedIntervall_ms());
		this.measurementUpdateThread = new MeasurementUpdateThread(unit.getForcedUpdateIntervall_ms());
		this.timeZone = timeZone;
		this.delayAfterSwitchingOnEnable = unit.isDelayAfterSwitchingOnEnable();
	}

	private Observer.Observable<Boolean> screenChangedByUserObserable = new Observable<>();

	private Object solvisMeasureObject = new Object();

	public boolean isCurrentImageValid() {
		return this.currentImage != null;
	}

	public MyImage getCurrentImage() throws IOException {
		if (this.screenSaverActive) {
			this.resetSreensaver();
			this.screenSaverActive = false;
		}
		MyImage image = this.currentImage;
		if (image == null) {
			this.currentImage = this.getRealImage();
		}
		return this.currentImage;
	}

	public MyImage getRealImage() throws IOException {
		MyImage image = new MyImage(getConnection().getScreen());
		return image;
	}

	public Screen getCurrentScreen() throws IOException {
		if (this.screenSaverActive) {
			this.resetSreensaver();
			this.screenSaverActive = false;
		}
		Screen screen = this.currentScreen;
		if (screen == null) {
			screen = this.solvisDescription.getScreens().getScreen(this.getCurrentImage(), this);
			this.currentScreen = screen;
		}
		return screen;
	}

	public boolean forceCurrentScreen(Screen current) throws IOException {
		this.clearCurrentImage();
		this.currentScreen = current;
		return true;
	}

	public void clearCurrentImage() {
		this.currentImage = null;
		this.currentScreen = null;
	}

	public String getMeasureData() throws IOException, ErrorPowerOn {
		String result = null;
		synchronized (solvisMeasureObject) {
			if (this.measureData == null) {
				this.measureData = this.getConnection().getMeasurements().substring(12);
				if (this.measureData.substring(0, 6).equals("000000")) {
					this.getSolvisState().remoteConnected();
					throw new ErrorPowerOn("Power on detected");
				}
			}
			result = this.measureData;
		}
		return result;
	}

	public void clearMeasuredData() {
		synchronized (solvisMeasureObject) {
			this.measureData = null;
		}
	}

	public void resetSreensaver() throws IOException, TerminationException {
		this.setScreenSaverActive(false);
		this.send(resetSceenSaver);
	}

	public void send(TouchPoint point) throws IOException, TerminationException {
		if (point == null) {
			logger.warn("TouchPoint is <null>, ignored");
		}
		this.getConnection().sendTouch(point.getCoordinate());
		AbortHelper.getInstance().sleep(point.getPushTime());
		this.getConnection().sendRelease();
		AbortHelper.getInstance().sleep(point.getReleaseTime());
		this.clearCurrentImage();
	}

	public void sendBack() throws IOException, TerminationException {
		this.getConnection().sendButton(Button.BACK);
		AbortHelper.getInstance().sleep(this.solvisDescription.getDurations().get("WindowChange").getTime_ms());
		this.clearCurrentImage();
	}

	public void gotoHome() throws IOException {
		this.solvisDescription.getFallBack().execute(this);
	}

//	public boolean gotoScreen(Screen screen) throws IOException, TerminationException {
//		if (screen == null) {
//			return false;
//		}
//		if (this.getCurrentScreen() == null) {
//			this.gotoHome();
//		}
//
//		if (this.getCurrentScreen() == screen) {
//			return true;
//		}
//
//		if (screen == this.getSolvisDescription().getScreens().ge) {
//			this.gotoHome();
//			return true;
//		}
//
//		List<ScreenTouch> previousScreens = screen.getPreviosScreens(this.getConfigurationMask());
//
//		boolean gone = false;
//
//		for (int cnt = 0; !gone && cnt < Constants.FAIL_REPEATS; ++cnt) {
//
//			for (int gotoDeepth = 0; !gone
//					&& this.getCurrentScreen() != null & gotoDeepth < Constants.MAX_GOTO_DEEPTH; ++gotoDeepth) {
//
//				// && this.getCurrentScreen() != this.homeScreen) {
//				// ListIterator<Screen> it = null;
//				ScreenTouch foundScreenTouch = null;
//				for (Iterator<ScreenTouch> it = previousScreens.iterator(); it.hasNext();) {
//					ScreenTouch st = it.next();
//					Screen previous = st.getScreen();
//					if (previous == this.getCurrentScreen()) {
//						foundScreenTouch = st;
//						break;
//					}
//				}
//
//				if (foundScreenTouch == null) {
//					this.sendBack();
//				} else {
//					this.send(foundScreenTouch.getTouchPoint());
//				}
//
//				if (this.getCurrentScreen() == screen) {
//					gone = true;
//				}
//			}
//			if (!gone) {
//				this.gotoHome(); // try it from beginning
//				logger.info("Goto screen <" + screen.getId() + "> not succcessful. Will be retried.");
//			}
//		}
//		if (!gone) {
//			logger.error("Screen <" + screen.getId() + "> not found.");
//		}
//		return this.getCurrentScreen() == screen;
//
//	}

	public void init() throws IOException, XmlError, XMLStreamException {
		this.configurationMask = this.getGrafics().getConfigurationMask();
		synchronized (solvisMeasureObject) {
			this.getSolvisDescription().getChannelDescriptions().init(this, this.getAllSolvisData());
		}
		SystemMeasurements oldMeasurements = this.backupHandler.getSystemMeasurements(this.id);
		this.getAllSolvisData().restoreSpecialMeasurements(oldMeasurements);
		this.worker.start();
		this.getSolvisDescription().getChannelDescriptions().initControl(this);
		this.getDistributor().register(this);
		this.measurementUpdateThread.start();
		this.getSolvisDescription().getClock().instantiate(this);
	}

	public void measure() throws IOException, ErrorPowerOn {
		synchronized (solvisMeasureObject) {
			this.getSolvisDescription().getChannelDescriptions().measure(this, this.getAllSolvisData());
		}
	}

	public void execute(CommandI command) {
		this.worker.push(command);
	}

	public ChannelDescription getChannelDescription(String description) {
		return this.solvisDescription.getChannelDescriptions().get(description, this.getConfigurationMask());
	}

	public Duration getDuration(String id) {
		return this.solvisDescription.getDurations().get(id);
	}

	public void registerScreenChangedByUserObserver(ObserverI<Boolean> observer) {
		this.screenChangedByUserObserable.register(observer);
	}

	public void notifyScreenChangedByUserObserver(boolean userChanged) throws Throwable {
		this.screenChangedByUserObserable.notify(userChanged);
	}

	public void saveScreen() throws IOException {
		if (this.savedScreen == null) {
			this.savedScreen = this.getCurrentScreen();
		}

	}

	public void restoreScreen() throws IOException {
		if (savedScreen != null) {
			this.savedScreen.goTo(this);
		}
		this.savedScreen = null;
	}

	/**
	 * @return the allSolvisData
	 */
	public AllSolvisData getAllSolvisData() {
		return allSolvisData;
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

	public SystemGrafics getGrafics() {
		return grafics;
	}

	public void setGrafics(SystemGrafics grafics) {
		this.grafics = grafics;
	}

	public void learning() throws IOException, LearningError {
		Reference<Screen> currentRef = new Reference<Screen>();
		this.getGrafics().clear();
		logger.log(LEARN, "Learning started.");
		this.initConfigurationMask(currentRef);
		logger.info("Configuration mask: " + Integer.toHexString(this.configurationMask));
		this.getGrafics().setConfigurationMask(configurationMask);
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
		Collection<LearnScreen> learnScreens = this.solvisDescription.getLearnScreens(this.getConfigurationMask());
		while (learnScreens.size() > 0) {
			home.learn(this, learnScreens, this.getConfigurationMask());
			for (Iterator<LearnScreen> it = learnScreens.iterator(); it.hasNext();) {
				if (it.next().getScreen().isLearned(this)) {
					it.remove();
				}
			}
			this.gotoHome();
		}
		this.solvisDescription.getClock().learn(this);
		this.solvisDescription.getChannelDescriptions().learn(this);
		logger.log(LEARN, "Learning finished.");
	}

	/**
	 * @return the solvisDescription
	 */
	public SolvisDescription getSolvisDescription() {
		return solvisDescription;
	}

	public void backupMeasurements(SystemMeasurements system) {
		this.allSolvisData.backupSpecialMeasurements(system);

	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public void registerObserver(Observer.ObserverI<SolvisData> observer) {
		this.allSolvisData.registerObserver(observer);
	}

	public Distributor getDistributor() {
		return distributor;
	}

	public SolvisState getSolvisState() {
		return solvisState;
	}

	public int getConfigurationMask() {
		return this.configurationMask;
	}

	public SolvisConnection getConnection() {
		return connection;
	}

	private class MeasurementUpdateThread extends Thread {

		private int updateIntervall;
		boolean abort = false;
		boolean power = false;
		boolean powerDownInInterval = false;

		public MeasurementUpdateThread(int forcedUpdateIntervall_ms) {
			super("MeasurementUpdateThread");
			this.updateIntervall = forcedUpdateIntervall_ms;
			solvisState.register(new PowerObserver());
		}

		private class PowerObserver implements ObserverI<SolvisState> {

			@Override
			public void update(SolvisState data, Object source) {
				switch (data.getState()) {
					case SOLVIS_CONNECTED:
						power = true;
						powerDownInInterval = false;
						break;
					case POWER_OFF:
						power = false;
				}

			}

		}

		@Override
		public void run() {

			while (!abort && updateIntervall != 0) {
				Calendar midNight = Calendar.getInstance();
				long now = midNight.getTimeInMillis();
				midNight.set(Calendar.HOUR_OF_DAY, 0);
				midNight.set(Calendar.MINUTE, 0);
				midNight.set(Calendar.SECOND, 0);
				midNight.set(Calendar.MILLISECOND, 0);

				long midNightLong = midNight.getTimeInMillis();
				long nextUpdate = (now - midNightLong) / this.updateIntervall * this.updateIntervall + midNightLong
						+ this.updateIntervall;
				int waitTime = (int) (nextUpdate - now);
				synchronized (this) {
					try {
						this.wait(waitTime);
					} catch (InterruptedException e) {
					}
				}
				if (!abort && !powerDownInInterval) {
					distributor.notify(getAllSolvisData().getMeasurementsPackage());
				}

				if (!power) {
					powerDownInInterval = true;
				}
			}
		}

		public synchronized void abort() {
			this.abort = true;
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
		return timeZone;
	}

	public int getDefaultReadMeasurementsIntervall_ms() {
		return defaultReadMeasurementsIntervall_ms;
	}

	private void initConfigurationMask(Reference<Screen> current) throws TerminationException {
		boolean connected = false;
		this.configurationMask = 0;
		;
		while (!connected) {
			try {
				this.configurationMask = this.getSolvisDescription().getConfigurations(this, current);
				//this.configurationMask += 2 ;
				connected = true;
			} catch (IOException e) {
				logger.error("Solvis not available. Powered down or wrong IP address. Will try again");
				AbortHelper.getInstance().sleep(1000);
			}
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

}
