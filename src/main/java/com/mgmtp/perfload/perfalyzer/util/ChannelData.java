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
 * @author rnaegele
 */
public class ChannelData {

	private final String channelBaseName;
	private final String channelKey;
	private final String marker;
	private final String value;

	public ChannelData(final String channelBaseName, final String channelKey, final String marker, final String value) {
		this.channelBaseName = channelBaseName;
		this.channelKey = channelKey;
		this.marker = marker;
		this.value = value;
	}

	public ChannelData(final String channelBaseName, final String channelKey, final String value) {
		this(channelBaseName, channelKey, null, value);

	}

	/**
	 * @return the channelBaseName
	 */
	public String getChannelBaseName() {
		return channelBaseName;
	}

	/**
	 * @return the channelKey
	 */
	public String getChannelKey() {
		return channelKey;
	}

	/**
	 * @return the marker
	 */
	public String getMarker() {
		return marker;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
}
