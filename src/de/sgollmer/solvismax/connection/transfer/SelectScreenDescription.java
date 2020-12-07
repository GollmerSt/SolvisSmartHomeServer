/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.model.objects.data.BooleanValue;

public class SelectScreenDescription extends Element {

	SelectScreenDescription(String screenId ) {
		this.name = screenId;
		Frame frame = new Frame();
		this.value = frame;

		Element writeableElement = new Element();
		writeableElement.name = "Writeable";
		SingleValue sv = new SingleValue(new BooleanValue(false, -1));
		writeableElement.value = sv;
		frame.add(writeableElement);

		Element typeElement = new Element();
		typeElement.name = "Type";
		sv = new SingleValue("SelectScreen");
		typeElement.value = sv;
		frame.add(typeElement);

	}
}
