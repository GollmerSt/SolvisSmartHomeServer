package de.sgollmer.solvismax.model.objects;

public abstract class DataSource implements DataSourceI {

	protected DataDescription description;

	/**
	 * @return the description
	 */
	public DataDescription getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(DataDescription description) {
		this.description = description;
	}
}
