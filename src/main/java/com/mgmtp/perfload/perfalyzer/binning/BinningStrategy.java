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

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Scanner;

import javax.annotation.Nullable;

import com.mgmtp.perfload.perfalyzer.util.ChannelManager;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;

/**
 * Interface for the actual binning and aggregation logic.
 * 
 * @author rnaegele
 */
public interface BinningStrategy {

	/**
	 * Bins the data and writes it to the specified channel. The method may already pre-aggregate
	 * the data received from the scanner.
	 * 
	 * @param scanner
	 *            provides access to the unbinned data
	 * @param destChannel
	 *            the channel to write the binned data to, can be null if no binning should happen
	 */
	void binData(Scanner scanner, @Nullable WritableByteChannel destChannel) throws IOException;

	/**
	 * Aggregates data and writes it to a channel retrieved from the specified channel manager. This
	 * must relies on {@link #binData(Scanner, WritableByteChannel)} being called before.
	 * 
	 * @param channelManager
	 *            the channel manager
	 */
	void aggregateData(final ChannelManager channelManager) throws IOException;

	/**
	 * Specifies whether binning is necessary. The caller can then decide whether or not to provide
	 * channel to {@link #binData(Scanner, WritableByteChannel)}.
	 * 
	 * @return whether binning is necessary
	 */
	boolean needsBinning();

	String transformDefautBinnedFilePath(PerfAlyzerFile file);
}
