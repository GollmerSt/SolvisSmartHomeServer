/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.Collection;

import de.sgollmer.solvismax.model.objects.ChannelInstance;
import de.sgollmer.solvismax.model.objects.IChannelSource.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.DoubleValue;
import de.sgollmer.solvismax.model.objects.data.IMode;

public class ChannelDescription extends Element {
	public ChannelDescription(final ChannelInstance instance) {
		super(instance.getName());

		Frame frame = new Frame();
		this.value = frame;

		boolean writeable = instance.isWriteable();
		Element writeableElement = new Element("Writeable", new SingleValue(new BooleanValue(writeable, -1L)));
		frame.add(writeableElement);

		String type = instance.getType().name();
		Element typeElement = new Element("Type", new SingleValue(type));
		frame.add(typeElement);

		String unitString = instance.getUnit();
		if (unitString != null) {
			Element unit = new Element("Unit", new SingleValue(unitString));
			frame.add(unit);
		}

		Double accuracy = instance.getAccuracy();
		if (accuracy != null) {
			Element ac = new Element("Accuracy", new SingleValue(new DoubleValue(accuracy, -1L)));
			frame.add(ac);
		}

		boolean isBoolean = instance.isBoolean();
		Element ib = new Element("IsBoolean", new SingleValue(new BooleanValue(isBoolean, -1L)));
		frame.add(ib);

		UpperLowerStep upperLowerStep = instance.getUpperLowerStep();
		if (upperLowerStep != null) {
			Element upper = new Element("Upper", new SingleValue(new DoubleValue(upperLowerStep.getUpper(), -1L)));
			frame.add(upper);
			Element lower = new Element("Lower", new SingleValue(new DoubleValue(upperLowerStep.getLower(), -1L)));
			frame.add(lower);
			Element step = new Element("Step", new SingleValue(new DoubleValue(upperLowerStep.getStep(), -1L)));
			frame.add(step);
			if (upperLowerStep.getIncrementChange() != null) {
				Element incrementChange = new Element("IncrementChange",
						new SingleValue(new DoubleValue(upperLowerStep.getIncrementChange(), -1L)));
				frame.add(incrementChange);
			}
			if (upperLowerStep.getChangedIncrement() != null) {
				Element changedIncrement = new Element("ChangedIncrement",
						new SingleValue(new DoubleValue(upperLowerStep.getChangedIncrement(), -1L)));
				frame.add(changedIncrement);
			}
		}

		Collection<? extends IMode<?>> modes = instance.getModes();
		if (modes != null) {
			ArrayValue arrayValue = new ArrayValue();

			for (IMode<?> mode : modes) {
				Frame modeFrame = new Frame();
				Element element = new Element("Name", new SingleValue(mode.getName()));
				modeFrame.add(element);
				element = new Element("Handling", new SingleValue(mode.getHandling().name()));
				modeFrame.add(element);
				arrayValue.add(modeFrame);
			}

			Element modesElement = new Element("Modes", arrayValue);

			frame.add(modesElement);
		}
	}
}
