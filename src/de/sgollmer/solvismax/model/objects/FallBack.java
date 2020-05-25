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

import de.sgollmer.solvismax.error.TerminationException;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.log.LogManager;
import de.sgollmer.solvismax.log.LogManager.Logger;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class FallBack implements Assigner {
	
	private static final Logger logger = LogManager.getInstance().getLogger(FallBack.class) ;

	private static final String XML_BACK = "Back";
	private static final String XML_SCREENREF = "ScreenRef";

	private final Collection<FallBackObject> sequence;

	private interface FallBackObject extends Assigner {
		public void execute(Solvis solvis) throws IOException;
	}

	public FallBack(Collection<FallBackObject> sequence) {
		this.sequence = sequence;
	}

	public void execute(Solvis solvis) throws IOException {
		for (FallBackObject obj : this.sequence) {
			obj.execute(solvis);
		}
	}

	@Override
	public void assign(SolvisDescription description) {
		for (FallBackObject obj : this.sequence) {
			obj.assign(description);
		}
		
	}
	public static class Creator extends CreatorByXML<FallBack> {

		private Collection<FallBackObject> sequence = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public FallBack create() throws XmlError, IOException {
			return new FallBack(this.sequence);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_BACK:
					return new Back.Creator(id, this.getBaseCreator());
				case XML_SCREENREF:
					return new ScreenRef.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_BACK:
					this.sequence.add((Back) created);
					break;
				case XML_SCREENREF:
					this.sequence.add((ScreenRef) created);
					break;
			}

		}

	}

	public static class ScreenRef extends de.sgollmer.solvismax.model.objects.screen.ScreenRef implements FallBackObject {
		public ScreenRef(String id) {
			super( id ) ;
		}

		@Override
		public void execute(Solvis solvis) throws IOException, TerminationException {
			Screen screen = this.getScreen().getIfSingle();
			
			if ( screen == null ) {
				logger.error( "The screen < " + this.getId() +  "> is not possible in the FallBack Element of the XML, because it's not unique over all configurations. Ignored");
			}
			
			solvis.send(screen.getTouchPoint());
		}
		public static class Creator extends de.sgollmer.solvismax.model.objects.screen.ScreenRef.Creator {

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public ScreenRef create() throws XmlError, IOException {
				return new ScreenRef(this.id);
			}

		}

	}

	public static class Back implements FallBackObject {

		@Override
		public void execute(Solvis solvis) throws IOException {
			solvis.sendBack();
		}

		public static class Creator extends CreatorByXML<Back> {

			public Creator(String id, BaseCreator<?> creator) {
				super(id, creator);
			}

			@Override
			public void setAttribute(QName name, String value) {

			}

			@Override
			public Back create() throws XmlError, IOException {
				return new Back();
			}

			@Override
			public CreatorByXML<?> getCreator(QName name) {
				return null;
			}

			@Override
			public void created(CreatorByXML<?> creator, Object created) {
			}

		}

		@Override
		public void assign(SolvisDescription description) {
		}

	}


}
