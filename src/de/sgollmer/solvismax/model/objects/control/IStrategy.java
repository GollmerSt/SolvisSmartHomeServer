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

public interface IStrategy extends IAssigner {

	SingleData<?> getValue(final SolvisScreen solvisScreen, final Solvis solvis, final IControlAccess controlAccess,
			final boolean optional) throws TerminationException, IOException;

	SetResult setValue(final Solvis solvis, final IControlAccess controlAccess, final SolvisData value)
			throws IOException, TerminationException, TypeException;

	SetResult setValueFast(final Solvis solvis, final SolvisData value) throws IOException, TerminationException;

	boolean isWriteable();

	Integer getDivisor();

	Double getAccuracy();

	List<? extends IMode<?>> getModes();

	UpperLowerStep getUpperLowerStep();

	boolean isXmlValid();

	boolean mustBeLearned();

	boolean learn(final Solvis solvis, final IControlAccess controlAccess) throws IOException, TerminationException;

	SingleData<?> interpretSetData(final SingleData<?> singleData, final boolean debug) throws TypeException;

	boolean isBoolean();

	String getCsvMeta(final String column, final boolean semicolon);

	void setControl(final Control control);

	SetResult setDebugValue(final Solvis solvis, final SingleData<?> value) throws TypeException;

	void instantiate(Solvis solvis);

}
