package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.TouchPoint;
import de.sgollmer.solvismax.model.objects.configuration.ConfigurationMasks;
import de.sgollmer.solvismax.model.objects.configuration.OfConfigs;
import de.sgollmer.solvismax.model.objects.screen.IScreenLearnable.LearnScreen;
import de.sgollmer.solvismax.model.objects.screen.Screen.ScreenTouch;
import de.sgollmer.solvismax.objects.Rectangle;

public interface IScreen extends OfConfigs.IElement<IScreen> {

	public String getId();

	public boolean isScreen();

	public boolean isScreen(MyImage image, Solvis solvis);

	public void createAndAddLearnScreen(LearnScreen learnScreen, Collection<LearnScreen> learnScreens, Solvis solvis);

	public void addNextScreen(IScreen screen);

	public OfConfigs<IScreen> getBackScreen();

	public IScreen getBackScreen(int configurationMask);

	public IScreen getPreviousScreen(int configurationMask);

	public ConfigurationMasks getConfigurationMasks();

	public TouchPoint getTouchPoint();

	public List<ScreenTouch> getPreviousScreens(int configurationMask);

	public void gotoLearning(Solvis solvis, IScreen current, Collection<LearnScreen> learnScreens)
			throws IOException, TerminationException;

	public void learn(Solvis solvis, Collection<LearnScreen> learnScreens, int configurationMask);

	public boolean goTo(Solvis solvis) throws IOException, TerminationException ;

	public boolean isIgnoreChanges();

	public Collection<Rectangle> getIgnoreRectangles();


}
