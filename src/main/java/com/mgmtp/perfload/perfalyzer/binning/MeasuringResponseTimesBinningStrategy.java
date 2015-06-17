/*
 * Copyright (c) 2013-2015 mgm technology partners GmbH
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
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;
import com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants;
import com.mgmtp.perfload.perfalyzer.util.AggregationType;
import com.mgmtp.perfload.perfalyzer.util.ChannelManager;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
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

/**
 * Binning implementation for measuring logs.
 *
 * @author ctchinda
 * @author rnaegele
 */
public class MeasuringResponseTimesBinningStrategy extends AbstractBinningStrategy {

	private final Map<String, UriMeasurings> measuringsMap = newTreeMap();
	private final Map<String, ExecutionMeasurings> perExecutionResponseTimes = newHashMap();
	private final Set<String> errorExecutions = newHashSet();

	public MeasuringResponseTimesBinningStrategy(final long startOfFirstBin, final NumberFormat intNumberFormat,
			final NumberFormat floatNumberFormat) {
		super(startOfFirstBin, intNumberFormat, floatNumberFormat);
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
			writeLineToChannel(quantilesChannel, sb.toString(), Charsets.UTF_8);

			// write response time distributions
			WritableByteChannel distributionChannel = channelManager.getChannel("distribution_" + mappingKey);
			writeDistributionHeader(distributionChannel);

			for (Entry<Long, MutableInt> e : measurings.responseDistributions.entrySet()) {
				sb = new StrBuilder();
				appendEscapedAndQuoted(sb, DELIMITER, e.getKey());
				appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(e.getValue()));
				writeLineToChannel(distributionChannel, sb.toString(), Charsets.UTF_8);
			}
		}

		writeExecutionAggregatedResponseTimesHeader(channelManager.getChannel("aggregatedResponseTimes"));
		if (!perExecutionResponseTimes.isEmpty()) {
			BinManager executionsPerMinuteBinManager = new BinManager(startOfFirstBin, PerfAlyzerConstants.BIN_SIZE_MILLIS_1_MINUTE);
			BinManager executionsPerTenMinutesBinManager = new BinManager(startOfFirstBin, PerfAlyzerConstants.BIN_SIZE_MILLIS_10_MINUTES);
			BinManager medianExecutionBinManager = new BinManager(startOfFirstBin, PerfAlyzerConstants.BIN_SIZE_MILLIS_30_SECONDS);

			List<ExecutionMeasurings> values = newArrayList(perExecutionResponseTimes.values());

			for (ExecutionMeasurings execMeasurings : values) {
				long timestampMillis = execMeasurings.timestampMillis;
				executionsPerMinuteBinManager.addValue(timestampMillis);
				executionsPerTenMinutesBinManager.addValue(timestampMillis);
				medianExecutionBinManager.addValue(timestampMillis, execMeasurings.sumResponseTimes.doubleValue() / 1000);
			}

			executionsPerMinuteBinManager.toCsv(channelManager.getChannel("execMin"), "time", "count", intNumberFormat);
			executionsPerTenMinutesBinManager.toCsv(channelManager.getChannel("exec10Min"), "time", "count", intNumberFormat);
			medianExecutionBinManager.toCsv(channelManager.getChannel("executions"), "time", "median", intNumberFormat, AggregationType.MEDIAN);

			double[] sumResponseTimes = values.stream().mapToDouble(input -> input.sumResponseTimes.doubleValue()).toArray();

			StrBuilder sb = new StrBuilder(150);
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(Doubles.min(sumResponseTimes) / 1000));
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(StatUtils.percentile(sumResponseTimes, 50d) / 1000));
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(Doubles.max(sumResponseTimes) / 1000));
			writeLineToChannel(channelManager.getChannel("aggregatedResponseTimes"), sb.toString(), Charsets.UTF_8);
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
		writeLineToChannel(destChannel, sb.toString(), Charsets.UTF_8);
	}

	private void writeDistributionHeader(final WritableByteChannel destChannel) throws IOException {
		StrBuilder sb = new StrBuilder();
		appendEscapedAndQuoted(sb, DELIMITER, "time");
		appendEscapedAndQuoted(sb, DELIMITER, "count");
		writeLineToChannel(destChannel, sb.toString(), Charsets.UTF_8);
	}

	private void writeExecutionAggregatedResponseTimesHeader(final WritableByteChannel destChannel) throws IOException {
		StrBuilder sb = new StrBuilder();
		appendEscapedAndQuoted(sb, DELIMITER, "minExecutionTime");
		appendEscapedAndQuoted(sb, DELIMITER, "medianExecutionTime");
		appendEscapedAndQuoted(sb, DELIMITER, "maxExecutionTime");
		writeLineToChannel(destChannel, sb.toString(), Charsets.UTF_8);
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
}
