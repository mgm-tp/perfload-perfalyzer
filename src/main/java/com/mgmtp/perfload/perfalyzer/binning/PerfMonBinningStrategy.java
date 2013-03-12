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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.writeLineToChannel;
import static com.mgmtp.perfload.perfalyzer.util.StrBuilderUtils.appendEscapedAndQuoted;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;

import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.joda.time.Duration;

import com.google.common.primitives.Doubles;
import com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants;
import com.mgmtp.perfload.perfalyzer.util.ChannelManager;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.PerfMonTypeConfig;

/**
 * Binning implementation for perfMon logs.
 * 
 * @author rnaegele
 */
public class PerfMonBinningStrategy extends AbstractBinningStrategy {

	private final List<Double> valuesForAggregation = newArrayListWithExpectedSize(3600);
	private PerfMonTypeConfig typeConfig;
	private String type;

	public PerfMonBinningStrategy(final Charset charset, final NumberFormat intNumberFormat, final NumberFormat floatNumberFormat) {
		super(charset, intNumberFormat, floatNumberFormat);
	}

	@Override
	public void binData(final Scanner scanner, final WritableByteChannel destChannel) throws IOException {
		long lastBinStartMillis = 0;

		List<Double> binValues = newArrayList();
		int binIndex = 0;

		while (scanner.hasNextLine()) {
			tokenizer.reset(scanner.nextLine());
			List<String> tokenList = tokenizer.getTokenList();

			long timestampMillis = Long.parseLong(tokenList.get(0));

			if (typeConfig == null) {
				binIndex = (int) timestampMillis / PerfAlyzerConstants.BIN_SIZE_MILLIS_30_SECONDS;

				// align with bin size
				lastBinStartMillis = binIndex * PerfAlyzerConstants.BIN_SIZE_MILLIS_30_SECONDS;

				type = tokenList.get(1);
				typeConfig = determineTypeConfig(type);
				writeHeader(destChannel);
			}

			try {
				Double value = Double.valueOf(tokenList.get(2));
				binValues.add(value);

				// save for aggregation
				valuesForAggregation.add(value);

				Duration duration = new Duration(lastBinStartMillis, timestampMillis);
				if (duration.getMillis() > PerfAlyzerConstants.BIN_SIZE_MILLIS_30_SECONDS) {
					lastBinStartMillis = timestampMillis;
					writeBinnedLine(binValues, binIndex++, destChannel);
					binValues.clear();
				}
			} catch (NumberFormatException ex) {
				log.error("Could not parse value {}. Line in perfMon file might be incomplete. Ignoring it.", ex);
			}
		}

		if (!binValues.isEmpty()) {
			writeBinnedLine(binValues, binIndex, destChannel);
		}
	}

	@Override
	public void aggregateData(final ChannelManager channelManager) throws IOException {
		WritableByteChannel channel = channelManager.getChannel("aggregated");
		writeAggregatedHeader(channel);
		writeAggregatedLine(channel);
	}

	@Override
	public boolean needsBinning() {
		return true;
	}

	@Override
	public String transformDefautBinnedFilePath(final PerfAlyzerFile file) {
		return file.getFile().getPath();
	}

	private void writeHeader(final WritableByteChannel destChannel) throws IOException {
		StrBuilder sb = new StrBuilder(50);
		appendEscapedAndQuoted(sb, DELIMITER, "seconds");

		switch (typeConfig) {
			case CPU:
			case IO:
			case JAVA:
				appendEscapedAndQuoted(sb, DELIMITER, "mean");
				break;
			case MEM:
			case SWAP:
				appendEscapedAndQuoted(sb, DELIMITER, "median");
				break;
			default:
				throw new IllegalStateException("Invalid perfMon data type");
		}

		writeLineToChannel(destChannel, sb.toString(), charset);
	}

	private void writeAggregatedHeader(final WritableByteChannel destChannel) throws IOException {
		StrBuilder sb = new StrBuilder();

		switch (typeConfig) {
			case CPU:
			case IO:
			case JAVA:
				appendEscapedAndQuoted(sb, DELIMITER, "min");
				appendEscapedAndQuoted(sb, DELIMITER, "mean");
				appendEscapedAndQuoted(sb, DELIMITER, "max");
				break;
			case MEM:
			case SWAP:
				appendEscapedAndQuoted(sb, DELIMITER, "min");
				appendEscapedAndQuoted(sb, DELIMITER, "q0.1");
				appendEscapedAndQuoted(sb, DELIMITER, "q0.5");
				appendEscapedAndQuoted(sb, DELIMITER, "q0.9");
				appendEscapedAndQuoted(sb, DELIMITER, "max");
				break;
			default:
				throw new IllegalStateException("Invalid perfMon data type");
		}

		writeLineToChannel(destChannel, sb.toString(), charset);
	}

	private void writeBinnedLine(final List<Double> binValues, final int binIndex, final WritableByteChannel destChannel)
			throws IOException {

		StrBuilder sb = new StrBuilder();
		appendEscapedAndQuoted(sb, DELIMITER, binIndex * PerfAlyzerConstants.BIN_SIZE_MILLIS_30_SECONDS / 1000);

		double[] binValuesArray = Doubles.toArray(binValues);

		switch (typeConfig) {
			case CPU:
			case IO:
			case JAVA:
				appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(StatUtils.mean(binValuesArray)));
				break;
			case MEM:
			case SWAP:
				appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(StatUtils.percentile(binValuesArray, 50d)));
				break;
			default:
				throw new IllegalStateException("Invalid perfMon data type");
		}

		writeLineToChannel(destChannel, sb.toString(), charset);
	}

	private void writeAggregatedLine(final WritableByteChannel destChannel) throws IOException {
		if (!valuesForAggregation.isEmpty()) {
			StrBuilder sb = new StrBuilder();

			double[] valuesArray = Doubles.toArray(valuesForAggregation);
			String min = intNumberFormat.format(Doubles.min(valuesArray));
			String max = intNumberFormat.format(Doubles.max(valuesArray));

			switch (typeConfig) {
				case CPU:
				case IO:
				case JAVA:
					String mean = intNumberFormat.format(StatUtils.mean(valuesArray));
					appendEscapedAndQuoted(sb, DELIMITER, min, mean, max);
					break;
				case MEM:
				case SWAP:
					Percentile percentile = new Percentile();
					percentile.setData(valuesArray);
					String q10 = intNumberFormat.format(percentile.evaluate(10d));
					String q50 = intNumberFormat.format(percentile.evaluate(50d));
					String q90 = intNumberFormat.format(percentile.evaluate(90d));
					appendEscapedAndQuoted(sb, DELIMITER, min, q10, q50, q90, max);
					break;
				default:
					throw new IllegalStateException("Invalid perfMon data type");
			}

			writeLineToChannel(destChannel, sb.toString(), charset);
		}
	}

	private PerfMonTypeConfig determineTypeConfig(final String perfmonType) {
		for (PerfMonTypeConfig tc : PerfMonTypeConfig.values()) {
			Matcher matcher = tc.getPattern().matcher(perfmonType);
			if (matcher.matches()) {
				return tc;
			}
		}
		throw new IllegalStateException("No binning content found for type: " + perfmonType);
	}
}
