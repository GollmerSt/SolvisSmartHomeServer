/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.SolvisErrorException;
import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.ILogger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.SolvisScreen;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class FallBack {

	private static final ILogger logger = LogManager.getInstance().getLogger(FallBack.class);

	private static final String XML_BACK = "Back";
	private static final String XML_SCREENREF = "ScreenRef";
	private static final String XML_LAST_CHANCE = "LastChance";

	private final Collection<IFallBackObject> sequence;
	private final FallBack lastChance;

	private interface IFallBackObject {
		void execute(final Solvis solvis) throws IOException, TerminationException, SolvisErrorException;
	}

	private FallBack(final Collection<IFallBackObject> sequence, final FallBack lastChance) {
		this.sequence = sequence;
		this.lastChance = lastChance;
	}

	public void execute(final Solvis solvis, final boolean lastChance)
			throws IOException, TerminationException, SolvisErrorException {
		if (lastChance && this.lastChance != null) {
			this.lastChance.execute(solvis, false);
		} else {
			for (IFallBackObject obj : this.sequence) {
				obj.execute(solvis);
			}
		}
	}

	static class Creator extends CreatorByXML<FallBack> {

		private Collection<IFallBackObject> sequence = new ArrayList<>();
		private FallBack lastChance = null;
		private final boolean inner;

		Creator(final String id, final BaseCreator<?> creator, boolean inner) {
			super(id, creator);
			this.inner = inner;
		}

		@Override
		public void setAttribute(final QName name, final String value) {
		}

		@Override
		public FallBack create() throws XmlException, IOException {
			return new FallBack(this.sequence, this.lastChance);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_BACK:
					return new Back.Creator(id, this.getBaseCreator());
				case XML_SCREENREF:
					return new ScreenRef.Creator(id, this.getBaseCreator());
				case XML_LAST_CHANCE:
					if (!this.inner) {
						return new Creator(id, this.getBaseCreator(), true);
					}
					break;
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_BACK:
					this.sequence.add((Back) created);
					break;
				case XML_SCREENREF:
					this.sequence.add((ScreenRef) created);
					break;
				case XML_LAST_CHANCE:
					this.lastChance = (FallBack) created;
					break;
			}

		}

	}

	private static class ScreenRef extends de.sgollmer.solvismax.model.objects.screen.ScreenRef
			implements IFallBackObject {
		private ScreenRef(final String id) {
			super(id);
		}

		@Override
		public void execute(final Solvis solvis) throws IOException, TerminationException, SolvisErrorException {
			Screen screen = (Screen) this.getScreen().getIfSingle();

			if (screen == null) {
				logger.error("The screen < " + this.getId()
						+ "> is not possible in the FallBack Element of the XML, because it's not unique over all configurations. Ignored");
				return;
			}

			screen.getSelectScreenStrategy().execute(solvis, SolvisScreen.get(solvis.getCurrentScreen()));
		}

		private static class Creator extends de.sgollmer.solvismax.model.objects.screen.ScreenRef.Creator {

			private Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public ScreenRef create() throws XmlException, IOException {
				return new ScreenRef(this.id);
			}

		}

	}

	private static class Back implements IFallBackObject {

		private Back() {

		}

		@Override
		public void execute(Solvis solvis) throws IOException, TerminationException, SolvisErrorException {
			solvis.sendBackWithCheckError();
		}

		private static class Creator extends CreatorByXML<Back> {

			private Creator(final String id, final BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(final QName name, final String value) {

			}

			@Override
			public Back create() throws XmlException, IOException {
				return new Back();
			}

			@Override
			public CreatorByXML<?> getCreator(final QName name) {
				return null;
			}

			@Override
			public void created(final CreatorByXML<?> creator, final Object created) {
			}

		}

	}

}
