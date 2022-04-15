package de.sgollmer.solvismax.smarthome;

import java.util.Collection;
import java.util.Iterator;

import de.sgollmer.solvismax.model.objects.configuration.ConfigurationTypes.Type;

public class MaskIterator implements Iterator<MaskIterator.OneConfiguration> {

	private final Collection<Type> types;
	private final MaskIterator upperIterator;
	private Iterator<Type> current;
	private Long upperMask;
	private String upperComment;

	public MaskIterator(final Collection<Type> types, final MaskIterator upperIterator) {
		this.types = types;
		this.upperIterator = upperIterator;
		this.current = this.types.iterator();
		if (upperIterator != null) {
			OneConfiguration configuration = upperIterator.next();
			this.upperMask = configuration.getMask();
			this.upperComment = configuration.getComment();
		} else {
			this.upperMask = 0L;
			this.upperComment = null;
		}
	}

	@Override
	public boolean hasNext() {
		if (this.current.hasNext()) {
			return true;
		} else if (this.upperIterator != null) {
			return this.upperIterator.hasNext();
		} else {
			return false;
		}
	}

	@Override
	public OneConfiguration next() {
		if (!this.current.hasNext()) {
			OneConfiguration configuration = this.upperIterator.next();
			this.upperMask = configuration.getMask();
			this.upperComment = configuration.getComment();

			this.current = this.types.iterator();
		}
		long mask = this.upperMask;
		Type type = this.current.next();

		String comment;

		if (type.getId() == null) {
			comment = this.upperComment;
		} else if (this.upperComment == null) {
			comment = type.getId();
		} else {
			comment = this.upperComment + " - " + type.getId();
		}

		mask |= type.getConfiguration();

		return new OneConfiguration(mask, comment);

	}

	public static class OneConfiguration {
		private final long mask;
		private final String comment;

		private OneConfiguration(final long mask, final String comment) {
			this.mask = mask;
			this.comment = comment;
		}

		public long getMask() {
			return this.mask;
		}

		public String getComment() {
			return this.comment;
		}
	}

}
