package de.sgollmer.solvismax.model.objects;

public class Dependency {
	private final String id ;
	private final String dependencyId ;
	
	private Dependency( String id, String dependencyId ) {
		this.id = id ;
		this.dependencyId = dependencyId ;
	}

	public String getDependencyId() {
		return dependencyId;
	}

	public String getId() {
		return id;
	}
	
	
}
