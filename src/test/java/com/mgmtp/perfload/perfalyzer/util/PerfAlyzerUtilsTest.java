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
package com.mgmtp.perfload.perfalyzer.util;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerUtils;

/**
 * @author rnaegele
 */
public class PerfAlyzerUtilsTest {

	private final Map<String, String> expected = newHashMapWithExpectedSize(11);

	@BeforeTest
	public void setUp() {
		expected.put("flushcache.numRequests", "2");
		expected.put("flushcache.numErrors", "0");
		expected.put("flushcache.minReqPerSec", "0");
		expected.put("flushcache.medianReqPerSec", "0");
		expected.put("flushcache.maxReqPerSec", "1");
		expected.put("flushcache.minReqPerMin", "0");
		expected.put("flushcache.medianReqPerMin", "0");
		expected.put("flushcache.maxReqPerMin", "2");
		expected.put("flushcache.minExecutionTime", "270");
		expected.put("flushcache.medianExecutionTime", "270");
		expected.put("flushcache.maxExecutionTime", "270");
		expected.put("browse.numRequests", "79,320");
		expected.put("browse.numErrors", "0");
		expected.put("browse.minReqPerSec", "0");
		expected.put("browse.medianReqPerSec", "76");
		expected.put("browse.maxReqPerSec", "222");
		expected.put("browse.minReqPerMin", "0");
		expected.put("browse.medianReqPerMin", "4,378");
		expected.put("browse.maxReqPerMin", "8,585");
		expected.put("browse.minExecutionTime", "242");
		expected.put("browse.medianExecutionTime", "3,174");
		expected.put("browse.maxExecutionTime", "198,476");
	}

	@Test
	public void testReadAggregatedMap() throws IOException {
		Map<String, String> map = PerfAlyzerUtils.readAggregatedMap(new File(
				"src/test/resources/utils/[measuring][executions].csv"), Charsets.UTF_8);
		assertEquals(map, expected);
	}
}
