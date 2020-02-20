package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.screen.Screen;
import de.sgollmer.solvismax.model.objects.screen.ScreenRef;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Service implements Assigner {
	
	private static final String XML_SERVICE_SCREEN = "ServiceScreen";
	
	private final Collection< ScreenRef > serviceScreenRefs ;
		
	public Service ( Collection< ScreenRef > serviceScreenRefs) {
		this.serviceScreenRefs = serviceScreenRefs ;
	}
	
	public boolean isServiceScreen( Screen screen, Solvis solvis ) {
		for ( ScreenRef ref : this.serviceScreenRefs ) {
			Screen cmp = ref.getScreen().get(solvis) ;
			if ( cmp != null &&  cmp == screen ) {
				return true ;
			}
		}
		return false ;
	}
	
	public static class Creator extends CreatorByXML<Service> {
		private final Collection< ScreenRef > serviceScreenRefs = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public Service create() throws XmlError, IOException {
			return new Service(serviceScreenRefs);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart() ;
			switch( id) {
				case XML_SERVICE_SCREEN:
					return new ScreenRef.Creator(id, getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_SERVICE_SCREEN:
					this.serviceScreenRefs.add((ScreenRef) created) ;
			}
		}
		
	}

	@Override
	public void assign(SolvisDescription description) {
		for ( ScreenRef ref : this.serviceScreenRefs ) {
			ref.assign(description);
		}
		
	}
}