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
    
    @SuppressWarnings("static-method")
	public String getVersion() {
    	return "00.10.03" ;
    }
    
    @SuppressWarnings("static-method")
	public String getFormatVersion() {
    	return "01.02" ;
    }

}
