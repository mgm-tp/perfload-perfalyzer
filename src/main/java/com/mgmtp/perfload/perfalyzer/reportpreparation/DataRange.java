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
package com.mgmtp.perfload.perfalyzer.reportpreparation;

/**
 * @author rnaegele
 */
public class DataRange {

	private final long lowerMillis;
	private final long upperMillis;

	public DataRange(final long lowerMillis, final long upperMillis) {
		this.lowerMillis = lowerMillis;
		this.upperMillis = upperMillis;
	}

	public long getLowerMillis() {
		return lowerMillis;
	}

	public long getUpperMillis() {
		return upperMillis;
	}

	public long getLowerSeconds() {
		return lowerMillis / 1000L;
	}

	public long getUpperSeconds() {
		return upperMillis / 1000L;
	}
}
