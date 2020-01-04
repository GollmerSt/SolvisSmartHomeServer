/************************************************************************
 * 
 * $Id: 73_SolvisClient.pm 78 2020-01-03 17:50:08Z stefa $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.Screen;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class CommandControl implements CommandI {
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
			success = this.getDescription().getValue(solvis);
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
	public Boolean isScreenRestore() {
		return null;
	}

	@Override
	public Handling getHandling(CommandI queueEntry) {
		if (!(queueEntry instanceof CommandControl)) {
			return new Handling(false, false);
		} else {
			CommandControl qCmp = (CommandControl) queueEntry;
			if (!this.description.equals(qCmp.description)) {
				return new Handling(false, false);
			} else {
				if (this.setValue == null) {
					return new Handling(false, true);
				} else {
					return new Handling(true, false);
				}
			}

		}
	}

	@Override
	public boolean first() {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CommandControl)) {
			return false;
		} else {
			CommandControl cmp = (CommandControl) obj;
			return this.description == cmp.description && this.inhibit == cmp.inhibit;
		}
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
