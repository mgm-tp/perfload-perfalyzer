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

import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_NORMALIZED_COL_REQUEST_TYPE;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.text.NumberFormat;
import java.util.Scanner;

import com.mgmtp.perfload.perfalyzer.util.ChannelManager;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;

/**
 * Binning implementation for measuring logs.
 * 
 * @author ctchinda
 */
public class MeasuringRequestsBinningStrategy extends AbstractBinningStrategy {

	private final int binSize;

	public MeasuringRequestsBinningStrategy(final int binSize, final NumberFormat intNumberFormat,
			final NumberFormat floatNumberFormat) {
		super(intNumberFormat, floatNumberFormat);
		this.binSize = binSize;
	}

	@Override
	public void binData(final Scanner scanner, final WritableByteChannel destChannel) throws IOException {
		BinManager binManager = new ChannelBinManager(binSize, destChannel, "seconds", "count", intNumberFormat);

		while (scanner.hasNextLine()) {
			tokenizer.reset(scanner.nextLine());
			String[] tokens = tokenizer.getTokenArray();
			if ("AGENT".equals(tokens[MEASURING_NORMALIZED_COL_REQUEST_TYPE])) {
				continue;
			}
			long timestampMillis = Long.parseLong(tokens[0]);
			binManager.addTimestamp(timestampMillis);
		}

		binManager.completeLastBin();
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
		return file.copy().addFileNamePart("requests").addFileNamePart(String.valueOf(binSize)).getFile().getPath();
	}
}
