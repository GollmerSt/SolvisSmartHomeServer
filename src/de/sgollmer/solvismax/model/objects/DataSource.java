package de.sgollmer.solvismax.model.objects;

public abstract class DataSource implements DataSourceI {
	
	protected final DataDescription description ;
	
	public DataSource( DataDescription description ) {
		this.description = description ;
	}

	/**
	 * @return the description
	 */
	public DataDescription getDescription() {
		return description;
	}
}
