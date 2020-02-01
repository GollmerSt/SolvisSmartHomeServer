/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public class CommandControl extends Command {
	private final ChannelDescription description;
	private final SingleData<?> setValue;
	private boolean inhibit = false;

	public CommandControl(ChannelDescription description, SingleData<?> setValue) {
		this.description = description;
		this.setValue = setValue;
	}

	public CommandControl(ChannelDescription description) {
		this(description, null);
	}

	public SingleData<?> getSetValue() {
		return setValue;
	}

	public ChannelDescription getDescription() {
		return description;
	}

	@Override
	public boolean isInhibit() {
		return inhibit;
	}

	@Override
	public void setInhibit(boolean inhibit) {
		this.inhibit = inhibit;
	}

	@Override
	public String toString() {
		return "Id: " + description.getId();
	}

	@Override
	public Screen getScreen(Solvis solvis) {
		return this.getDescription().getScreen(solvis.getConfigurationMask());
	}

	@Override
	public boolean execute(Solvis solvis) throws IOException, ErrorPowerOn {
		boolean success = false;
		SolvisData data = solvis.getAllSolvisData().get(description);
		if (this.getSetValue() == null) {
			success = this.getDescription().getValue(solvis, solvis.getTimeAfterLastSwitchingOn());
		} else {
			SolvisData clone = data.clone();
			clone.setSingleData(this.getSetValue());
			success = this.getDescription().setValue(solvis, clone);
			if (success) {
				data.setSingleData(this.getSetValue());
			}
		}
		return success;
	}

	@Override
	public Handling getHandling(Command queueEntry, Solvis solvis) {
		if (!(queueEntry instanceof CommandControl) || queueEntry.isInhibit()) {
			return new Handling(false, false, false);
		} else {
			boolean sameScreen = queueEntry.getScreen(solvis) == this.getScreen(solvis);
			CommandControl qCmp = (CommandControl) queueEntry;
			if (!this.description.equals(qCmp.description)) {
				return new Handling(false, false, sameScreen);
			} else {
				if (this.setValue == null) {
					return new Handling(false, true, sameScreen);
				} else {
					return new Handling(true, false, sameScreen);
				}
			}

		}
	}

}
