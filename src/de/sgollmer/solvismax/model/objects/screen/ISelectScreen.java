package de.sgollmer.solvismax.model.objects.screen;

import java.io.IOException;

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;

public interface ISelectScreen extends IAssigner{
	public boolean execute( Solvis solvis, AbstractScreen startingScreen) throws IOException, TerminationException;
	public int getSettingTime(Solvis solvis);
}
