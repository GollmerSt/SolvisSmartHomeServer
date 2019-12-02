package de.sgollmer.solvismax.model.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import de.sgollmer.solvismax.error.XmlError;
import de.sgollmer.solvismax.model.Solvis;
import de.sgollmer.solvismax.model.objects.Assigner;
import de.sgollmer.solvismax.model.objects.DataSource;
import de.sgollmer.solvismax.model.objects.SolvisDescription;
import de.sgollmer.solvismax.xml.BaseCreator;
import de.sgollmer.solvismax.xml.CreatorByXML;

public class UpdateStrategies implements Assigner {

	private final Collection<Strategy<?>> strategies;

	public UpdateStrategies(Collection<Strategy<?>> strategies) {
		this.strategies = strategies;
	}

	public enum StrategyEnum {

		SCREEN_CHANGED(new ByScreenChange.Creator(), "ScreenChange"),
		BURNER_OFF_SOMETIMES(new HeatingBurner.Creator(), "HeatingBurner");

		private final UpdateCreator<?> creator;
		private final String xmlId;

		private StrategyEnum(UpdateCreator<?> creator, String xmlId) {
			this.creator = creator;
			this.xmlId = xmlId;
		}

		public CreatorByXML<?> getStrategy() {
			return creator;
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

	public static abstract class Strategy<S extends Strategy<?>> implements Assigner {

		protected DataSource source;

		public abstract void instantiate(Solvis solvis);

		/**
		 * @param source
		 *            the source to set
		 */
		public void setSource(DataSource source) {
			this.source = source;
		}

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
		public UpdateStrategies create() throws XmlError, IOException {
			return new UpdateStrategies(updateStrategies);
		}

		@Override
		public CreatorByXML<?> getCreator(QName name) {
			String id = name.getLocalPart();
			return StrategyEnum.byXml(id).createCreator(id, getBaseCreator());
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
	public void assign(SolvisDescription description) {
		for (Strategy<?> strategy : this.strategies) {
			strategy.assign(description);
		}

	}

	public void instantiate(Solvis solvis) {
		for (Strategy<?> strategy : this.strategies) {
			strategy.instantiate(solvis);
		}

	}

}
