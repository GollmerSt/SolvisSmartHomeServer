package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.slf4j.LoggerFactory;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.connection.AccountInfo;
import de.sgollmer.solvismax.connection.Distributor;
import de.sgollmer.solvismax.connection.SolvisConnection;
import de.sgollmer.solvismax.connection.SolvisConnection.Button;
import de.sgollmer.solvismax.connection.transfer.ChannelDescriptionsPackage;
import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.TerminationHelper;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.AllSolvisGrafics;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.Duration;
import de.sgollmer.solvismax.model.objects.GraficsLearnable.LearnScreen;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.Screen.ScreenTouch;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.SystemGrafics;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.backup.MeasurementsBackupHandler;
import de.sgollmer.solvismax.model.objects.backup.SystemMeasurements;
import de.sgollmer.solvismax.model.objects.data.IntegerValue;
import de.sgollmer.solvismax.model.objects.data.ModeI;
import de.sgollmer.solvismax.model.objects.data.ModeValue;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.xml.ControlFileReader;
import de.sgollmer.solvismax.xml.GraficFileHandler;
import de.sgollmer.solvismax.xml.XmlStreamReader;

public class Solvis {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Solvis.class);

	private static boolean LEARNING = false;

	private Screen homeScreen;

	private final SolvisState solvisState = new SolvisState();
	private final SolvisDescription solvisDescription;
	private final AllSolvisData allSolvisData = new AllSolvisData(this);
	private SystemGrafics grafics;

	private SolvisWorkers worker;

	private final String id;
	private int configurationMask = 0;
	private MyImage currentImage = null;
	private Screen currentScreen = null;
	private Screen savedScreen = null;
	private String measureData = null;
	private boolean screenSaverActive = false;
	private final TouchPoint resetSceenSaver;
	private final SolvisConnection connection;
	private final MeasurementsBackupHandler backupHandler;
	private final Distributor distributor;
	private final MeasurementUpdateThread measurementUpdateThread;

	public Solvis(String id, SolvisDescription solvisDescription, SystemGrafics grafics, SolvisConnection connection,
			MeasurementsBackupHandler measurementsBackupHandler) {
		this.id = id;
		this.solvisDescription = solvisDescription;
		this.resetSceenSaver = solvisDescription.getSaver().getResetScreenSaver();
		this.grafics = grafics;
		this.connection = connection;
		this.allSolvisData.setAverageCount(this.solvisDescription.getMiscellaneous().getDefaultAverageCount());
		this.allSolvisData.setReadMeasurementInterval(
				this.solvisDescription.getMiscellaneous().getDefaultReadMeasurementsIntervall());
		this.worker = new SolvisWorkers(this);
		this.backupHandler = measurementsBackupHandler;
		this.backupHandler.register(this, id);
		this.distributor = new Distributor();
		this.measurementUpdateThread = new MeasurementUpdateThread();
	}

	private Observer.Observable<Boolean> screenChangedByUserObserable = new Observable<>();

	private Object solvisGUIObject = new Object();
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
			synchronized (solvisGUIObject) {
				screen = this.solvisDescription.getScreens().getScreen(this.getCurrentImage(), this);
				this.currentScreen = screen;
			}
		}
		return screen;
	}

	public boolean forceCurrentScreen(Screen current) throws IOException {
		synchronized (solvisGUIObject) {
			this.clearCurrentImage();
			this.currentScreen = current;
			return true;
		}
	}

	public void clearCurrentImage() {
		synchronized (solvisGUIObject) {
			this.currentImage = null;
			this.currentScreen = null;
		}
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
		synchronized (solvisGUIObject) {
			this.getConnection().sendTouch(point.getCoordinate());
			TerminationHelper.getInstance().sleep(point.getPushTime());
			this.getConnection().sendRelease();
			TerminationHelper.getInstance().sleep(point.getReleaseTime());
			this.clearCurrentImage();
		}
	}

	public void sendBack() throws IOException, TerminationException {
		synchronized (solvisGUIObject) {
			this.getConnection().sendButton(Button.BACK);
			TerminationHelper.getInstance().sleep(this.solvisDescription.getDurations().get("WindowChange").getTime_ms());
			this.clearCurrentImage();
		}
	}

	public void gotoHome() throws IOException {
		synchronized (solvisGUIObject) {
			this.solvisDescription.getFallBack().execute(this);
		}
	}

	public boolean gotoScreen(Screen screen) throws IOException, TerminationException {
		synchronized (solvisGUIObject) {
			if (screen == null) {
				return false;
			}
			if (this.getCurrentScreen() == null) {
				this.gotoHome();
			}

			if (this.getCurrentScreen() == screen) {
				return true;
			}

			if (screen == this.homeScreen) {
				this.gotoHome();
				return true;
			}

			List<ScreenTouch> previousScreens = screen.getPreviosScreens(this.getConfigurationMask());

			boolean gone = false;

			for (int cnt = 0; !gone && cnt < Constants.FAIL_REPEATS; ++cnt) {

				for (int gotoDeepth = 0; !gone
						&& this.getCurrentScreen() != null & gotoDeepth < Constants.MAX_GOTO_DEEPTH; ++gotoDeepth) {

					// && this.getCurrentScreen() != this.homeScreen) {
					// ListIterator<Screen> it = null;
					ScreenTouch foundScreenTouch = null;
					for (Iterator<ScreenTouch> it = previousScreens.iterator(); it.hasNext();) {
						ScreenTouch st = it.next();
						Screen previous = st.getScreen();
						if (previous == this.getCurrentScreen()) {
							foundScreenTouch = st;
							break;
						}
					}

					if (foundScreenTouch == null) {
						this.sendBack();
					} else {
						this.send(foundScreenTouch.getTouchPoint());
					}

					if (this.getCurrentScreen() == screen) {
						gone = true;
					}
				}
				if (!gone) {
					this.gotoHome(); // try it from beginning
					logger.info("Goto screen <" + screen.getId() + "> not succcessful. Will be retried.");
				}
			}
			if (!gone) {
				logger.error("Screen <" + screen.getId() + "> not found.");
			}
			return this.getCurrentScreen() == screen;
		}

	}

	public void init() throws IOException, XmlError, XMLStreamException {
		synchronized (solvisGUIObject) {
			this.gotoHome();
			this.configurationMask = this.getSolvisDescription().getConfigurations(this);
			synchronized (solvisMeasureObject) {
				this.getSolvisDescription().getChannelDescriptions().init(this, this.getAllSolvisData());
			}
			SystemMeasurements oldMeasurements = this.backupHandler.getSystemMeasurements(this.id);
			this.getAllSolvisData().restoreSpecialMeasurements(oldMeasurements);
			this.worker.start();
			this.getSolvisDescription().getChannelDescriptions().initControl(this);
			this.getDistributor().register(this);
			this.measurementUpdateThread.start();
		}
	}

	public void measure() throws IOException, ErrorPowerOn {
		synchronized (solvisMeasureObject) {
			this.getSolvisDescription().getChannelDescriptions().measure(this, this.getAllSolvisData());
		}
	}

	public void execute(Command command) {
		this.worker.push(command);
	}

	public boolean getValue(ChannelDescription description) throws IOException, ErrorPowerOn {
		SolvisData data = this.allSolvisData.get(description);
		return description.getValue(data, this);
	}

	public boolean setValue(ChannelDescription description, SolvisData value) throws IOException {
		synchronized (solvisGUIObject) {
			return description.setValue(this, value);
		}
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
		;
	}

	public void saveScreen() throws IOException {
		synchronized (solvisGUIObject) {
			if (this.savedScreen == null) {
				this.savedScreen = this.getCurrentScreen();
			}
		}

	}

	public void restoreScreen() throws IOException {
		synchronized (solvisGUIObject) {
			this.gotoScreen(this.savedScreen);
			this.savedScreen = null;
		}
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
		logger.info("Learning started.");
		this.gotoHome();
		this.configurationMask = this.getSolvisDescription().getConfigurations(this);
		logger.info("Configuration mask: " + Integer.toHexString(this.configurationMask));
		this.getGrafics().clear();
		String homeId = this.solvisDescription.getHomeId();
		Screen home = this.solvisDescription.getScreens().get(homeId, this.getConfigurationMask());
		if (home == null) {
			throw new AssertionError("Assign error: Screen description of <" + homeId + "> not found.");
		}
		this.forceCurrentScreen(home);
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
		this.solvisDescription.getChannelDescriptions().learn(this);
		logger.info("Learning finished.");
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

	public static void main(String[] args) throws IOException, XmlError, XMLStreamException {
		String id = "mySolvis";
		ControlFileReader reader = new ControlFileReader(null);

		XmlStreamReader.Result<SolvisDescription> result = reader.read();

		SolvisDescription solvisDescription = result.getTree();

		ChannelDescriptionsPackage json = new ChannelDescriptionsPackage(solvisDescription.getChannelDescriptions(), 1);

		StringBuilder jsonBuiler = new StringBuilder();
		json.getFrame().addTo(jsonBuiler);
		System.out.println(jsonBuiler.toString());

		final MeasurementsBackupHandler measurementsBackupHandler = new MeasurementsBackupHandler(null,
				solvisDescription.getMiscellaneous().getMeasurementsBackupTime_ms());

		GraficFileHandler graficFileHandler = new GraficFileHandler(null, result.getHash());
		AllSolvisGrafics grafics = graficFileHandler.read();

		SolvisConnection connection = new SolvisConnection("http://192.168.1.40", new AccountInfo() {

			@Override
			public String getAccount() {
				return "SGollmer";
			}

			@Override
			public char[] createPassword() {
				return "e5am1kro".toCharArray();
			}
		});

		final Solvis solvis = new Solvis(id, solvisDescription, grafics.get(id), connection, measurementsBackupHandler);

		if (LEARNING || solvis.getGrafics().isEmpty()) {
			solvis.learning();

			graficFileHandler.write(grafics);
		}

		solvis.gotoHome();

		solvis.init();

		measurementsBackupHandler.start();

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				measurementsBackupHandler.terminate();
				solvis.worker.terminate();
			}
		}));
		ChannelDescription description = solvis.getChannelDescription("C01.Anlagenmodus");

		solvis.execute(new Command(description));

		solvis.execute(new Command(description, new ModeValue<ModeI>(new ModeI() {

			@Override
			public String getName() {
				return "Tag";
			}
		})));

		description = solvis.getChannelDescription("C07.Raumeinfluss_HK1");

		solvis.execute(new Command(description));

		solvis.execute(new Command(description, new IntegerValue(50)));

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		solvis.execute(new Command(description, new IntegerValue(90)));

		// solvis.worker.terminate();

	}

	public String getId() {
		return id;
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
		private long nextUpdate;
		boolean terminate = false;

		public MeasurementUpdateThread() {
			super("MeasurementUpdateThread");
			this.updateIntervall = getSolvisDescription().getMiscellaneous().getForcedUpdateIntervall();
			Calendar midNight = Calendar.getInstance();
			long time = midNight.getTimeInMillis();
			midNight.set(Calendar.HOUR_OF_DAY, 0);
			midNight.set(Calendar.MINUTE, 0);
			midNight.set(Calendar.SECOND, 0);
			midNight.set(Calendar.MILLISECOND, 0);

			long midNightLong = midNight.getTimeInMillis();

			for (nextUpdate = midNightLong; nextUpdate < time + 1000; nextUpdate += updateIntervall)
				;
		}

		@Override
		public void run() {

			while (!terminate && updateIntervall != 0 ) {
				long time = System.currentTimeMillis();
				int waitTime = (int) (this.nextUpdate - time);
				if ( waitTime <= 0) {
					waitTime = Constants.WAITTIME_IF_LE_ZERO ;
				}
				this.nextUpdate += updateIntervall;
				synchronized (this) {
					try {
						this.wait(waitTime);
					} catch (InterruptedException e) {
					}
				}
				if (!terminate) {
					distributor.notify(getAllSolvisData().getMeasurementsPackage());
				}
			}
		}

		public synchronized void terminate() {
			this.terminate = true;
			this.notifyAll();
		}

	}

	public void terminate() {
		this.distributor.teminate();
		this.worker.terminate();
		this.measurementUpdateThread.terminate();
	}

}
