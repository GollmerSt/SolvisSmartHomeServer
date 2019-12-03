package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import de.sgollmer.solvismax.connection.AccountInfo;
import de.sgollmer.solvismax.connection.SolvisConnection;
import de.sgollmer.solvismax.connection.SolvisConnection.Button;
import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.objects.AllSolvisData;
import de.sgollmer.solvismax.model.objects.AllSolvisGrafics;
import de.sgollmer.solvismax.model.objects.DataDescription;
import de.sgollmer.solvismax.model.objects.Duration;
import de.sgollmer.solvismax.model.objects.GraficsLearnable.LearnScreen;
import de.sgollmer.solvismax.model.objects.Observer;
import de.sgollmer.solvismax.model.objects.Observer.Observable;
import de.sgollmer.solvismax.model.objects.Observer.ObserverI;
import de.sgollmer.solvismax.model.objects.Screen;
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

	private static boolean LEARNING = false;

	private SmartHome smartHome;

	private Screen homeScreen;

	private final SolvisDescription solvisDescription;
	private final AllSolvisData allSolvisData = new AllSolvisData(this);
	private SystemGrafics grafics;

	private SolvisWorkers worker;
	private WatchDog watchDog;

	private MyImage currentImage = null;
	private Screen currentScreen = null;
	private Screen savedScreen = null;
	private String measureData = null;
	private boolean screenSaverActive = false;
	private final TouchPoint resetSceenSaver;
	private final SolvisConnection connection;
	private final MeasurementsBackupHandler backupHandler;
	private final String id;

	public Solvis(String id, SolvisDescription solvisDescription, SystemGrafics grafics, SolvisConnection connection,
			MeasurementsBackupHandler measurementsBackupHandler) {
		this.id = id;
		this.solvisDescription = solvisDescription;
		this.resetSceenSaver = solvisDescription.getSaver().getResetScreenSaver();
		this.watchDog = new WatchDog(this, solvisDescription.getSaver());
		this.grafics = grafics;
		this.connection = connection;
		this.allSolvisData.setAverageCount(this.solvisDescription.getMiscellaneous().getDefaultAverageCount());
		this.allSolvisData.setReadMeasurementInterval(
				this.solvisDescription.getMiscellaneous().getDefaultReadMeasurementsIntervall());
		this.worker = new SolvisWorkers(this);
		this.backupHandler = measurementsBackupHandler;
		this.backupHandler.register(this, id);
	}

	private Observer.Observable<Screen> screenChangedByUserObserable = new Observable<Screen>();

	private Object solvisGUIObject = new Object();
	private Object solvisMeasureObject = new Object();

	public boolean isCurrentImageValid() {
		return this.currentImage != null;
	}

	public MyImage getCurrentImage() throws IOException {
		if (this.screenSaverActive) {
			this.send(resetSceenSaver);
			this.screenSaverActive = false;
		}
		MyImage image = this.currentImage;
		if (image == null) {
			this.currentImage = this.getRealImage();
		}
		return this.currentImage;
	}

	public MyImage getRealImage() throws IOException {
		synchronized (solvisGUIObject) {
			MyImage image = new MyImage(connection.getScreen());
			return image;
		}
	}

	public Screen getCurrentScreen() throws IOException {
		if (this.screenSaverActive) {
			this.send(resetSceenSaver);
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

	public String getMeasureData() throws IOException {
		synchronized (solvisMeasureObject) {
			if (this.measureData == null) {
				this.measureData = this.connection.getMeasurements().substring(12);
			}
			return this.measureData;
		}
	}

	public void clearMeasuredData() {
		synchronized (solvisMeasureObject) {
			this.measureData = null;
		}
	}

	public void send(TouchPoint point) throws IOException {
		synchronized (solvisGUIObject) {
			this.connection.sendTouch(point.getCoordinate());
			try {
				Thread.sleep(point.getPushTime());
			} catch (InterruptedException e) {
			}
			this.connection.sendRelease();
			try {
				Thread.sleep(point.getReleaseTime());
			} catch (InterruptedException e) {
			}
			this.clearCurrentImage();
		}
	}

	public void sendBack() throws IOException {
		synchronized (solvisGUIObject) {
			this.connection.sendButton(Button.BACK);
			try {
				// Thread.sleep(this.solvisDescription.getDurations().get("Standard").getTime_ms()
				// ) ;
				Thread.sleep(this.solvisDescription.getDurations().get("WindowChange").getTime_ms());
			} catch (InterruptedException e) {
			}
			this.clearCurrentImage();
		}
	}

	public void gotoHome() throws IOException {
		synchronized (solvisGUIObject) {
			for (int cnt = 0; cnt < 4; ++cnt) {
				this.sendBack();
			}
		}
	}

	public boolean gotoScreen(Screen screen) throws IOException {
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

			List<Screen> previousScreens = screen.getPreviosScreens();

			while (this.getCurrentScreen() != null && this.getCurrentScreen() != screen) {
				// && this.getCurrentScreen() != this.homeScreen) {
				// ListIterator<Screen> it = null;
				Screen next = screen;
				boolean found = false;
				for (Iterator<Screen> it = previousScreens.iterator(); it.hasNext() && !found;) {
					Screen previous = it.next();
					if (previous == this.getCurrentScreen()) {
						found = true;
						break;
					} else {
						next = previous;
					}
				}

				if (found) {
					this.send(next.getTouchPoint());
				} else {
					this.sendBack();
				}

			}
			return this.getCurrentScreen() != null && this.getCurrentScreen() == screen;
		}
	}

	public void init() throws IOException, XmlError, XMLStreamException {
		synchronized (solvisGUIObject) {
			synchronized (solvisMeasureObject) {
				this.getSolvisDescription().getDataDescriptions().init(this, this.getAllSolvisData());
			}
			SystemMeasurements oldMeasurements = this.backupHandler.getSystemMeasurements(this.id);
			this.getAllSolvisData().restoreSpecialMeasurements(oldMeasurements);
			this.worker.start();
			this.getSolvisDescription().getDataDescriptions().initControl(this);
		}
	}

	public void measure() throws IOException {
		synchronized (solvisMeasureObject) {
			this.getSolvisDescription().getDataDescriptions().measure(this, this.getAllSolvisData());
		}
	}

	public void execute(Command command) {
		this.worker.push(command);
	}

	public boolean getValue(DataDescription description) throws IOException, ErrorPowerOn {
		SolvisData data = this.allSolvisData.get(description);
		return description.getValue(data, this);
	}

	// public boolean setValue(String description, String value) {
	// synchronized (solvisObject) {
	// return this.descriptions.setValue(this, description, value);
	// }
	// }
	//
	public boolean setValue(DataDescription description, SolvisData value) throws IOException {
		synchronized (solvisGUIObject) {
			return description.setValue(this, value);
		}
	}

	public DataDescription getDataDescription(String description) {
		return this.solvisDescription.getDataDescriptions().get(description);
	}

	public Duration getDuration(String id) {
		return this.solvisDescription.getDurations().get(id);
	}

	public WatchDog getWatchDog() {
		return this.watchDog;

	}

	public void registerScreenChangedByUserObserver(ObserverI<Screen> observer) {
		this.screenChangedByUserObserable.register(observer);
	}

	public void notifyScreenChangedByUserObserver(Screen screen) {
		this.screenChangedByUserObserable.notify(screen);
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

	public SmartHome getSmartHome() {
		return smartHome;
	}

	/**
	 * @return the allSolvisData
	 */
	public AllSolvisData getAllSolvisData() {
		return allSolvisData;
	}

	/**
	 * @param screenSaver
	 *            the screenSaver to set
	 */
	public void setScreenSaverActive(boolean screenSaverActive) {
		this.screenSaverActive = screenSaverActive;
	}

	public SystemGrafics getGrafics() {
		return grafics;
	}

	public void setGrafics(SystemGrafics grafics) {
		this.grafics = grafics;
	}

	public void learning() throws IOException {
		this.getGrafics().clear();
		this.gotoHome();
		String homeId = this.solvisDescription.getHomeId();
		Screen home = this.solvisDescription.getScreens().get(homeId);
		if (home == null) {
			throw new AssertionError("Assign error: Screen description of <" + homeId + "> not found.");
		}
		this.forceCurrentScreen(home);
		Collection<LearnScreen> learnScreens = this.solvisDescription.getLearnScreens();
		while (learnScreens.size() > 0) {
			home.learn(this, learnScreens);
			for (Iterator<LearnScreen> it = learnScreens.iterator(); it.hasNext();) {
				if (it.next().getScreen().isLearned(this)) {
					it.remove();
				}
			}
			this.gotoHome();
		}
		this.solvisDescription.getDataDescriptions().learn(this);
	}

	public static void main(String[] args) throws IOException, XmlError, XMLStreamException {
		String id = "mySolvis";
		ControlFileReader reader = new ControlFileReader(null);

		XmlStreamReader.Result<SolvisDescription> result = reader.read();

		SolvisDescription solvisDescription = result.getTree();

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
		DataDescription description = solvis.getDataDescription("C01.Anlagenmodus");

		solvis.execute(new Command(description));

		solvis.execute(new Command(description, new ModeValue<ModeI>(new ModeI() {

			@Override
			public String getName() {
				return "Tag";
			}
		})));

		description = solvis.getDataDescription("C07.Raumeinfluss_HK1");

		solvis.execute(new Command(description));

		solvis.execute(new Command(description, new IntegerValue(50)));

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		solvis.execute(new Command(description, new IntegerValue(90)));

		// solvis.worker.terminate();

	}

	/**
	 * @return the solvisDescription
	 */
	public SolvisDescription getSolvisDescription() {
		return solvisDescription;
	}

	public void powerDetected(boolean power) {
		// TODO Auto-generated method stub
	}

	public void errorScreenDetected() {
		// TODO Auto-generated method stub

	}

	public void backupMeasurements(SystemMeasurements system) {
		this.allSolvisData.backupSpecialMeasurements(system);

	}

}
