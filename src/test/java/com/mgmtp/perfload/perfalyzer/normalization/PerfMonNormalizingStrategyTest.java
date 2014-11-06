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
package com.mgmtp.perfload.perfalyzer.normalization;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.mgmtp.perfload.perfalyzer.util.ChannelData;
import com.mgmtp.perfload.perfalyzer.util.Marker;
import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.mgmtp.perfload.perfalyzer.hamcrest.RegexMatchers.matches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author rnaegele
 */
public class PerfMonNormalizingStrategyTest {

	private static final String PATTERN_CHANNEL_VALUE = "\"0\";\"%s\";\"%s\"";

	private final Map<String, String> channelValues = newHashMapWithExpectedSize(7);

	{
		channelValues.put("cpu_X", "42.0");
		channelValues.put("mem", String.valueOf(43d / 1024d));
		channelValues.put("swap", String.valueOf(44d / 1024d));
		channelValues.put("io_0_r", "0.0"); // normalized
		channelValues.put("io_0_w", "0.0"); // normalized
		channelValues.put("java_0", "48.0");
	}

	@Test
	public void testNormalization() throws IOException, NormalizationException {
		List<String> perfMonLines = Resources.readLines(Resources.getResource("normalization/perfmon.out"), Charsets.UTF_8);

		NormalizingStrategy strategy = new PerfMonNormalizingStrategy(new TimestampNormalizer(ZonedDateTime.parse(
				"2011-12-09T11:54:15.335+01:00"), ZonedDateTime.now(), 0), ImmutableList.<Marker>of());

		boolean firstLine = true;
		for (String line : perfMonLines) {
			List<ChannelData> channelDataList = strategy.normalizeLine("", line);
			if (firstLine) { // the meta line, which we are not interested in
				assertThat(channelDataList, hasSize(0));
				firstLine = false;
			} else {
				ChannelData channelData = channelDataList.get(0);
				String channelKey = channelData.getChannelKey();
				if (channelKey.startsWith("io_")) {
					assertThat(channelDataList, hasSize(2));
				} else {
					assertThat(channelDataList, hasSize(1));
				}
				String regex = String.format(PATTERN_CHANNEL_VALUE, channelKey, channelValues.get(channelKey));
				assertThat(channelData.getValue(), matches(regex));
			}
		}
	}
}
