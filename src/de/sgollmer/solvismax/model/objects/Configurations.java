/************************************************************************
 * 
 * $Id: 73_SolvisClient.pm 78 2020-01-03 17:50:08Z stefa $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Configurations {
	
	private static final String XML_HEATER_LOOPS = "HeaterLoops";

	private final Collection< Configuration > configurations ;
	
	public Configurations( Collection<Configuration> configurations ) {
		this.configurations = configurations ;
	}
	
	public int get( Solvis solvis ) throws IOException {
		int configurationMask = 0 ;
		for ( Configuration configuration : this.configurations ) {
			configurationMask |= configuration.getConfiguration(solvis) ;
		}
		return configurationMask ;
	}
	
	public interface Configuration {
		public int getConfiguration(Solvis solvis) throws IOException ;
	}
	
	public static class Creator extends CreatorByXML<Configurations > {

		private final Collection< Configuration > configurations = new ArrayList<>() ;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
		}

		@Override
		public Configurations create() throws XmlError, IOException {
			return new Configurations(configurations);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart() ;
			switch ( id ) {
				case XML_HEATER_LOOPS:
					return new HeaterLoops.Creator(id, getBaseCreator()) ;
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch ( creator.getId() ) {
				case XML_HEATER_LOOPS:
					this.configurations.add((Configuration) created) ;
					break ;
			}
		}
		
	}
	
}
