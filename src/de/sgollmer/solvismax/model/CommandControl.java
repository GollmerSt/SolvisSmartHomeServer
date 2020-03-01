/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.helper.AbortHelper;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.Screen;

public class CommandControl extends Command {
	private final ChannelDescription baseDescription;
	private final SingleData<?> setValue;
	private final Screen screen;
	private final Collection<ChannelDescription> descriptions = new ArrayList<>();
	private boolean inhibit = false;
	private boolean baseCommandExecuted = false;

	public CommandControl(ChannelDescription description, SingleData<?> setValue, Solvis solvis) {
		this.screen = description.getScreen(solvis.getConfigurationMask());
		this.baseDescription = description;
		this.setValue = setValue;
		for (ChannelDescription channelDescription : solvis.getSolvisDescription().getChannelDescriptions()
				.getChannelDescriptions(this.screen, solvis)) {
			if (channelDescription != this.baseDescription) {
				this.descriptions.add(channelDescription);
			}
		}
	}

	public CommandControl(ChannelDescription description, Solvis solvis) {
		this(description, null, solvis);
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
	public Screen getScreen(Solvis solvis) {
		return this.screen;
	}

	public boolean execute(Solvis solvis, ChannelDescription description, SingleData<?> setValue)
			throws IOException, ErrorPowerOn {
		boolean success = false;
		SolvisData data = solvis.getAllSolvisData().get(description);
		if (setValue == null) {
			success = description.getValue(solvis, solvis.getTimeAfterLastSwitchingOn());
		} else {
			SolvisData clone = data.clone();
			clone.setSingleData(setValue);
			SingleData<?> setData = description.setValue(solvis, clone);
			if (setData != null) {
				data.setSingleData(setData);
				success = true;
			}
		}
		return success;
	}

	@Override
	public boolean execute(Solvis solvis) throws IOException, ErrorPowerOn {

		boolean result = true;

		if (!baseCommandExecuted) {
			result = this.execute(solvis, this.baseDescription, this.setValue);
		}

		if (result) {
			baseCommandExecuted = true;

			if (this.setValue != null) {
				AbortHelper.getInstance().sleep(1000);
			}

			solvis.clearCurrentScreen();

			for (Iterator<ChannelDescription> it = this.descriptions.iterator(); it.hasNext();) {
				ChannelDescription description = it.next();
				boolean success = this.execute(solvis, description, null);
				if (success) {
					it.remove();
				}
				result &= success;
			}
		}
		return result;
	}

	@Override
	public Handling getHandling(Command queueEntry, Solvis solvis) {
		if (!(queueEntry instanceof CommandControl) || queueEntry.isInhibit()) {
			return new Handling(false, false, false);
		} else {
			CommandControl qCmp = (CommandControl) queueEntry;
			boolean sameScreen = qCmp.getScreen(solvis) == this.getScreen(solvis);
			if ( sameScreen && this.setValue == null && qCmp.setValue == null ) {
					return new Handling(true, false, false) ;
				}
			if (this.baseDescription.equals(qCmp.baseDescription)) {
					return new Handling(this.setValue != null , this.setValue == null, true);
			} else {
				return new Handling(false, false, sameScreen);
			}

		}

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		String prefix = "";
		if (!this.descriptions.isEmpty()) {
			prefix = "  ";
			builder.append("Commands:\n");
		}

		builder.append(prefix);
		this.append(this.baseDescription, this.setValue, builder);

		for (ChannelDescription description : this.descriptions) {
			builder.append('\n');
			builder.append(prefix);
			this.append(description, null, builder);
		}
		return builder.toString();
	}

	public void append(ChannelDescription description, SingleData<?> data, StringBuilder builder) {
		builder.append("Id: ");
		builder.append(description.getId());

		if (data != null) {
			builder.append(", set value: ");
			builder.append(data.toString());
		}
	}
}
