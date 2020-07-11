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
import de.sgollmer.solvismax.error.TypeError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.objects.Rectangle;

public interface IStrategy extends IAssigner {

	public SingleData<?> getValue(SolvisScreen solvisScreen, Solvis solvis, IControlAccess controlAccess,
			boolean optional) throws TerminationException, IOException;

	public SingleData<?> setValue(Solvis solvis, IControlAccess controlAccess, SolvisData value)
			throws IOException, TerminationException, TypeError;

	public boolean isWriteable();

	public Integer getDivisor();

	public Double getAccuracy();

	public List<? extends IMode> getModes();

	public UpperLowerStep getUpperLowerStep();

	public void setCurrentRectangle(Rectangle rectangle);

	public boolean mustBeLearned();

	public boolean learn(Solvis solvis, IControlAccess controlAccess) throws IOException;

	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeError;

	public boolean isXmlValid(boolean modbus);

	public boolean isBoolean();

	public SingleData<?> createSingleData(String value) throws TypeError;

}
