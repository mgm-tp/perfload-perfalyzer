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

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

/**
 * Normalizes a timestamp based on a reference timestamp.
 * 
 * @author rnaegele
 */
public class TimestampNormalizer {

	private final DateTime start;
	private final Interval interval;

	/**
	 * @param testStartDate
	 *            the test start date; used as reference timestamp for normalization
	 * @param testEndDate
	 *            the test end date
	 */
	public TimestampNormalizer(final DateTime testStartDate, final DateTime testEndDate, final int warmUpSeconds) {
		this.start = warmUpSeconds > 0L ? testStartDate.plusSeconds(warmUpSeconds) : testStartDate;
		this.interval = new Interval(start, testEndDate);
	}

	/**
	 * Checks whether the given timestamp is within the time range of the test, i. e. not before
	 * test start and not after test end.
	 * 
	 * @param timestamp
	 *            the timestamp
	 * @return {@code true} if the timestamp is within the time range of the test
	 */
	public boolean isInRange(final DateTime timestamp) {
		return interval.contains(timestamp);
	}

	/**
	 * Corrects the specified timestamp subtracting the specified offset, calculates the duration
	 * between the reference timestamp and the corrected timestamp, and returns the result in
	 * seconds.
	 * 
	 * @param timestamp
	 *            the timestamp to normalize
	 * @param offsetMillis
	 *            the offset in milliseconds
	 * @return the duration in seconds between the reference timestamp and the specified timestamp,
	 *         corrected by the specified offset
	 */
	public long normalizeTimestamp(final DateTime timestamp, final long offsetMillis) {
		DateTime corrected = timestamp.minus(offsetMillis);
		Duration duration = new Duration(start, corrected);
		return duration.getMillis();
	}
}
