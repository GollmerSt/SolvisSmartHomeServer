package de.sgollmer.solvismax.model.objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.data.SolvisData;

public class AllSolvisData {

	private final Solvis solvis;

	private final Map<String, SolvisData> solvisDatas = new HashMap<>();
	private Map<String, SolvisData> pendingUpdates = new HashMap<>();
	private boolean updatePending = false;
	private int averageCount ;
	private boolean burnerSynchronisation = true ;

	public AllSolvisData(Solvis solvis) {
		this.solvis = solvis;
	}

	public SolvisData get(DataDescription description) {
		String id = description.getId();
		SolvisData data = null ;
		synchronized (this) {
			data = this.solvisDatas.get(id);
			if (data == null) {
				data = new SolvisData(description, this);
				this.solvisDatas.put(id, data);
			}
		}
		return data;
	}

	public SolvisData get(String id ) { 
		SolvisData data = null ;
		synchronized (this) {
			data = this.solvisDatas.get(id);
			if (data == null  ) {
				DataDescription description = solvis.getDataDescription(id) ;
				data = new SolvisData(description, this);
				this.solvisDatas.put(id, data);
			}
		}
		return data;
	}
	
	public SolvisData checkAndGet( String id ) {
		DataDescription description = this.solvis.getDataDescription(id) ;
		if ( description == null ) {
			throw new UnknownError( "Unknown error: <" + id + "> is unknown" ) ;
		}
		return this.get(description) ;
	}

	public void update(SolvisData data) {
		Collection<SolvisData> toUpate = null;
		synchronized (this) {
			this.solvisDatas.put(data.getId(), data);
			if (!this.updatePending) {
				toUpate = this.pendingUpdates.values();
				this.pendingUpdates = new HashMap<>();
			}
		}
		if (toUpate != null) {
			solvis.getSmartHome().send(toUpate, solvis);
		}
	}

	public void setUpdatePending(boolean set) {
		if (set) {
			this.updatePending = true;
		} else {
			Collection<SolvisData> toUpate = null;
			synchronized (this) {
				this.updatePending = false;
				toUpate = this.pendingUpdates.values();
				this.pendingUpdates = new HashMap<>();
			}
			solvis.getSmartHome().send(toUpate, solvis);
		}
	}

	/**
	 * @return the averageCount
	 */
	public int getAverageCount() {
		return averageCount;
	}

	/**
	 * @param averageCount the averageCount to set
	 */
	public void setAverageCount(int averageCount) {
		this.averageCount = averageCount;
	}

	public boolean isBurnerSynchronisation() {
		return burnerSynchronisation;
	}

	public void setBurnerSynchronisation(boolean burnerSynchronisation) {
		this.burnerSynchronisation = burnerSynchronisation;
	}

	/**
	 * @return the solvis
	 */
	public Solvis getSolvis() {
		return solvis;
	}
	
}
