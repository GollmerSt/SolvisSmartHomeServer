/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import java.util.Collection;

import de.sgollmer.solvismax.model.objects.ChannelSourceI.UpperLowerStep;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.FloatValue;
import de.sgollmer.solvismax.model.objects.data.ModeI;

public class ChannelDescription extends Element {
	public ChannelDescription(de.sgollmer.solvismax.model.objects.ChannelDescription description) {
		this.name = description.getId();
		Frame frame = new Frame();
		this.value = frame;

		boolean writeable = description.isWriteable();
		Element writeableElement = new Element();
		writeableElement.name = "Writeable";
		SingleValue sv = new SingleValue(new BooleanValue(writeable));
		writeableElement.value = sv;
		frame.add(writeableElement);

		String type = description.getType().name();
		Element typeElement = new Element();
		typeElement.name = "Type";
		sv = new SingleValue(type);
		typeElement.value = sv;
		frame.add(typeElement);

		String unitString = description.getUnit();
		if (unitString != null) {
			Element unit = new Element();
			unit.name = "Unit";
			unit.value = new SingleValue(unitString);
			frame.add(unit);
		}

		Float accuracy = description.getAccuracy();
		if (accuracy != null) {
			Element ac = new Element();
			ac.name = "Accuracy";
			ac.value = new SingleValue(new FloatValue(accuracy));
			frame.add(ac);
		}

		boolean isBoolean = description.isBoolean();
		Element ib = new Element();
		ib.name = "IsBoolean";
		ib.value = new SingleValue(new BooleanValue(isBoolean));
		frame.add(ib);

		UpperLowerStep upperLowerStep = description.getUpperLowerStep();
		if (upperLowerStep != null) {
			Element upper = new Element();
			upper.name = "Upper";
			upper.value = new SingleValue(new FloatValue(upperLowerStep.getUpper()));
			frame.add(upper);
			Element lower = new Element();
			lower.name = "Lower";
			lower.value = new SingleValue(new FloatValue(upperLowerStep.getLower()));
			frame.add(lower);
			Element step = new Element();
			step.name = "Step";
			step.value = new SingleValue(new FloatValue(upperLowerStep.getStep()));
			frame.add(step);
		}

		Collection<? extends ModeI> modes = description.getModes();
		if (modes != null) {

			ArrayValue arrayValue = new ArrayValue();

			for (ModeI mode : modes) {
				Value value = new SingleValue(mode.getName());
				arrayValue.add(value);
			}

			Element modesElement = new Element();
			modesElement.name = "Modes";
			modesElement.value = arrayValue;

			frame.add(modesElement);
		}
	}
}
