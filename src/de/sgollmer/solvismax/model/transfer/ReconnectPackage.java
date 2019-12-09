package de.sgollmer.solvismax.model.transfer;

public class ReconnectPackage extends JsonPackage {
	
	private Integer clientId = null ;
	
	public ReconnectPackage() {
		this.command = Command.RECONNECT ; 
	}
	
	@Override
	public void finish() {
		Frame f = this.data ;
		if ( f.elements.size() > 0 ) {
			Element e = f.elements.get(0) ;
			switch ( e.name ) {
				case "id":
					if ( e.value instanceof SingleValue ) {
						this.clientId = ((SingleValue)e.value).getData().getInt() ;
					}
			}
		}
		this.data = null ;
	}

	public Integer getClientId() {
		return clientId;
	}

}
