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

import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * @author rnaegele
 */
public class MemoryBinManager extends BinManager {

	private final Map<Integer, Long> bins = newLinkedHashMap();

	/**
	 * @param binSize
	 *            the bin size
	 */
	public MemoryBinManager(final int binSize) {
		super(binSize);
	}

	@Override
	protected void binningStarted() {
		// no-op
	}

	@Override
	protected void binCompleted(final int bin, final long counter) {
		bins.put(bin, counter);
	}

	public Map<Integer, Long> getBins() {
		return ImmutableMap.copyOf(bins);
	}
}
