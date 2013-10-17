/*
 * Copyright (c) 2013 mgm technology partners GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mgmtp.perfload.perfalyzer.binning;

import org.apache.commons.lang3.mutable.MutableLong;
import org.joda.time.Duration;

/**
 * Encapsulates the actual binning logic.
 * 
 * @author rnaegele
 */
public abstract class BinManager {

	protected final int binSize;
	private MutableLong binCounter;
	protected int binIndex;
	private long lastBinStartMillis;
	protected boolean starting = true;

	/**
	 * @param binSize
	 *            the bin size
	 */
	public BinManager(final int binSize) {
		this.binSize = binSize;
	}

	/**
	 * Adds a timestamp incrementing the internal counter.
	 * 
	 * @param timestampMillis
	 *            the timestamp in milliseconds
	 */
	public final void addTimestamp(final long timestampMillis) {
		addTimestamp(timestampMillis, true);
	}

	/**
	 * Adds a timestamp incrementing to the correct bin incremeting the bin counter if desired. Bins
	 * are automatically created ({@link #binningStarted()} and completed (
	 * {@link #binCompleted(int, long)} as necessary.
	 * 
	 * @param timestampMillis
	 *            the timestamp in milliseconds
	 * @param incrementBinCounter
	 *            if {@code true}, the internal counter is incremented
	 */
	public final void addTimestamp(final long timestampMillis, final boolean incrementBinCounter) {
		if (binCounter == null) {
			binCounter = new MutableLong();

			// determine index of the bin before the first bin that has data
			int lastInitialEmptyBinIndex = (int) timestampMillis / binSize;

			binningStarted();

			// create empty bins at the beginning if necessary
			for (int i = 0; i < lastInitialEmptyBinIndex; ++i) {
				binCompleted(computeBin(), 0L);
			}

			// align with bin size
			lastBinStartMillis = binIndex * binSize;
			starting = false;
		}

		Duration duration = new Duration(lastBinStartMillis, timestampMillis);
		long durationMillis = duration.getMillis();
		if (durationMillis >= binSize) {
			int factor = (int) durationMillis / binSize;
			for (int i = 0; i < factor; ++i) {
				binCompleted(computeBin(), binCounter.getValue());
				binCounter.setValue(0);
			}
			lastBinStartMillis = binIndex * binSize;
		}

		if (incrementBinCounter) {
			binCounter.increment();
		}
	}

	/**
	 * Closes the last bin if it has a counter greater than zero calling
	 * {@link #binCompleted(int, long)}. This method should always be called explicitly after
	 * binning in order to ensure that the last bin is closed.
	 */
	public void completeLastBin() {
		if (binCounter != null && binCounter.getValue() > 0) {
			binCompleted(computeBin(), binCounter.getValue());
		}
	}

	private int computeBin() {
		return binIndex++ * binSize / 1000;
	}

	/**
	 * Executed after binning has started.
	 */
	protected abstract void binningStarted();

	/**
	 * Executes whenever a bin is full.
	 * 
	 * @param bin
	 *            the bin
	 * @param counter
	 *            the counter representing the number of elements in the bin
	 */
	protected abstract void binCompleted(int bin, long counter);
}
