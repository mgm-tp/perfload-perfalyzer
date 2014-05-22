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

/**
 * @author ctchinda
 */
public class Marker {

	private final String name;
	private long leftMillis;
	private long rightMillis;

	public Marker(final String name) {
		this.name = name;
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
		this.rightMillis = rightMillis;
	}
}
