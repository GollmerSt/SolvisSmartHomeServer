/************************************************************************
 * 
 * $Id: ServerCommand.java 81 2020-01-04 21:05:15Z stefa $
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.connection.transfer;

public class ServerCommandPackage extends JsonPackage {
	
	public ServerCommandPackage() {
		super.command = Command.SERVER_COMMAND ;
	}
	
	public enum ServerCommandEnum{
		BACKUP, SCREEN_RESTORE_INHIBIT, SCREEN_RESTORE_ENABLE
	}
	
	private ServerCommandEnum serverCommandEnum ;
	
	@Override
	public void finish() {
		Frame f = this.data ;
		if (f.size() > 0) {
			Element e = f.get(0) ;
			if ( e.getName().equals("Command")) {
				if (e.value instanceof SingleValue ) {
				this.serverCommandEnum = ServerCommandEnum.valueOf(( ( SingleValue) e.getValue()).getData().toString() );
			}
		}
		this.data = null ;
	}
		
	}

	public ServerCommandEnum getServerCommand() {
		return this.serverCommandEnum;
	}
}
