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
	public ChannelDescription(ChannelInstance instance) {
		
		this.name = instance.getName();
		Frame frame = new Frame();
		this.value = frame;

		boolean writeable = instance.isWriteable();
		Element writeableElement = new Element();
		writeableElement.name = "Writeable";
		SingleValue sv = new SingleValue(new BooleanValue(writeable, -1));
		writeableElement.value = sv;
		frame.add(writeableElement);

		String type = instance.getType().name();
		Element typeElement = new Element();
		typeElement.name = "Type";
		sv = new SingleValue(type);
		typeElement.value = sv;
		frame.add(typeElement);

		String unitString = instance.getUnit();
		if (unitString != null) {
			Element unit = new Element();
			unit.name = "Unit";
			unit.value = new SingleValue(unitString);
			frame.add(unit);
		}

		Double accuracy = instance.getAccuracy();
		if (accuracy != null) {
			Element ac = new Element();
			ac.name = "Accuracy";
			ac.value = new SingleValue(new DoubleValue(accuracy, -1));
			frame.add(ac);
		}

		boolean isBoolean = instance.isBoolean();
		Element ib = new Element();
		ib.name = "IsBoolean";
		ib.value = new SingleValue(new BooleanValue(isBoolean, -1));
		frame.add(ib);

		UpperLowerStep upperLowerStep = instance.getUpperLowerStep();
		if (upperLowerStep != null) {
			Element upper = new Element();
			upper.name = "Upper";
			upper.value = new SingleValue(new DoubleValue(upperLowerStep.getUpper(), -1));
			frame.add(upper);
			Element lower = new Element();
			lower.name = "Lower";
			lower.value = new SingleValue(new DoubleValue(upperLowerStep.getLower(), -1));
			frame.add(lower);
			Element step = new Element();
			step.name = "Step";
			step.value = new SingleValue(new DoubleValue(upperLowerStep.getStep(), -1));
			frame.add(step);
			if (upperLowerStep.getIncrementChange() != null) {
				Element incrementChange = new Element();
				incrementChange.name = "IncrementChange";
				incrementChange.value = new SingleValue(new DoubleValue(upperLowerStep.getIncrementChange(), -1));
				frame.add(incrementChange);
			}
			if (upperLowerStep.getChangedIncrement() != null) {
				Element changedIncrement = new Element();
				changedIncrement.name = "ChangedIncrement";
				changedIncrement.value = new SingleValue(new DoubleValue(upperLowerStep.getChangedIncrement(), -1));
				frame.add(changedIncrement);
			}
		}

		Collection<? extends IMode<?>> modes = instance.getModes();
		if (modes != null) {
			ArrayValue arrayValue = new ArrayValue();

			for (IMode<?> mode : modes) {
				IValue value = new SingleValue(mode.getName());
				arrayValue.add(value);
			}

			Element modesElement = new Element();
			modesElement.name = "Modes";
			modesElement.value = arrayValue;

			frame.add(modesElement);
		}
	}
}
