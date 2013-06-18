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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newTreeMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_NORMALIZED_COL_EXECUTION_ID;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_NORMALIZED_COL_REQUEST_TYPE;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_NORMALIZED_COL_RESULT;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_NORMALIZED_COL_URI_ALIAS;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.writeLineToChannel;
import static com.mgmtp.perfload.perfalyzer.util.StrBuilderUtils.appendEscapedAndQuoted;
import static org.apache.commons.lang3.StringUtils.leftPad;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;
import com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants;
import com.mgmtp.perfload.perfalyzer.util.ChannelManager;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;

/**
 * Binning implementation for measuring logs.
 * 
 * @author ctchinda
 */
public class MeasuringResponseTimesBinningStrategy extends AbstractBinningStrategy {

	private final Map<String, UriMeasurings> measuringsMap = newTreeMap();
	private final Map<String, ExecutionMeasurings> perExecutionResponseTimes = newHashMap();
	private final Set<String> errorExecutions = newHashSet();

	public MeasuringResponseTimesBinningStrategy(final Charset charset, final NumberFormat intNumberFormat,
			final NumberFormat floatNumberFormat) {
		super(charset, intNumberFormat, floatNumberFormat);
	}

	@Override
	public void binData(final Scanner scanner, final WritableByteChannel destChannel) throws IOException {

		while (scanner.hasNextLine()) {
			tokenizer.reset(scanner.nextLine());
			String[] tokens = tokenizer.getTokenArray();

			long timestampMillis = Long.parseLong(tokens[0]);
			Long responseTime = Long.valueOf(tokens[2]);
			String type = tokens[MEASURING_NORMALIZED_COL_REQUEST_TYPE];
			String uriAlias = tokens[MEASURING_NORMALIZED_COL_URI_ALIAS];
			String result = tokens[MEASURING_NORMALIZED_COL_RESULT];
			String executionId = tokens[MEASURING_NORMALIZED_COL_EXECUTION_ID];

			String key = type + "||" + uriAlias;
			UriMeasurings measurings = measuringsMap.get(key);
			if (measurings == null) {
				measurings = new UriMeasurings();
				measurings.type = type;
				measurings.uriAlias = uriAlias;
				measuringsMap.put(key, measurings);
			}

			if (responseTime > 0) {
				// response time distribution is calculated by grouping by response time
				// only positive values allowed on logarithmic axis
				// response time might by -1 in case of an error
				MutableInt mutableInt = measurings.responseDistributions.get(responseTime);
				if (mutableInt == null) {
					mutableInt = new MutableInt();
					measurings.responseDistributions.put(responseTime, mutableInt);
				}
				mutableInt.increment();
			}

			// collect all response times for a URI, so quantiles can be calculated later
			measurings.responseTimes.add(responseTime.doubleValue());

			if ("ERROR".equals(result)) {
				measurings.errorCount.increment();

				errorExecutions.add(executionId);
			}

			if (!isNullOrEmpty(executionId)) {
				ExecutionMeasurings execMeasurings = perExecutionResponseTimes.get(executionId);
				if (execMeasurings == null) {
					execMeasurings = new ExecutionMeasurings();
					execMeasurings.sumResponseTimes = new MutableLong(responseTime);
					perExecutionResponseTimes.put(executionId, execMeasurings);
				} else {
					perExecutionResponseTimes.get(executionId).sumResponseTimes.add(responseTime);
				}
				// always update timestamp so we eventually have the last timestamp of the execution
				execMeasurings.timestampMillis = timestampMillis;
			}
		}
	}

	@Override
	public String transformDefautBinnedFilePath(final PerfAlyzerFile file) {
		return file.getFile().getPath();
	}

	@Override
	public void aggregateData(final ChannelManager channelManager) throws IOException {
		WritableByteChannel quantilesChannel = channelManager.getChannel("quantiles");
		writeQuantilesHeader(quantilesChannel);

		int i = 0;
		for (Entry<String, UriMeasurings> entry : measuringsMap.entrySet()) {
			UriMeasurings measurings = entry.getValue();
			String uri = measurings.uriAlias;
			if (measurings.responseTimes.isEmpty()) {
				continue;
			}

			Percentile percentile = new Percentile();
			double[] responseTimes = Doubles.toArray(measurings.responseTimes);
			percentile.setData(responseTimes);

			// each uri is mapped to a key which is simple a number that is left-padded for better sorting
			String mappingKey = leftPad(String.valueOf(i++), 3, '0');

			StrBuilder sb = new StrBuilder(150);
			appendEscapedAndQuoted(sb, DELIMITER, mappingKey);
			appendEscapedAndQuoted(sb, DELIMITER, measurings.type);
			appendEscapedAndQuoted(sb, DELIMITER, uri);
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(responseTimes.length));
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(measurings.errorCount));
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(Doubles.min(responseTimes)));
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(percentile.evaluate(10d)));
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(percentile.evaluate(50d)));
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(percentile.evaluate(90d)));
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(Doubles.max(responseTimes)));
			writeLineToChannel(quantilesChannel, sb.toString(), charset);

			// write response time distributions
			WritableByteChannel distributionChannel = channelManager.getChannel("distribution_" + mappingKey);
			writeDistributionHeader(distributionChannel);

			for (Entry<Long, MutableInt> e : measurings.responseDistributions.entrySet()) {
				sb = new StrBuilder();
				appendEscapedAndQuoted(sb, DELIMITER, e.getKey());
				appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(e.getValue()));
				writeLineToChannel(distributionChannel, sb.toString(), charset);
			}
		}

		writeExecutionAggregatedResponseTimesHeader(channelManager.getChannel("aggregatedResponseTimes"));
		if (!perExecutionResponseTimes.isEmpty()) {
			BinManager executionsPerMinuteBinManager = new ChannelBinManager(PerfAlyzerConstants.BIN_SIZE_MILLIS_1_MINUTE,
					channelManager.getChannel("execMin"), "time", "count", charset, intNumberFormat);
			BinManager executionsPerTenMinutesBinManager = new ChannelBinManager(PerfAlyzerConstants.BIN_SIZE_MILLIS_10_MINUTES,
					channelManager.getChannel("exec10Min"), "time", "count", charset, intNumberFormat);
			MedianBinManager medianExecutionBinManager = new MedianBinManager(PerfAlyzerConstants.BIN_SIZE_MILLIS_30_SECONDS,
					channelManager.getChannel("executions"), "time", "median", charset, intNumberFormat);

			List<ExecutionMeasurings> values = newArrayList(perExecutionResponseTimes.values());
			Collections.sort(values);

			for (ExecutionMeasurings execMeasurings : values) {
				medianExecutionBinManager.addBinValue(execMeasurings.sumResponseTimes.doubleValue());

				long timestampMillis = execMeasurings.timestampMillis;
				executionsPerMinuteBinManager.addTimestamp(timestampMillis);
				executionsPerTenMinutesBinManager.addTimestamp(timestampMillis);
				medianExecutionBinManager.addTimestamp(timestampMillis);
			}

			executionsPerMinuteBinManager.completeLastBin();
			executionsPerTenMinutesBinManager.completeLastBin();
			medianExecutionBinManager.completeLastBin();

			double[] sumResponseTimes = Doubles.toArray(Collections2.transform(values,
					new Function<ExecutionMeasurings, Double>() {
						@Override
						public Double apply(final ExecutionMeasurings input) {
							return input.sumResponseTimes.doubleValue();
						}
					}));

			StrBuilder sb = new StrBuilder(150);
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(Doubles.min(sumResponseTimes)));
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(StatUtils.percentile(sumResponseTimes, 50d)));
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(Doubles.max(sumResponseTimes)));
			writeLineToChannel(channelManager.getChannel("aggregatedResponseTimes"), sb.toString(), charset);
		}
	}

	private void writeQuantilesHeader(final WritableByteChannel destChannel) throws IOException {
		StrBuilder sb = new StrBuilder();
		appendEscapedAndQuoted(sb, DELIMITER, "key");
		appendEscapedAndQuoted(sb, DELIMITER, "type");
		appendEscapedAndQuoted(sb, DELIMITER, "uri");
		appendEscapedAndQuoted(sb, DELIMITER, "requests");
		appendEscapedAndQuoted(sb, DELIMITER, "errors");
		appendEscapedAndQuoted(sb, DELIMITER, "min");
		appendEscapedAndQuoted(sb, DELIMITER, "q0.1");
		appendEscapedAndQuoted(sb, DELIMITER, "q0.5");
		appendEscapedAndQuoted(sb, DELIMITER, "q0.9");
		appendEscapedAndQuoted(sb, DELIMITER, "max");
		writeLineToChannel(destChannel, sb.toString(), charset);
	}

	private void writeDistributionHeader(final WritableByteChannel destChannel) throws IOException {
		StrBuilder sb = new StrBuilder();
		appendEscapedAndQuoted(sb, DELIMITER, "time");
		appendEscapedAndQuoted(sb, DELIMITER, "count");
		writeLineToChannel(destChannel, sb.toString(), charset);
	}

	private void writeExecutionAggregatedResponseTimesHeader(final WritableByteChannel destChannel) throws IOException {
		StrBuilder sb = new StrBuilder();
		appendEscapedAndQuoted(sb, DELIMITER, "minExecutionTime");
		appendEscapedAndQuoted(sb, DELIMITER, "medianExecutionTime");
		appendEscapedAndQuoted(sb, DELIMITER, "maxExecutionTime");
		writeLineToChannel(destChannel, sb.toString(), charset);
	}

	@Override
	public boolean needsBinning() {
		return false;
	}

	/**
	 * Container for measurings for a specified URI
	 */
	static class UriMeasurings {
		String type;
		public String uriAlias;
		Map<Long, MutableInt> responseDistributions = newTreeMap(); // tree map for sorting
		List<Double> responseTimes = newArrayListWithCapacity(5000); // Double needed for quantile computation
		MutableInt errorCount = new MutableInt();
	}

	static class ExecutionMeasurings implements Comparable<ExecutionMeasurings> {
		long timestampMillis;
		MutableLong sumResponseTimes;

		@Override
		public int compareTo(final ExecutionMeasurings other) {
			return Longs.compare(timestampMillis, other.timestampMillis);
		}
	}

	static class MedianBinManager extends ChannelBinManager {

		private List<Double> binValues;
		private boolean completingLastBin;

		public MedianBinManager(final int binSize, final WritableByteChannel destChannel, final String header1,
				final String header2,
				final Charset charset, final NumberFormat numberFormat) {
			super(binSize, destChannel, header1, header2, charset, numberFormat);
		}

		public void addBinValue(final double seconds) {
			if (binValues == null) {
				binValues = newArrayListWithExpectedSize(42);
			}
			binValues.add(seconds);
		}

		@Override
		protected void binCompleted(final int bin, final long counter) {
			if (starting || completingLastBin) {
				formatAndWriteToChannel(bin, counter);
			} else if (!binValues.isEmpty()) {
				double median = StatUtils.percentile(Doubles.toArray(binValues), 50d);
				formatAndWriteToChannel(bin, median);
				binValues.clear();
			}
		}

		@Override
		public void completeLastBin() {
			completingLastBin = true;
			super.completeLastBin();
		}
	}
}
