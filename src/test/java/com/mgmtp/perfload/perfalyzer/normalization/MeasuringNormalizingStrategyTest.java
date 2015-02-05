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

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.mgmtp.perfload.perfalyzer.util.ChannelData;
import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;

/**
 * @author ctchinda
 * @author rnaegele
 */
public class MeasuringNormalizingStrategyTest {

	private static final String INPUT_1 =
			"\"1\";\"1\";\"1\";\"2000-01-02T00:00:00.000+00:00\";\"193\";\"288\";\"testoperation\";\"appserver01\";\"SUCCESS\";\"\";\"GET\";\"http://www.mgm-tp.com/foo\";\"uriAlias\";\"/192.168.19.103\";\"client\";\"2000000009101500512\";\"2000000009101500512\"";
	private static final String INPUT_2 =
			"\"1\";\"1\";\"1\";\"2000-01-02T00:00:00.000+00:00\";\"193\";\"288\";\"testoperation\";\"appserver01\";\"SUCCESS\";\"\";\"GET\";\"com.mgmtp.test.MyClass.myMethod(int)\";\"com.mgmtp.test.MyClass.myMethod(int)\";\"/192.168.19.103\";\"client\";\"2000000009101500512\";\"2000000009101500512\"";
	private static final String INPUT_INVALID_URI_SYNTAX =
			"\"1\";\"1\";\"1\";\"2000-01-02T00:00:00.000+00:00\";\"193\";\"288\";\"testoperation\";\"appserver01\";\"SUCCESS\";\"\";\"GET\";\"com.mgmtp.test.MyClass.myMethod(int, long)\";\"uriAlias\";\"/192.168.19.103\";\"client\";\"2000000009101500512\";\"2000000009101500512\"";
	private static final String OUTPUT_1 =
			"\"86400000\";\"193\";\"288\";\"testoperation\";\"GET\";\"/foo\";\"uriAlias\";\"SUCCESS\";\"\";\"2000000009101500512\";\"2000000009101500512\"";
	private static final String OUTPUT_2 =
			"\"86400000\";\"193\";\"288\";\"testoperation\";\"GET\";\"com.mgmtp.test.MyClass.myMethod(int)\";\"com.mgmtp.test.MyClass.myMethod(int)\";\"SUCCESS\";\"\";\"2000000009101500512\";\"2000000009101500512\"";
	private static final String OUTPUT_INVALID_URI_SYNTAX =
			"\"86400000\";\"193\";\"288\";\"testoperation\";\"GET\";\"com.mgmtp.test.MyClass.myMethod(int, long)\";\"uriAlias\";\"SUCCESS\";\"\";\"2000000009101500512\";\"2000000009101500512\"";

	@DataProvider(name = "testdata")
	public Object[][] createData1() {
		return new Object[][]{
				{INPUT_1, OUTPUT_1},
				{INPUT_2, OUTPUT_2},
				{INPUT_INVALID_URI_SYNTAX, OUTPUT_INVALID_URI_SYNTAX}
		};
	}

	@Test(dataProvider = "testdata")
	public void testNormalization(final String input, final String output) throws NormalizationException {
		NormalizingStrategy strategy = new MeasuringNormalizingStrategy(
				new TimestampNormalizer(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.ofOffset("UTC", ZoneOffset.UTC)), ZonedDateTime.now(), 0));
		List<ChannelData> result = strategy.normalizeLine(input);
		ChannelData channel = getOnlyElement(result);
		assertThat("channel key", channel.getChannelKey(), is(equalTo("testoperation")));
		assertThat(channel.getValue(), equalTo(output));
	}
}
