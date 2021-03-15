/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.control;

import java.io.IOException;
import java.util.List;

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;

public interface IStrategy extends IAssigner {

	SingleData<?> getValue(SolvisScreen solvisScreen, Solvis solvis, IControlAccess controlAccess, boolean optional)
			throws TerminationException, IOException;

	SetResult setValue(Solvis solvis, IControlAccess controlAccess, SolvisData value)
			throws IOException, TerminationException, TypeException;

	boolean isWriteable();

	Integer getDivisor();

	Double getAccuracy();

	List<? extends IMode<?>> getModes();

	UpperLowerStep getUpperLowerStep();

	void setCurrentRectangle(Rectangle rectangle);

	boolean isXmlValid();

	boolean mustBeLearned();

	boolean learn(Solvis solvis, IControlAccess controlAccess) throws IOException, TerminationException;

	SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeException;

	boolean isBoolean();

	SingleData<?> createSingleData(String value, long timeStamp) throws TypeException;

}
