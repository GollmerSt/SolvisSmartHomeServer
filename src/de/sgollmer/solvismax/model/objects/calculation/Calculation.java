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

import de.sgollmer.solvismax.Constants.Csv;
import de.sgollmer.solvismax.error.AliasException;
import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.error.TypeException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Alias;
import de.sgollmer.solvismax.model.objects.ChannelDescription;
import de.sgollmer.solvismax.model.objects.ChannelSource;
import de.sgollmer.solvismax.model.objects.IChannelSource;
import de.sgollmer.solvismax.model.objects.IInstance;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.model.objects.calculation.Strategies.Strategy;
import de.sgollmer.solvismax.model.objects.data.IMode;
import de.sgollmer.solvismax.model.objects.data.SingleData;
import de.sgollmer.solvismax.model.objects.data.SolvisData;
import de.sgollmer.solvismax.model.objects.screen.AbstractScreen;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class Calculation extends ChannelSource {

	private static final String XML_ALIAS = "Alias";

	private final Strategy<?> strategy;
	private final AliasGroup aliasGroup;

	private Calculation(final Strategies strategies, final AliasGroup aliasGroup) {
		this.strategy = strategies.create(this);
		this.aliasGroup = aliasGroup;
	}

	@Override
	public boolean getValue(final SolvisData dest, final Solvis solvis, final long executionStartTime) {
		return this.strategy.getValue(dest, solvis);
	}

	@Override
	public SetResult setValue(final Solvis solvis, final SolvisData value) {
		return this.strategy.setValue(solvis, value);
	}

	@Override
	public SetResult setDebugValue(final Solvis solvis, final SingleData<?> value) {
		return this.strategy.setDebugValue(solvis, value);
	}

	@Override
	public SetResult setValueFast(final Solvis solvis, final SolvisData value) {
		return null;
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
	public IInstance instantiate(final Solvis solvis) throws AssignmentException, AliasException {
		this.strategy.instantiate(solvis);
		return null;
	}

	/**
	 * @return the aliasGroup
	 */
	AliasGroup getAliasGroup() {
		return this.aliasGroup;
	}

	public static class Creator extends CreatorByXML<Calculation> {

		private Strategies strategy;
		private final AliasGroup aliasGroup = new AliasGroup();

		public Creator(final String id, final BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(final QName name, final String value) {
			String id = name.getLocalPart();
			switch (id) {
				case "strategy":
					this.strategy = Strategies.getByName(value);
					break;
			}
		}

		@Override
		public Calculation create() throws XmlException {
			return new Calculation(this.strategy, this.aliasGroup);
		}

		@Override
		public CreatorByXML<?> getCreator(final QName name) {
			String id = name.getLocalPart();
			switch (id) {
				case XML_ALIAS:
					return new Alias.Creator(id, this.getBaseCreator());
			}
			return null;
		}

		@Override
		public void created(final CreatorByXML<?> creator, final Object created) {
			switch (creator.getId()) {
				case XML_ALIAS:
					this.aliasGroup.add((Alias) created);
			}
		}

	}

	@Override
	public void assign(final SolvisDescription description)
			throws XmlException, AssignmentException, ReferenceException {
		this.aliasGroup.assign(description);
		if (this.strategy != null) {
			this.strategy.assign(description);
		}

	}

	@Override
	public void learn(final Solvis solvis) throws IOException {
	}

	@Override
	public Type getType() {
		return IChannelSource.Type.CALCULATION;
	}

	@Override
	public AbstractScreen getScreen(final Solvis solvis) {
		return null;
	}

	@Override
	public Collection<? extends IMode<?>> getModes() {
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
	public SingleData<?> interpretSetData(final SingleData<?> singleData) throws TypeException {
		return this.strategy.interpretSetData(singleData);
	}

	@Override
	public ChannelDescription getRestoreChannel(final Solvis solvis) {
		return null;
	}

	@Override
	protected SingleData<?> createSingleData(final String value, final long timeStamp) {
		return null;
	}

	@Override
	public de.sgollmer.solvismax.model.objects.control.DependencyGroup getDependencyGroup() {
		return null;
	}

	@Override
	public boolean mustBackuped() {
		return true;
	}

	@Override
	public boolean isDelayed(final Solvis solvis) {
		return this.getAliasGroup().isDelayed(solvis);
	}

	@Override
	public Integer getScanInterval_ms(final Solvis solvis) {
		return this.aliasGroup.getScanInterval_ms(solvis);
	}

	@Override
	public String getCsvMeta(final String column, final boolean semicolon) {
		switch (column) {
			case Csv.STRATEGY:
				return this.strategy.getClass().getSimpleName();
		}
		return this.strategy.getCsvMeta(column, semicolon);
	}

}
