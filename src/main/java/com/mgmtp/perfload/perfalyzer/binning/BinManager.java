/*
 * Copyright (c) 2013-2014 mgm technology partners GmbH
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

import com.google.common.base.Charsets;
import com.mgmtp.perfload.perfalyzer.util.AggregationType;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.math3.stat.StatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.WritableByteChannel;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.LongStream.Builder;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.writeLineToChannel;
import static com.mgmtp.perfload.perfalyzer.util.StrBuilderUtils.appendEscapedAndQuoted;
import static java.util.stream.IntStream.range;

/**
 * Encapsulates the actual binning logic.
 *
 * @author rnaegele
 */
public class BinManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(BinManager.class);

	private final double domainStart;
	private final List<Bin> bins = new ArrayList<>(50);
	private final int binSize;
	private final int indexOffset;

	/**
	 * @param domainStart
	 * 		the domain value where binning starts
	 * @param binSize
	 * 		the bin size
	 */
	public BinManager(final double domainStart, final int binSize) {
		this.domainStart = domainStart;
		this.binSize = binSize;
		this.indexOffset = (int) Math.ceil(domainStart / binSize);
	}

	/**
	 * Adds a value to be binned. This in fact incremets the count of the bin the domain value fits in. No range value is
	 * added to the bin. The specified value must be greater than or equal to the {@code domainStart} value specified
	 * in the constructor.
	 *
	 * @param domainValue
	 * 		the domain value
	 */
	public void addValue(final double domainValue) {
		addValue(domainValue, null);
	}

	/**
	 * Adds a value to be binned. This in fact incremets the count of the bin the domain value fits in. Additionally, a range
	 * values is added to the bin's list of range values for later per-bin aggragation. The specified domain value must be
	 * greater than or equal to the {@code domainStart} value specified in the constructor.
	 *
	 * @param domainValue
	 * @param rangeValue
	 */
	public void addValue(final double domainValue, final Double rangeValue) {
		double offset = domainValue - domainStart;
		checkState(offset >= 0, "Cannot add rangeValue to a bin [rangeValue (%s) < start of domain (%s)].", domainValue,
				domainStart);

		// calculate bin index for the rangeValue
		int binIndexInRange = (int) (offset / binSize);
		int existingBins = bins.size();
		Bin bin;

		if (existingBins <= binIndexInRange) {
			// create missing empty bins
			range(existingBins, binIndexInRange).forEach(i -> bins.add(new Bin(i + indexOffset)));

			// create new bin
			bin = new Bin(binIndexInRange + indexOffset);
			bins.add(bin);
		} else {
			bin = bins.get(binIndexInRange);
		}

		bin.counter++;
		if (rangeValue != null) {
			bin.values.add(rangeValue);
		}
	}

	/**
	 * Creates a {@link java.util.stream.LongStream} with the bin counts as its source.
	 *
	 * @return the stream
	 */
	public LongStream countStream() {
		Builder builder = LongStream.builder();
		bins.forEach(bin -> builder.add(bin.counter));
		return builder.build();
	}

	/**
	 * Creates a {@link java.util.stream.Stream} with the bins as its source.
	 *
	 * @return the stream
	 */
	public Stream<Bin> binStream() {
		return bins.stream();
	}

	/**
	 * Creates a {@link java.util.stream.Stream} with the bins' lists of values as its source.
	 *
	 * @return the stream
	 */
	public Stream<List<Double>> valuesStream() {
		return bins.stream().map(Bin::getValues);
	}


	/**
	 * Creates a {@link java.util.stream.DoubleStream} with flattened bin values as its source.
	 *
	 * @return
	 */
	public DoubleStream flatValuesStream() {
		return valuesStream().flatMapToDouble(doubles -> doubles.stream().mapToDouble(d -> d));
	}


	/**
	 * Writes the bins as CSV to the specified channel. The bin counts are used as range values.
	 *
	 * @param destChannel
	 * 		the channel to write to
	 * @param domainHeader
	 * 		the domain header
	 * @param rangeHeader
	 * 		the range header
	 * @param numberFormat
	 * 		the number format
	 */
	public void toCsv(final WritableByteChannel destChannel, final String domainHeader, final String rangeHeader,
			final NumberFormat numberFormat) {
		toCsv(destChannel, domainHeader, rangeHeader, numberFormat, AggregationType.COUNT);
	}

	/**
	 * Writes the bins as CSV to the specified channel. The range values are aggregated per bin using the specified aggregation
	 * type.
	 *
	 * @param destChannel
	 * 		the channel to write to
	 * @param domainHeader
	 * 		the domain header
	 * @param rangeHeader
	 * 		the range header
	 * @param numberFormat
	 * 		the number format
	 * @param aggregationType the aggregation type
	 */
	public void toCsv(final WritableByteChannel destChannel, final String domainHeader, final String rangeHeader,
			final NumberFormat numberFormat, final AggregationType aggregationType) {
		StrBuilder sb = new StrBuilder(50);
		appendEscapedAndQuoted(sb, DELIMITER, domainHeader);
		appendEscapedAndQuoted(sb, DELIMITER, rangeHeader);
		writeLineToChannel(destChannel, sb.toString(), Charsets.UTF_8);

		for (Bin bin : bins) {
			sb = new StrBuilder();
			appendEscapedAndQuoted(sb, DELIMITER, numberFormat.format(bin.getAbsoluteBinIndex() * binSize / 1000));

			double[] values = bin.values.stream().mapToDouble(d -> d).toArray();
			switch (aggregationType) {
				case MEAN: {
					double mean = values.length == 0 ? 0d : StatUtils.mean(values);
					appendEscapedAndQuoted(sb, DELIMITER, numberFormat.format(mean));
					break;
				}
				case MEDIAN:
					double median = values.length == 0 ? 0d : StatUtils.percentile(values, 50d);
					appendEscapedAndQuoted(sb, DELIMITER, numberFormat.format(median));
					break;
				case COUNT:
					appendEscapedAndQuoted(sb, DELIMITER, numberFormat.format(bin.counter));
					break;
			}

			writeLineToChannel(destChannel, sb.toString(), Charsets.UTF_8);
		}
	}

	/**
	 * Represents a bin. Each bin has a counter and a list of values associated to the bin.
	 */
	public static class Bin {
		private final int absoluteBinIndex;
		private long counter;
		private final List<Double> values = new LinkedList<>();

		public Bin(final int absoluteBinIndex) {
			this.absoluteBinIndex = absoluteBinIndex;
		}

		public int getAbsoluteBinIndex() {
			return absoluteBinIndex;
		}

		public long getCounter() {
			return counter;
		}

		public List<Double> getValues() {
			return Collections.unmodifiableList(values);
		}
	}
}
