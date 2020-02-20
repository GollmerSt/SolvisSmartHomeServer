/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.Constants;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.helper.ImageHelper;
import de.sgollmer.solvismax.helper.Reference;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.ScreenLearnable.LearnScreen;
import de.sgollmer.solvismax.objects.Coordinate;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Configurations {

	private static final String XML_HEATER_LOOPS = "HeaterLoops";
	private static final String XML_SOLAR = "Solar";
	private static final String XML_REFERENCE_POINT = "ReferencePoint";

	private final Collection<Configuration> configurations;
	private final Coordinate referencePoint;

	public Configurations(Collection<Configuration> configurations, Coordinate referencePoint) {
		this.configurations = configurations;
		this.referencePoint = referencePoint;
	}

	public int get(Solvis solvis, Reference<Screen> currentRef) throws IOException {

		solvis.gotoHome();
		this.determineOrigin(solvis);
		Screen current = solvis.getHomeScreen();

		int configurationMask = 0;
		Screen home = solvis.getHomeScreen();
		Configuration homeConfiguration = null;
		Collection<LearnScreen> learnConfigurationScreens = new ArrayList<>();
		for (Iterator<Configuration> it = this.configurations.iterator(); it.hasNext();) {
			Configuration configuration = it.next();
			Screen screen = configuration.getScreen(solvis);
			if (screen == home) {
				homeConfiguration = configuration;
			} else {
				screen.createAndAddLearnScreen(null, learnConfigurationScreens, 0);
			}
		}
		if (homeConfiguration != null) {
			Collection<LearnScreen> learnHomeScreen = new ArrayList<>();
			home.createAndAddLearnScreen(null, learnHomeScreen, 0);
			home.gotoLearning(solvis, current, learnHomeScreen, 0);
			home.learn(solvis, learnHomeScreen, 0);
			configurationMask |= homeConfiguration.getConfiguration(solvis);
			current = home;
		}
		for (Configuration configuration : this.configurations) {
			Screen screen = configuration.getScreen(solvis);
			if (screen != home) {
				screen.gotoLearning(solvis, current, learnConfigurationScreens, 0);
				screen.learn(solvis, learnConfigurationScreens, 0);
				configurationMask |= configuration.getConfiguration(solvis);
				current = screen;
			}
		}
		home.gotoLearning(solvis, current, learnConfigurationScreens, 0);
		currentRef.set(current);

		return configurationMask;
	}

	public interface Configuration {
		public int getConfiguration(Solvis solvis) throws IOException;

		public Screen getScreen(Solvis solvis);
	}

	public static class Creator extends CreatorByXML<Configurations> {

		private final Collection<Configuration> configurations = new ArrayList<>();
		private Coordinate referencePoint;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public Configurations create() throws XmlError, IOException {
			return new Configurations(configurations, referencePoint);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_HEATER_LOOPS:
					return new HeaterLoops.Creator(id, getBaseCreator());
				case XML_SOLAR:
					return new Solar.Creator(id, getBaseCreator());
				case XML_REFERENCE_POINT:
					return new Coordinate.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_HEATER_LOOPS:
					this.configurations.add((Configuration) created);
					break;
				case XML_SOLAR:
					this.configurations.add((Configuration) created);
					break;
				case XML_REFERENCE_POINT:
					this.referencePoint = (Coordinate) created;
					break;
			}
		}

	}

	private void determineOrigin(Solvis solvis) throws IOException {
		Coordinate origin = new Coordinate(0, 0);
		solvis.getGrafics().setOrigin(origin);
		MyImage image = solvis.getRealScreen().getImage();
		int xFound = 0;
		int yFound = 0;
		boolean found = false;
		int brightnessX = 0;
		int x;
		
		int rightX = this.referencePoint.getX() + Constants.REFERENCE_POINT_SEARCH_RANGE ;
		int upperY = this.referencePoint.getY() - Constants.REFERENCE_POINT_SEARCH_RANGE ;
		for (x = rightX; x >= 0 && !found; --x) {
			brightnessX = ImageHelper.getBrightness(image.getRGB(x, upperY));
			if (brightnessX < 256) {
				found = true;
				xFound = x;
				for (int y = -Constants.REFERENCE_POINT_SEARCH_RANGE; y < Constants.REFERENCE_POINT_SEARCH_RANGE
						&& found; ++y) {
					int brightnessY = ImageHelper.getBrightness(image.getRGB(x, y + this.referencePoint.getY()));
					if (brightnessX != brightnessY) {
						found = false;
					}
				}
			}
		}
		for (x = xFound; x >= 0
				&& brightnessX >= ImageHelper.getBrightness(image.getRGB(x, upperY)); --x)
			;

		found = false;
		for (int y = upperY; y < image.getHeight()
				&& !found; ++y) {
			int brightnessY = ImageHelper.getBrightness(image.getRGB(x, y));
			if (brightnessY == brightnessX) {
				found = true;
				yFound = y;
			}
		}
		xFound -= referencePoint.getX();
		yFound -= referencePoint.getY();

		origin = new Coordinate(xFound, yFound);

		solvis.getGrafics().setOrigin(origin);
		solvis.clearCurrentScreen();
	}
}
