package de.sgollmer.solvismax.model.objects;

import java.io.IOException;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.ErrorPowerOn;
import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.calculation.Calculation;
import de.sgollmer.solvismax.model.objects.control.Control;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.measure.Measurement;
import de.sgollmer.solvismax.xml.CreatorByXML;
import de.sgollmer.solvismax.xml.BaseCreator;

public class DataDescription implements DataSourceI, Assigner {
	private final String id;
	private final DataSource dataSource;

	public DataDescription(String id, DataSource dataSource) {
		this.id = id;
		this.dataSource = dataSource;
	}

	public String getId() {
		return this.id;
	}

	@Override
	public boolean getValue(SolvisData dest, Solvis solvis) throws IOException, ErrorPowerOn {
		return this.dataSource.getValue(dest, solvis);
	}

	@Override
	public boolean setValue(Solvis solvis, SolvisData value) throws IOException {
		return this.dataSource.setValue(solvis, value);
	}

	@Override
	public boolean isWriteable() {
		return this.dataSource.isWriteable();
	}

	@Override
	public boolean isAverage() {
		return this.dataSource.isAverage();
	}

	@Override
	public Integer getDivisor() {
		return this.dataSource.getDivisor();
	}

	@Override
	public String getUnit() {
		return this.dataSource.getUnit();
	}

	@Override
	public void assign(SolvisDescription description ) {
		this.dataSource.assign(description);

	}

	@Override
	public void instantiate(Solvis solvis) {
		this.dataSource.instantiate(solvis);

	}

	public static class Creator extends CreatorByXML<DataDescription> {

		private String id;
		private DataSource dataSource;

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			if ( name.getLocalPart().equals("id")) {
				this.id = value ;
			}
			
		}

		@Override
		public DataDescription create() throws XmlError {
			DataDescription description = new DataDescription(id, dataSource);
			dataSource.setDescription(description);
			return description ;
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String source = name.getLocalPart() ;
			switch( source ) {
				case "Control":
					return new Control.Creator(source, this.getBaseCreator()) ;
				case "Measurement":
					return new Measurement.Creator(source, this.getBaseCreator()) ;
				case "Calculation":
					return new Calculation.Creator(source, this.getBaseCreator()) ;
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch( creator.getId() ) {
				case "Control":
				case "Measurement":
				case "Calculation":
					this.dataSource = (DataSource) created ; ;
			}
			
		}

	}

	@Override
	public void createAndAddLearnScreen(LearnScreen learnScreen, Collection<LearnScreen> learnScreens) {
		dataSource.createAndAddLearnScreen(null, learnScreens);
		
	}

	@Override
	public void learn(Solvis solvis) throws IOException {
		this.dataSource.learn(solvis);
		
	}

	@Override
	public Type getType() {
		return this.dataSource.getType();
	}

	@Override
	public Screen getScreen() {
		return this.dataSource.getScreen();
	}

}
