package de.sgollmer.solvismax.model.objects.data;

import de.sgollmer.solvismax.helper.Helper.Boolean;

public class TwoValues extends SingleData<TwoValues> {
	
	private final SingleData<?> first;
	private  final SingleData<?> second;

	public TwoValues(final SingleData<?> first, final SingleData<?> second, final long timeStamp) {
		super(timeStamp);
		this.first = first;
		this.second = second;
	}

	@Override
	public int compareTo(SingleData<?> cmp) {
		if ( !(cmp instanceof TwoValues ) ) {
			return 1;
		}
		int c = this.first.compareTo(((TwoValues)cmp).first);
		if ( c!=0) {
			return c;
		} else {
			return this.second.compareTo(((TwoValues)cmp).second);
		}
	}

	@Override
	public Integer getInt() {
		return null;
	}

	@Override
	public Long getLong() {
		return null;
	}

	@Override
	public Double getDouble() {
		return null;
	}

	@Override
	public Boolean getBoolean() {
		return null;
	}

	@Override
	SingleData<TwoValues> create(int value, long timeStamp) {
		return null;
	}

	@Override
	public SingleData<TwoValues> create(long timeStamp) {
		return null;
	}

	@Override
	public String getXmlId() {
		return null;
	}

	@Override
	public String toJson() {
		return null;
	}

	@Override
	public TwoValues get() {
		return this;
	}

	public SingleData<?> getFirst() {
		return this.first;
	}

	public SingleData<?> getSecond() {
		return this.second;
	}

}
