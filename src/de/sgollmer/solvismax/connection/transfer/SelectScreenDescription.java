/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

import de.sgollmer.solvismax.model.objects.data.BooleanValue;

public class SelectScreenDescription extends Element {

	SelectScreenDescription(final String screenId) {
		super(screenId);
		Frame frame = new Frame();
		this.setValue(frame);
		
		Element writeableElement = new Element("Writeable", new SingleValue(new BooleanValue(false, -1L)));
		frame.add(writeableElement);

		Element typeElement = new Element("Type", new SingleValue("SelectScreen"));
		frame.add(typeElement);

	}
}
