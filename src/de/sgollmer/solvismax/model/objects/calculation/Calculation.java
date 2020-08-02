/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.objects.calculation;

import java.io.IOException;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.DependencyException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.error.XmlException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.ChannelSource;
import de.sgollmer.solvismax.model.objects.IChannelSource;
import de.sgollmer.solvismax.model.objects.Dependencies;
import de.sgollmer.solvismax.model.objects.Dependency;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class Calculation extends ChannelSource {

	private static final String XML_DEPENDENCY = "Dependency";

	private final Strategy<?> strategy;
	private final Dependencies dependencies;

	private Calculation(Strategies strategies, Dependencies dependencies) {
		this.strategy = strategies.create(this);
		this.dependencies = dependencies;
	}

	@Override
	public boolean getValue(SolvisData dest, Solvis solvis) {
		return this.strategy.getValue(dest, solvis);
	}

	@Override
	public SingleData<?> setValue(Solvis solvis, SolvisData value) {
		return this.strategy.setValue(solvis, value);
	}

	@Override
	public boolean isWriteable() {
		return this.strategy.isWriteable();
	}

	@Override
	public boolean isAverage() {
		return false;
	}

	@Override
	public Integer getDivisor() {
		return 1;
	}

	@Override
	public Double getAccuracy() {
		return this.strategy.getAccuracy();

	}

	@Override
	public void instantiate(Solvis solvis) throws AssignmentException, DependencyException {
		this.strategy.instantiate(solvis);
	}

	/**
	 * @return the dependencies
	 */
	Dependencies getDependencies() {
		return this.dependencies;
	}

	public static class Creator extends CreatorByXML<Calculation> {

		private Strategies strategy;
		private final Dependencies dependencies = new Dependencies();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {
			String id = name.getLocalPart();
			switch (id) {
				case "strategy":
					this.strategy = Strategies.getByName(value);
					break;
			}
		}

		@Override
		public Calculation create() throws XmlException {
			return new Calculation(this.strategy, this.dependencies);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_DEPENDENCY:
					return new Dependency.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			switch (creator.getId()) {
				case XML_DEPENDENCY:
					this.dependencies.add((Dependency) created);
			}
		}

	}

	@Override
	public void assign(SolvisDescription description) throws XmlException, AssignmentException, ReferenceException {
		this.dependencies.assign(description);
		if (this.strategy != null) {
			this.strategy.assign(description);
		}

	}

	@Override
	public void learn(Solvis solvis) throws IOException {
	}

	@Override
	public Type getType() {
		return IChannelSource.Type.CALCULATION;
	}

	@Override
	public AbstractScreen getScreen(int configurationMask) {
		return null;
	}

	@Override
	public Collection<? extends IMode> getModes() {
		return null;
	}

	@Override
	public UpperLowerStep getUpperLowerStep() {
		return null;
	}

	@Override
	public boolean isBoolean() {
		return this.strategy.isBoolean();
	}

	@Override
	public SingleData<?> interpretSetData(SingleData<?> singleData) throws TypeException {
		return null;
	}

	@Override
	public boolean isModbus(Solvis solvis) {
		return false;
	}

	@Override
	public ChannelDescription getRestoreChannel(Solvis solvis) {
		return null;
	}

	@Override
	protected SingleData<?> createSingleData(String value) {
		return null;
	}
}
