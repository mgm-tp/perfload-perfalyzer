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

import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_RAW_COL_ERROR_MSG;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_RAW_COL_EXECUTION_ID;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_RAW_COL_OPERATION;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_RAW_COL_REQUEST_ID;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_RAW_COL_REQUEST_TYPE;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_RAW_COL_RESPONSE_TIME;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_RAW_COL_RESPONSE_TIME_FIRST_BYTE;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_RAW_COL_RESULT;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_RAW_COL_URI;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.MEASURING_RAW_COL_URI_ALIAS;
import static com.mgmtp.perfload.perfalyzer.util.StrBuilderUtils.appendEscapedAndQuoted;

import java.net.URI;
import java.util.List;

import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.lang3.text.StrTokenizer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.perfload.perfalyzer.util.ChannelData;
import com.mgmtp.perfload.perfalyzer.util.Marker;
import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;

/**
 * Normalizing implementation for measuring logs.
 * 
 * @author ctchinda
 */
public class MeasuringNormalizingStrategy implements NormalizingStrategy {

	private static final String CHANNEL_BASE_NAME = "measuring";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
	private final TimestampNormalizer timestampNormalizer;
	private final List<Marker> markers;

	public MeasuringNormalizingStrategy(final TimestampNormalizer timestampNormalizer, final List<Marker> markers) {
		this.timestampNormalizer = timestampNormalizer;
		this.markers = markers;
		tokenizer.setDelimiterChar(DELIMITER);
	}

	@Override
	public List<ChannelData> normalizeLine(final String fileName, final String line) {
		tokenizer.reset(line);
		String[] tokens = tokenizer.getTokenArray();

		List<ChannelData> channelDataList = newArrayListWithExpectedSize(3);
		DateTime timestamp = new DateTime(tokens[3]);

		if (!timestampNormalizer.isInRange(timestamp)) {
			log.trace("Skipping measuring entry. Timestamp not in time range of test: " + timestamp);
			return channelDataList;
		}

		StrBuilder sb = new StrBuilder(200);

		long normalizedTimestamp = timestampNormalizer.normalizeTimestamp(timestamp, 0L);

		String responseTimeFirstByte = tokens[MEASURING_RAW_COL_RESPONSE_TIME_FIRST_BYTE];
		String responseTime = tokens[MEASURING_RAW_COL_RESPONSE_TIME];
		String operation = tokens[MEASURING_RAW_COL_OPERATION];
		if (operation == null || operation.length() == 0) {
			return channelDataList;
		}

		String result = tokens[MEASURING_RAW_COL_RESULT];
		String errorMsg = tokens[MEASURING_RAW_COL_ERROR_MSG];
		String type = tokens[MEASURING_RAW_COL_REQUEST_TYPE];
		String uriString = tokens[MEASURING_RAW_COL_URI];
		String uriAlias = tokens[MEASURING_RAW_COL_URI_ALIAS];

		URI uri = URI.create(uriString);

		String uriPath = uri.getPath();
		String query = uri.getQuery();
		if (query != null) {
			uriPath += '?' + query;
		}
		if (uriString.equals(uriAlias)) {
			uriAlias = uriPath;
		}

		String executionId = tokens[MEASURING_RAW_COL_EXECUTION_ID];
		String requestId = tokens[MEASURING_RAW_COL_REQUEST_ID];

		appendEscapedAndQuoted(sb, DELIMITER, normalizedTimestamp);
		appendEscapedAndQuoted(sb, DELIMITER, responseTimeFirstByte);
		appendEscapedAndQuoted(sb, DELIMITER, responseTime);
		appendEscapedAndQuoted(sb, DELIMITER, operation);
		appendEscapedAndQuoted(sb, DELIMITER, type);
		appendEscapedAndQuoted(sb, DELIMITER, uriPath);
		appendEscapedAndQuoted(sb, DELIMITER, uriAlias);
		appendEscapedAndQuoted(sb, DELIMITER, result);
		appendEscapedAndQuoted(sb, DELIMITER, errorMsg);
		appendEscapedAndQuoted(sb, DELIMITER, executionId);
		appendEscapedAndQuoted(sb, DELIMITER, requestId);

		String resultLine = sb.toString();
		channelDataList.add(new ChannelData(CHANNEL_BASE_NAME, operation, resultLine));

		for (Marker marker : markers) {
			if (marker.isInMarker(normalizedTimestamp)) {
				sb = new StrBuilder();
				channelDataList.add(new ChannelData(CHANNEL_BASE_NAME, operation, marker.getName(), resultLine));
			}
		}

		return channelDataList;
	}
}
