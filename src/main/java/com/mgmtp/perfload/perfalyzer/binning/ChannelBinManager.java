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

import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.writeLineToChannel;
import static com.mgmtp.perfload.perfalyzer.util.StrBuilderUtils.appendEscapedAndQuoted;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.text.NumberFormat;

import org.apache.commons.lang3.text.StrBuilder;

import com.mgmtp.perfload.perfalyzer.PerfAlyzerException;

/**
 * @author rnaegele
 */
public class ChannelBinManager extends BinManager {

	protected WritableByteChannel destChannel;
	protected String header1;
	protected String header2;

	protected Charset charset;
	protected NumberFormat numberFormat;

	/**
	 * @param binSize
	 *            the bin size
	 * @param destChannel
	 *            the channel to write the binned data to
	 * @param header1
	 *            the name of the first CSV header
	 * @param header2
	 *            the name of the second CSV header
	 * @param charset
	 *            the charset to use
	 * @param numberFormat
	 *            the number format to use
	 */
	public ChannelBinManager(final int binSize, final WritableByteChannel destChannel, final String header1,
			final String header2, final Charset charset, final NumberFormat numberFormat) {
		super(binSize);
		this.destChannel = destChannel;
		this.header1 = header1;
		this.header2 = header2;
		this.charset = charset;
		this.numberFormat = numberFormat;
	}

	@Override
	protected void binningStarted() {
		try {
			StrBuilder sb = new StrBuilder(50);
			appendEscapedAndQuoted(sb, DELIMITER, header1);
			appendEscapedAndQuoted(sb, DELIMITER, header2);
			writeLineToChannel(destChannel, sb.toString(), charset);
		} catch (IOException ex) {
			throw new PerfAlyzerException(ex.getMessage(), ex);
		}
	}

	@Override
	protected void binCompleted(final int bin, final long counter) {
		try {
			StrBuilder sb = new StrBuilder();
			appendEscapedAndQuoted(sb, DELIMITER, numberFormat.format(bin));
			appendEscapedAndQuoted(sb, DELIMITER, numberFormat.format(counter));
			writeLineToChannel(destChannel, sb.toString(), charset);
		} catch (IOException ex) {
			throw new PerfAlyzerException(ex.getMessage(), ex);
		}
	}

}
