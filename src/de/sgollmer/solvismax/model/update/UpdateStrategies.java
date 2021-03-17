/************************************************************************
 * 
 * $Id$
 *
 * 
 ************************************************************************/

package de.sgollmer.solvismax.model.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.AssignmentException;
import de.sgollmer.solvismax.error.ReferenceException;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.ChannelSource;
import de.sgollmer.solvismax.model.objects.IAssigner;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.xmllibrary.BaseCreator;
import de.sgollmer.xmllibrary.CreatorByXML;
import de.sgollmer.xmllibrary.XmlException;

public class UpdateStrategies implements IAssigner {

	private final Collection<Strategy<?>> strategies;

	public UpdateStrategies(Collection<Strategy<?>> strategies) {
		this.strategies = strategies;
	}

	public void setSource(ChannelSource source) {
		for (Strategy<?> strategy : this.strategies) {
			strategy.setSource(source);
		}
	}

	public enum StrategyEnum {

		HUMAN_ACCESS(new HumanAccess.Creator(), "HumanAccess"),
		EQUIPMENT_ON_OFF(new EquipmentOnOff.Creator(), "EquipmentOnOff");

		private final UpdateCreator<?> creator;
		private final String xmlId;

		private StrategyEnum(UpdateCreator<?> creator, String xmlId) {
			this.creator = creator;
			this.xmlId = xmlId;
		}

		public CreatorByXML<?> getStrategy() {
			return this.creator;
		}

		public static UpdateCreator<?> byXml(String xmlId) {
			for (StrategyEnum strategy : StrategyEnum.values()) {
				if (strategy.xmlId.equals(xmlId)) {
					return strategy.creator;
				}
			}
			return null;
		}
	}

	public static abstract class Strategy<S extends Strategy<?>> implements IAssigner {

		protected ChannelSource source;

		public abstract void instantiate(Solvis solvis);

		/**
		 * @param source the source to set
		 */
		public void setSource(ChannelSource source) {
			this.source = source;
		}

		public boolean isHumanAccessDependend() {
			return false;
		}

	}
	
	public interface IExecutable {
		public void trigger();
		public String getTriggerId();
	}

	public static class Creator extends CreatorByXML<UpdateStrategies> {

		private final Collection<Strategy<?>> updateStrategies = new ArrayList<>();

		public Creator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		@Override
		public void setAttribute(QName name, String value) {

		}

		@Override
		public UpdateStrategies create() throws XmlException, IOException {
			return new UpdateStrategies(this.updateStrategies);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			UpdateCreator<?> creator = StrategyEnum.byXml(id);
			if (creator != null) {
				return creator.createCreator(id, getBaseCreator());
			} else {
				return null;
			}
		}

		@Override
		public void created(CreatorByXML<?> creator, Object created) {
			if (StrategyEnum.byXml(creator.getId()) != null) {
				this.updateStrategies.add((Strategy<?>) created);
			}
		}

	}

	public static abstract class UpdateCreator<T> extends CreatorByXML<T> {

		public UpdateCreator(String id, BaseCreator<?> creator) {
			super(id, creator);
		}

		public abstract UpdateCreator<T> createCreator(String id, BaseCreator<?> creator);
	}

	@Override
	public void assign(SolvisDescription description) throws XmlException, AssignmentException, ReferenceException {
		for (Strategy<?> strategy : this.strategies) {
			strategy.assign(description);
		}

	}

	public void instantiate(Solvis solvis) {
		for (Strategy<?> strategy : this.strategies) {
			strategy.instantiate(solvis);
		}
	}

	public boolean isHumanAccessDependend() {
		for (Strategy<?> strategy : this.strategies) {
			if (strategy.isHumanAccessDependend()) {
				return true;
			}
		}
		return false;
	}

}
