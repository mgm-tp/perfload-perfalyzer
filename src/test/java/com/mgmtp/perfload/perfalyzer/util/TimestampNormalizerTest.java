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
package com.mgmtp.perfload.perfalyzer.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.joda.time.DateTime;
import org.testng.annotations.Test;

import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;

/**
 * @author rnaegele
 */
public class TimestampNormalizerTest {

	private final TimestampNormalizer tn = new TimestampNormalizer(new DateTime(2012, 1, 1, 1, 1), new DateTime(), 0);
	private final DateTime input = new DateTime(2012, 1, 1, 3, 1);

	@Test
	public void testWithoutOffset() {
		long millis = tn.normalizeTimestamp(input, 0L);
		assertThat(millis, is(equalTo(2 * 3600L * 1000L))); // two hours
	}

	@Test
	public void testWithPositiveOffset() {
		long millis = tn.normalizeTimestamp(input, 3600000L /* one hour */);
		assertThat(millis, is(equalTo(3600L * 1000L))); // one hour
	}

	@Test
	public void testWithNegativeOffset() {
		long millis = tn.normalizeTimestamp(input, -3600000L /* one hour */);
		assertThat(millis, is(equalTo(3 * 3600L * 1000L))); // three hours
	}
}
