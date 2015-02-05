package com.mgmtp.perfload.perfalyzer.reportpreparation;

/**
 * @author rnaegele
 */
public class DataRange {

	private final long lowerMillis;
	private final long upperMillis;

	public DataRange(final long lowerMillis, final long upperMillis) {
		this.lowerMillis = lowerMillis;
		this.upperMillis = upperMillis;
	}

	public long getLowerMillis() {
		return lowerMillis;
	}

	public long getUpperMillis() {
		return upperMillis;
	}

	public long getLowerSeconds() {
		return lowerMillis / 1000L;
	}

	public long getUpperSeconds() {
		return upperMillis / 1000L;
	}
}
