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

import com.mgmtp.perfload.perfalyzer.util.ChannelManager;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.text.NumberFormat;
import java.util.Scanner;

import javax.annotation.Nullable;

import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_NORMALIZED_COL_REQUEST_TYPE;

/**
 * Binning implementation for measuring logs.
 *
 * @author ctchinda
 */
public class MeasuringRequestsBinningStrategy extends AbstractBinningStrategy {

	private final int binSize;

	public MeasuringRequestsBinningStrategy(final long startOfFirstBin, final int binSize, final NumberFormat intNumberFormat,
			final NumberFormat floatNumberFormat) {
		super(startOfFirstBin, intNumberFormat, floatNumberFormat);
		this.binSize = binSize;
	}

	@Override
	public void binData(final Scanner scanner, @Nullable final WritableByteChannel destChannel) throws IOException {
		BinManager binManager = new BinManager(startOfFirstBin, binSize); // new ChannelBinManager(binSize, destChannel, "seconds", "count", intNumberFormat);

		while (scanner.hasNextLine()) {
			tokenizer.reset(scanner.nextLine());
			String[] tokens = tokenizer.getTokenArray();
			if ("AGENT".equals(tokens[MEASURING_NORMALIZED_COL_REQUEST_TYPE])) {
				continue;
			}
			long timestampMillis = Long.parseLong(tokens[0]);
			binManager.addValue(timestampMillis);
		}

		binManager.toCsv(destChannel, "seconds", "count", intNumberFormat);
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
