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
package com.mgmtp.perfload.perfalyzer.normalization;

import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.util.StrBuilderUtils.appendEscapedAndQuoted;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.perfload.perfalyzer.util.ChannelData;
import com.mgmtp.perfload.perfalyzer.util.PerfMonTypeConfig;
import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;

/**
 * Normalizing implementation for perfMon logs.
 *
 * @author rnaegele
 */
public class PerfMonNormalizingStrategy implements NormalizingStrategy {

	private static final String CHANNEL_BASE_NAME = "perfmon";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
	private final TimestampNormalizer timestampNormalizer;
	private Map<String, Double> firstValues;

	public PerfMonNormalizingStrategy(final TimestampNormalizer timestampNormalizer) {
		this.timestampNormalizer = timestampNormalizer;
		tokenizer.setDelimiterChar('\t');
	}

	@Override
	public List<ChannelData> normalizeLine(final String line) {
		tokenizer.reset(line);
		List<String> tokenList = tokenizer.getTokenList();

		List<ChannelData> result = newArrayListWithExpectedSize(3);
		ZonedDateTime timestamp;
		try {
			timestamp = ZonedDateTime.parse(tokenList.get(0));
		} catch (IllegalArgumentException ex) {
			log.error("Invalid data line: {}", line);
			return result;
		}

		if (!timestampNormalizer.isInRange(timestamp)) {
			log.trace("Skipping perfMon entry. Timestamp not in time range of test: " + timestamp);
			return result;
		}

		String type = tokenList.get(1);

		for (PerfMonTypeConfig typeConfig : PerfMonTypeConfig.values()) {
			Matcher matcher = typeConfig.getPattern().matcher(type);

			if (matcher.matches()) {
				if (typeConfig.isNormalizeValues()) {
					if (firstValues == null) {
						firstValues = newHashMapWithExpectedSize(3);
					}
				}

				for (ValueHolder vh : selectValues(typeConfig, tokenList)) {
					String channelKey = type;
					if (vh.descriptor != null) {
						channelKey += "_" + vh.descriptor;
					}
					long normalizedTimestamp = timestampNormalizer.normalizeTimestamp(timestamp, 0L);

					try {
						// normalize against first value
						double value = typeConfig.factor(Double.parseDouble(vh.value));
						if (typeConfig.isNormalizeValues()) {
							Double firstValue = firstValues.get(channelKey);
							if (firstValue == null) {
								firstValues.put(channelKey, value);
								value = 0d;
							} else {
								value = value - firstValue;
							}
						}

						StrBuilder sb = new StrBuilder();
						appendEscapedAndQuoted(sb, DELIMITER, normalizedTimestamp);
						appendEscapedAndQuoted(sb, DELIMITER, channelKey);
						appendEscapedAndQuoted(sb, DELIMITER, value);

						String resultLine = sb.toString();
						result.add(new ChannelData(CHANNEL_BASE_NAME, channelKey, resultLine));
					} catch (NumberFormatException ex) {
						// in case a line in the perfmon file is incomplete
						log.warn("Could not parse line: " + line, ex);
					}
				}
				break;
			}
		}

		return result;
	}

	private List<ValueHolder> selectValues(final PerfMonTypeConfig typeConfig, final List<String> tokenList) {
		List<ValueHolder> result = newArrayListWithExpectedSize(2);

		switch (typeConfig) {
			case CPU:
			case DOCKER_CPU:
				result.add(new ValueHolder(tokenList.get(2)));
				break;
			case IO:
				result.add(new ValueHolder(tokenList.get(4), "r"));
				result.add(new ValueHolder(tokenList.get(5), "w"));
				break;
			case MEM:
			case SWAP:
			case DOCKER_MEM:
				result.add(new ValueHolder(tokenList.get(3)));
				break;
			case JAVA:
				result.add(new ValueHolder(substringBefore(tokenList.get(tokenList.size() - 2), "%")));
				break;
			default:
				throw new IllegalStateException("Invalid perfMon data type");
		}

		return result;
	}

	static class ValueHolder {
		String value;
		String descriptor;

		public ValueHolder(final String value, final String descriptor) {
			this.value = value;
			this.descriptor = descriptor;
		}

		public ValueHolder(final String value) {
			this(value, null);
		}
	}
}
