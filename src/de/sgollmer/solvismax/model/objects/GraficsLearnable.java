package de.sgollmer.solvismax.model.objects;

import java.io.IOException;

import de.sgollmer.solvismax.error.LearningError;
import de.sgollmer.solvismax.model.Solvis;

public interface GraficsLearnable {
	public void learn( Solvis solvis) throws IOException, LearningError ;

}
