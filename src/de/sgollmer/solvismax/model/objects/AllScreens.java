package de.sgollmer.solvismax.model.objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.imagepatternrecognition.image.MyImage;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class AllScreens implements GraficsLearnable {
	private Map< String, Screen > screens = new HashMap<>() ;
	
	public void add( Screen screen ) {
		this.screens.put(screen.getId(), screen) ;
	}
	
	public Screen get( String id ) {
		return this.screens.get(id) ;
	}
	
	public Screen getScreen(MyImage image, Solvis solvis) {
		for ( Screen screen : screens.values()) {
			if ( screen.isScreen(image, solvis)) {
				return screen ;
			}
		}
		return null ;
	}
	
	public void assign( SolvisDescription description ) {
		for ( Screen screen : screens.values() ) {
			screen.assign(description);
		}
	}
	
	public static class Creator extends CreatorByXML<AllScreens> {
		
		private AllScreens allScreens = new AllScreens() ;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public AllScreens create() throws XmlError {
			return allScreens ;
		}

		@Override
		public CreatorByXML<Screen> getCreator(QName name) {
			return new Screen.Creator( name.getLocalPart(), this.getBaseCreator() );
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			allScreens.add((Screen) created);
			
		}
	}

	@Override
	public void createAndAddLearnScreen(LearnScreen learnScreen, Collection<LearnScreen> learnScreens) {
		for ( Screen screen : this.screens.values() ) {
			screen.createAndAddLearnScreen(null, learnScreens);
		}
	}

	@Override
	public void learn(Solvis solvis) {
		
	}

}
