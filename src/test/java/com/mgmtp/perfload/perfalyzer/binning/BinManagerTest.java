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

import com.mgmtp.perfload.perfalyzer.util.AggregationType;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author rnaegele
 */
public class BinManagerTest {

	private static final long[] TEST_DATA = {
			0, 500, // bin 0: 2
			1000, 1234, 1777, // bin 1: 3
			2000, 2345, 2999, // bin 2: 3
			4000, 4500, // bin 4: 2
			10000, 10999, // bin 10: 2
			12345 // bin 12: 1
	};

	private long[] expectedCounts = {2, 3, 3, 0, 2, 0, 0, 0, 0, 0, 2, 0, 1};

	@Test
	public void testWithCountOnly() {
		BinManager binManager = new BinManager(0, 1000);
		Arrays.stream(TEST_DATA).forEach(binManager::addValue);

		assertThat(binManager.countStream().count(), equalTo(13L));
		assertThat(binManager.countStream().toArray(), equalTo(expectedCounts));
	}

	@Test
	public void testWithRangeValues() {
		BinManager binManager = new BinManager(0, 1000);
		binManager.addValue(0, 1d);
		binManager.addValue(1000, 1d);
		binManager.addValue(1500, 2d);
		binManager.addValue(2000, 1d);
		binManager.addValue(2500, 2d);
		binManager.addValue(2999, 3d);

		DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
		NumberFormat nf = new DecimalFormat("0.00", dfs);
		nf.setRoundingMode(RoundingMode.HALF_UP);

		TestChannel channel = new TestChannel();
		binManager.toCsv(channel, "bin", "mean", nf, AggregationType.MEAN);

		assertThat(channel.lines,
				contains(
						"\"bin\";\"mean\"",
						"\"0.00\";\"1.00\"",
						"\"1.00\";\"1.50\"",
						"\"2.00\";\"2.00\""
				)
		);
	}

	@Test
	public void testWithPositiveDomainStart() {
		BinManager binManager = new BinManager(10500, 1000);
		binManager.addValue(12345);
		binManager.addValue(15999);
		binManager.addValue(16000);

		DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
		NumberFormat nf = new DecimalFormat("0.00", dfs);
		nf.setRoundingMode(RoundingMode.HALF_UP);

		TestChannel channel = new TestChannel();
		binManager.toCsv(channel, "bin", "count", nf);

		assertThat(channel.lines,
				contains(
						"\"bin\";\"count\"",
						"\"11.00\";\"0.00\"",
						"\"12.00\";\"1.00\"",
						"\"13.00\";\"0.00\"",
						"\"14.00\";\"0.00\"",
						"\"15.00\";\"0.00\"",
						"\"16.00\";\"2.00\""
				)
		);
	}

	static class TestChannel implements WritableByteChannel {
		List<String> lines = new LinkedList<>();

		@Override
		public int write(final ByteBuffer src) throws IOException {
			CharsetDecoder decoder = UTF_8.newDecoder();
			String line = decoder.decode(src).toString().trim();
			lines.add(line);
			return line.getBytes(UTF_8).length;
		}

		@Override
		public boolean isOpen() {
			return false;
		}

		@Override
		public void close() throws IOException {
			// no op
		}
	}
}
