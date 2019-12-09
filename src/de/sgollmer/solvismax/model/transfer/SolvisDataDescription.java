package de.sgollmer.solvismax.model.transfer;

import java.util.Collection;

import de.sgollmer.solvismax.model.objects.DataDescription;
import de.sgollmer.solvismax.model.objects.data.BooleanValue;
import de.sgollmer.solvismax.model.objects.data.FloatValue;
import de.sgollmer.solvismax.model.objects.data.ModeI;

public class SolvisDataDescription extends Element {
	public SolvisDataDescription(DataDescription description) {
		this.name = description.getId();
		Frame frame = new Frame();
		this.value = frame;

		boolean writable = description.isWriteable();
		Element writeable = new Element();
		writeable.name = "Writeable";
		SingleValue sv = new SingleValue(new BooleanValue(writable));
		writeable.value = sv;
		frame.add(writeable);

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
