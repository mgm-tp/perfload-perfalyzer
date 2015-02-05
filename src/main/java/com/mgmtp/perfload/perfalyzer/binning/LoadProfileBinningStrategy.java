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

import com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants;
import com.mgmtp.perfload.perfalyzer.util.ChannelManager;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.text.NumberFormat;
import java.util.Scanner;

/**
 * Binning implementation for perfMon logs.
 *
 * @author rnaegele
 */
public class LoadProfileBinningStrategy extends AbstractBinningStrategy {

	public LoadProfileBinningStrategy(final NumberFormat intNumberFormat, final NumberFormat floatNumberFormat) {
		super(0L, intNumberFormat, floatNumberFormat);
	}

	@Override
	public void binData(final Scanner scanner, final WritableByteChannel destChannel) throws IOException {
		BinManager binManager = new BinManager(0L, PerfAlyzerConstants.BIN_SIZE_MILLIS_1_MINUTE);

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			tokenizer.reset(line);
			long timestampMillis = Long.parseLong(tokenizer.nextToken());
			binManager.addValue(timestampMillis);
		}

		binManager.toCsv(destChannel, "seconds", "count", intNumberFormat);
	}

	@Override
	public void aggregateData(final ChannelManager channelManager) throws IOException {
		// no op
	}

	@Override
	public boolean needsBinning() {
		return true;
	}

	@Override
	public String transformDefautBinnedFilePath(final PerfAlyzerFile file) {
		return file.getFile().getPath();
	}
}
