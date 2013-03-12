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
package com.mgmtp.perfload.perfalyzer.normalization;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.joda.time.DateTime;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.mgmtp.perfload.perfalyzer.normalization.MeasuringNormalizingStrategy;
import com.mgmtp.perfload.perfalyzer.normalization.NormalizationException;
import com.mgmtp.perfload.perfalyzer.normalization.NormalizingStrategy;
import com.mgmtp.perfload.perfalyzer.util.ChannelData;
import com.mgmtp.perfload.perfalyzer.util.Marker;
import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;

/**
 * @author ctchinda
 */
public class MeasuringNormalizingStrategyTest {

	private final String inputString = "\"1\";\"1\";\"1\";\"2000-01-01\";\"193\";\"288\";\"kapesta\";\"appserver01\";\"SUCCESS\";\"\";\"GET\";\"http://elsterltas01:7777/eportal/eop/auth/AuthentisierungDispatcher.tax\";\"uriAlias\";\"/192.168.19.103\";\"client\";\"2000000009101500512\";\"2000000009101500512\"";
	private final String outputString = "\"946681200000\";\"193\";\"288\";\"kapesta\";\"GET\";\"/eportal/eop/auth/AuthentisierungDispatcher.tax\";\"uriAlias\";\"SUCCESS\";\"\";\"2000000009101500512\";\"2000000009101500512\"";

	@Test
	public void testNormalization() throws NormalizationException {
		NormalizingStrategy strategy = new MeasuringNormalizingStrategy(new TimestampNormalizer(new DateTime(0L), new DateTime(),
				0), ImmutableList.<Marker>of());
		List<ChannelData> result = strategy.normalizeLine("", inputString);
		ChannelData channel = getOnlyElement(result);
		assertThat("channel key", channel.getChannelKey(), is(equalTo("kapesta")));
		assertThat(channel.getValue(), equalTo(outputString));
	}
}
