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
package com.mgmtp.perfload.perfalyzer.util;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author ctchinda
 */
public class Marker {

	private final String name;
	private long leftMillis;
	private long rightMillis;
	private ZonedDateTime leftDateTime;
	private ZonedDateTime rightDateTime;
	private boolean locked;

	public Marker(final String name) {
		this.name = checkNotNull(name, "'name' must not be null");
	}

	/**
	 * check if a given time is within the Marker interval
	 *
	 * @param millis
	 *            the time in milliseconds
	 */
	public boolean isInMarker(final long millis) {
		return millis >= this.leftMillis && millis <= this.rightMillis;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the leftMillis
	 */
	public long getLeftMillis() {
		return leftMillis;
	}

	/**
	 * @param leftMillis
	 *            the leftMillis to set
	 */
	public void setLeftMillis(final long leftMillis) {
		checkState(!locked, "Markers has already been locked!");
		this.leftMillis = leftMillis;
	}

	/**
	 * @return the rightMillis
	 */
	public long getRightMillis() {
		return rightMillis;
	}

	/**
	 * @param rightMillis
	 *            the rightMillis to set
	 */
	public void setRightMillis(final long rightMillis) {
		checkState(!locked, "Markers has already been locked!");
		this.rightMillis = rightMillis;
	}

	public ZonedDateTime getLeftDateTime() {
		return leftDateTime;
	}

	public ZonedDateTime getRightDateTime() {
		return rightDateTime;
	}

	public void calculateDateTimeFields(final ZonedDateTime testStart) {
		checkState(!locked, "Markers has already been locked!");
		leftDateTime = testStart.plus(leftMillis, ChronoUnit.MILLIS);
		rightDateTime = testStart.plus(rightMillis, ChronoUnit.MILLIS);
		locked = true;
	}

	/**
	 * Checks whether the given timestamp is within the time range of the marker, i. e. not before
	 * the left marker and not after the right marker.
	 *
	 * @param timestamp the timestamp
	 * @return {@code true} if the timestamp is within the time range of the marker
	 */
	public boolean isInRange(final ZonedDateTime timestamp) {
		return !timestamp.isBefore(leftDateTime) && rightDateTime.isAfter(timestamp);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Marker marker = (Marker) o;

		if (leftMillis != marker.leftMillis) {
			return false;
		}
		if (rightMillis != marker.rightMillis) {
			return false;
		}
		return name.equals(marker.name);

	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + (int) (leftMillis ^ (leftMillis >>> 32));
		result = 31 * result + (int) (rightMillis ^ (rightMillis >>> 32));
		return result;
	}
}
