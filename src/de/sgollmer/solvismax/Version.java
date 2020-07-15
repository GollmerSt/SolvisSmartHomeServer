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
    	return "01.01.00, modbus alpha, MQTT beta" ;
    }
    
	public String getServerFormatVersion() {
    	return "01.02" ;
    }

	public String getMqttFormatVersion() {
    	return "01.00" ;
    }

}
