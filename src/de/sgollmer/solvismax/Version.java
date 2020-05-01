/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax;

public class Version {

    public static Version getInstance() {
    	Version version = VersionHolder.INSTANCE;
        return version;
    }

    private static class VersionHolder {

        private static final Version INSTANCE = new Version();
    }
    
	public String getVersion() {
    	return "01.00.07, modbus alpha, two stations beta" ;
    }
    
	public String getFormatVersion() {
    	return "01.02" ;
    }

}
