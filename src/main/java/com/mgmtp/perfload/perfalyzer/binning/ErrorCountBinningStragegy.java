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
import com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants;
import com.mgmtp.perfload.perfalyzer.util.ChannelManager;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.text.StrBuilder;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import static com.google.common.collect.Maps.newHashMap;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_NORMALIZED_COL_ERROR_MSG;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_NORMALIZED_COL_RESULT;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.writeLineToChannel;
import static com.mgmtp.perfload.perfalyzer.util.StrBuilderUtils.appendEscapedAndQuoted;

/**
 * @author rnaegele
 */
public class ErrorCountBinningStragegy extends AbstractBinningStrategy {

	private final Map<String, MutableInt> errorsByType = newHashMap();

	public ErrorCountBinningStragegy(final long startOfFirstBin, final NumberFormat intNumberFormat, final NumberFormat floatNumberFormat) {
		super(startOfFirstBin, intNumberFormat, floatNumberFormat);
	}

	@Override
	public void binData(final Scanner scanner, final WritableByteChannel destChannel) throws IOException {
		BinManager binManager = new BinManager(startOfFirstBin, PerfAlyzerConstants.BIN_SIZE_MILLIS_30_SECONDS);

		while (scanner.hasNextLine()) {
			tokenizer.reset(scanner.nextLine());
			String[] tokens = tokenizer.getTokenArray();

			long timestampMillis = Long.parseLong(tokens[0]);

			boolean isError = "ERROR".equals(tokens[MEASURING_NORMALIZED_COL_RESULT]);
			if (isError) {
				String errorMsg = tokens[MEASURING_NORMALIZED_COL_ERROR_MSG];
				MutableInt errorsByTypeCounter = errorsByType.get(errorMsg);
				if (errorsByTypeCounter == null) {
					errorsByTypeCounter = new MutableInt();
					errorsByType.put(errorMsg, errorsByTypeCounter);
				}
				errorsByTypeCounter.increment();

				binManager.addValue(timestampMillis);
			}
		}

		binManager.toCsv(destChannel, "seconds", "count", intNumberFormat);
	}

	@Override
	public boolean needsBinning() {
		return true;
	}

	@Override
	public void aggregateData(final ChannelManager channelManager) throws IOException {
		WritableByteChannel channel = channelManager.getChannel("errorsByType");

		StrBuilder sb = new StrBuilder();
		appendEscapedAndQuoted(sb, DELIMITER, "error", "count");
		writeLineToChannel(channel, sb.toString(), Charsets.UTF_8);

		for (Entry<String, MutableInt> entry : errorsByType.entrySet()) {
			sb = new StrBuilder(300);
			appendEscapedAndQuoted(sb, DELIMITER, entry.getKey());
			appendEscapedAndQuoted(sb, DELIMITER, intNumberFormat.format(entry.getValue().getValue()));
			writeLineToChannel(channel, sb.toString(), Charsets.UTF_8);
		}
	}

	@Override
	public String transformDefautBinnedFilePath(final PerfAlyzerFile file) {
		return file.copy().addFileNamePart("errorCount").getFile().getPath();
	}
}
