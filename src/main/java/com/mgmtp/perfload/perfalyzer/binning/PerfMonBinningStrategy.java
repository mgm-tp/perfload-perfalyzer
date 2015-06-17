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
import com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants;
import com.mgmtp.perfload.perfalyzer.util.ChannelManager;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.PerfMonTypeConfig;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.text.NumberFormat;
import java.util.List;
import java.util.Scanner;

import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.writeLineToChannel;
import static com.mgmtp.perfload.perfalyzer.util.StrBuilderUtils.appendEscapedAndQuoted;

/**
 * Binning implementation for perfMon logs.
 *
 * @author rnaegele
 */
public class PerfMonBinningStrategy extends AbstractBinningStrategy {

	private final BinManager binManager;
	private PerfMonTypeConfig typeConfig;

	public PerfMonBinningStrategy(final long startOfFirstBin, final NumberFormat intNumberFormat, final NumberFormat floatNumberFormat) {
		super(startOfFirstBin, intNumberFormat, floatNumberFormat);
		binManager = new BinManager(startOfFirstBin, PerfAlyzerConstants.BIN_SIZE_MILLIS_30_SECONDS);
	}

	@Override
	public void binData(final Scanner scanner, final WritableByteChannel destChannel) throws IOException {
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			tokenizer.reset(line);
			List<String> tokenList = tokenizer.getTokenList();

			if (typeConfig == null) {
				String type = tokenList.get(1);
				typeConfig = PerfMonTypeConfig.fromString(type);
			}

			try {
				long timestampMillis = Long.parseLong(tokenList.get(0));
				Double value = Double.valueOf(tokenList.get(2));
				binManager.addValue(timestampMillis, value);
			} catch (NumberFormatException ex) {
				log.error("Could not parse value {}. Line in perfMon file might be incomplete. Ignoring it.", ex);
			}
		}

		binManager.toCsv(destChannel, "seconds", typeConfig.getHeader(), intNumberFormat, typeConfig.getAggregationType());
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

	private void writeAggregatedHeader(final WritableByteChannel destChannel) throws IOException {
		StrBuilder sb = new StrBuilder();
		typeConfig.getAggregatedHeaders().forEach(header -> appendEscapedAndQuoted(sb, DELIMITER, header));
		writeLineToChannel(destChannel, sb.toString(), Charsets.UTF_8);
	}

	private void writeAggregatedLine(final WritableByteChannel destChannel) throws IOException {
		double[] allValues = binManager.flatValuesStream().toArray();

		StrBuilder sb = new StrBuilder();

		String min = intNumberFormat.format(Doubles.min(allValues));
		String max = intNumberFormat.format(Doubles.max(allValues));

		switch (typeConfig) {
			case CPU:
			case IO:
			case JAVA:
				String mean = intNumberFormat.format(StatUtils.mean(allValues));
				appendEscapedAndQuoted(sb, DELIMITER, min, mean, max);
				break;
			case MEM:
			case SWAP:
				Percentile percentile = new Percentile();
				percentile.setData(allValues);
				String q10 = intNumberFormat.format(percentile.evaluate(10d));
				String q50 = intNumberFormat.format(percentile.evaluate(50d));
				String q90 = intNumberFormat.format(percentile.evaluate(90d));
				appendEscapedAndQuoted(sb, DELIMITER, min, q10, q50, q90, max);
				break;
			default:
				throw new IllegalStateException("Invalid perfMon data type");
		}

		writeLineToChannel(destChannel, sb.toString(), Charsets.UTF_8);
	}
}
