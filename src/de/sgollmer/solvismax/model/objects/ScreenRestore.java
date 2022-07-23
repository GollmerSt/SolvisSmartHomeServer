package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import de.sgollmer.solvismax.error.SolvisErrorException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen.GotoStatus;

public class ScreenRestore {

	private static final ILogger logger = LogManager.getInstance().getLogger(ScreenRestore.class);

	private final Solvis solvis;

	private AbstractScreen defaultScreen = null;

	private AbstractScreen savedScreen = null;

	private final Set<Object> inhibitServices = new HashSet<>();

	public ScreenRestore(final Solvis solvis) {
		this.solvis = solvis;
	}

	public void set(final boolean enable, final Object service) {
		if (enable) {
			this.inhibitServices.remove(service);
		} else {
			this.inhibitServices.add(service);
		}
	}

	private boolean isEnabled() {
		return this.inhibitServices.size() == 0;
	}

	public boolean restore() throws IOException, TerminationException, SolvisErrorException {
		
		if ( this.solvis.getSolvisState().isMessageErrorVisible()) {
			return false;
		}

		boolean error = this.solvis.getSolvisState().isMessageError();

		if (this.solvis.isControlEnabled() && !error && !this.isEnabled()) {
			return false;
		}

		AbstractScreen screen;

		if (error) {
			screen = this.solvis.getHomeScreen();
		} else if (this.defaultScreen == null) {
			screen = this.savedScreen;
		} else {
			screen = this.defaultScreen;
		}

		if (screen != null) {
			if (screen.isNoRestore()) {
				screen = this.solvis.getHomeScreen();
			}
			if (screen != SolvisScreen.get(this.solvis.getCurrentScreen(false))) {
				if (screen.goTo(this.solvis) == GotoStatus.CHANGED) {
					logger.info("Screen <" + screen.getId() + "> restored.");
				}
			}
		}
		return true;
	}

	public void save() throws IOException, TerminationException {
		AbstractScreen current = SolvisScreen.get(this.solvis.getCurrentScreen(false));
		if (current == null) {
			current = SolvisScreen.get(this.solvis.getCurrentScreen());
		}
		if (current == null) {
			current = this.solvis.getHomeScreen();
		}
		if (current != this.savedScreen && this.defaultScreen == null) {
			logger.info("Screen <" + current.getId() + "> saved");
			this.savedScreen = current;
		}
	}

	public void setDefault(AbstractScreen defaultScreen) {
		this.defaultScreen = defaultScreen;
	}

}
