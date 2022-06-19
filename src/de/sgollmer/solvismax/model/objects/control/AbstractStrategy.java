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
import de.sgollmer.solvismax.model.objects.IChannelSource.SetResult;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;

public abstract class  AbstractStrategy {
	
	private Control control = null;

	abstract SingleData<?> getValue(final SolvisScreen solvisScreen, final Solvis solvis,
			final boolean optional) throws TerminationException, IOException;

	abstract SetResult setValue(final Solvis solvis, final SolvisData value)
			throws IOException, TerminationException, TypeException;

	abstract SetResult setValueFast(final Solvis solvis, final SolvisData value) throws IOException, TerminationException;

	abstract boolean isWriteable();

	abstract Integer getDivisor();

	abstract Double getAccuracy();

	abstract List<? extends IMode<?>> getModes();

	abstract UpperLowerStep getUpperLowerStep();

	abstract boolean isXmlValid();

	abstract boolean mustBeLearned();

	abstract boolean learn(final Solvis solvis) throws IOException, TerminationException;

	abstract SingleData<?> interpretSetData(final SingleData<?> singleData, final boolean debug) throws TypeException;

	abstract boolean isBoolean();

	abstract String getCsvMeta(final String column, final boolean semicolon);

	abstract SetResult setDebugValue(final Solvis solvis, final SingleData<?> value) throws TypeException;

	abstract void instantiate(Solvis solvis);

	abstract boolean inhibitGuiReadAfterWrite();

	void setControl(final Control control) {
		this.control = control;
	}

	Control getControl() {
		return this.control;
	}

}
