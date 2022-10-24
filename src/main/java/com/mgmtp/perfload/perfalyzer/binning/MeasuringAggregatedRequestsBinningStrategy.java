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
import com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants;
import com.mgmtp.perfload.perfalyzer.util.ChannelManager;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.math3.stat.StatUtils;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.text.NumberFormat;
import java.util.Scanner;

import javax.annotation.Nullable;

import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_NORMALIZED_COL_REQUEST_TYPE;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_NORMALIZED_COL_RESULT;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.writeLineToChannel;
import static com.mgmtp.perfload.perfalyzer.util.StrBuilderUtils.appendEscapedAndQuoted;

/**
 * Binning implementation for measuring logs.
 *
 * @author ctchinda $
 */
public class MeasuringAggregatedRequestsBinningStrategy extends AbstractBinningStrategy {

	public MeasuringAggregatedRequestsBinningStrategy(final long startOfFirstBin, final NumberFormat intNumberFormat, final NumberFormat floatNumberFormat) {
		super(startOfFirstBin, intNumberFormat, floatNumberFormat);
	}

	@Override
	public void binData(final Scanner scanner, @Nullable final WritableByteChannel destChannel) throws IOException {
		BinManager binSecondManager = new BinManager(startOfFirstBin, PerfAlyzerConstants.BIN_SIZE_MILLIS_1_SECOND);
		BinManager binMinuteManager = new BinManager(startOfFirstBin, PerfAlyzerConstants.BIN_SIZE_MILLIS_1_MINUTE);

		int requestCounter = 0;
		int errorCounter = 0;

		while (scanner.hasNextLine()) {
			tokenizer.reset(scanner.nextLine());
			String[] tokens = tokenizer.getTokenArray();

			long timestampMillis = Long.parseLong(tokens[0]);

			if (!"AGENT".equals(tokens[MEASURING_NORMALIZED_COL_REQUEST_TYPE])) {
				requestCounter++;
				binSecondManager.addValue(timestampMillis);
				binMinuteManager.addValue(timestampMillis);
			}
			if ("ERROR".equals(tokens[MEASURING_NORMALIZED_COL_RESULT])) {
				errorCounter++;
			}
		}

//		binSecondManager.completeLastBin();
//		binMinuteManager.completeLastBin();

		double[] requestsPerSecond = binSecondManager.countStream().asDoubleStream().toArray();//   Doubles.toArray(binSecondManager.getBins().values().stream().map(longToDouble()).collect(toList()));
		double minRequestsPerSecond = StatUtils.min(requestsPerSecond);
		double medianRequestsPerSecond = StatUtils.percentile(requestsPerSecond, 50d);
		double maxRequestsPerSecond = StatUtils.max(requestsPerSecond);

		double[] requestsPerMinute = binMinuteManager.countStream().asDoubleStream().toArray(); // Doubles.toArray(binMinuteManager.getBins().values().stream().map(longToDouble()).collect(toList()));
		double minRequestsPerMinute = StatUtils.min(requestsPerMinute);
		double medianRequestsPerMinute = StatUtils.percentile(requestsPerMinute, 50d);
		double maxRequestsPerMinute = StatUtils.max(requestsPerMinute);

		StrBuilder sb = new StrBuilder();
		appendEscapedAndQuoted(sb, DELIMITER, "numRequests");
		appendEscapedAndQuoted(sb, DELIMITER, "numErrors");

		appendEscapedAndQuoted(sb, DELIMITER, "minReqPerSec");
		appendEscapedAndQuoted(sb, DELIMITER, "medianReqPerSec");
		appendEscapedAndQuoted(sb, DELIMITER, "maxReqPerSec");

		appendEscapedAndQuoted(sb, DELIMITER, "minReqPerMin");
		appendEscapedAndQuoted(sb, DELIMITER, "medianReqPerMin");
		appendEscapedAndQuoted(sb, DELIMITER, "maxReqPerMin");
		writeLineToChannel(destChannel, sb.toString(), Charsets.UTF_8);

		sb = new StrBuilder();
		appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(requestCounter));
		appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(errorCounter));

		appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(minRequestsPerSecond));
		appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(medianRequestsPerSecond));
		appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(maxRequestsPerSecond));

		appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(minRequestsPerMinute));
		appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(medianRequestsPerMinute));
		appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(maxRequestsPerMinute));
		writeLineToChannel(destChannel, sb.toString(), Charsets.UTF_8);
	}

	@Override
	public boolean needsBinning() {
		return true;
	}

	@Override
	public void aggregateData(final ChannelManager channelManager) throws IOException {
		// no-op
	}

	@Override
	public String transformDefautBinnedFilePath(final PerfAlyzerFile file) {
		return file.copy().addFileNamePart("requestsPerInterval").getFile().getPath();
	}
}
