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

import com.google.common.collect.ImmutableList;
import com.mgmtp.perfload.perfalyzer.util.ChannelData;
import org.apache.commons.lang3.text.StrTokenizer;

import java.util.Collections;
import java.util.List;

import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;

/**
 * Normalizing implementation for load profiles.
 *
 * @author rnaegele
 */
public class LoadProfileNormalizingStrategy implements NormalizingStrategy {

	private static final String CHANNEL_BASE_NAME = "loadprofile";
	private static final String MARKER = "[[marker]]";

	private final StrTokenizer tokenizer = StrTokenizer.getCSVInstance();

	public LoadProfileNormalizingStrategy() {
		tokenizer.setDelimiterChar(DELIMITER);
	}

	@Override
	public List<ChannelData> normalizeLine(final String line) {
		tokenizer.reset(line);
		String[] tokens = tokenizer.getTokenArray();
		String operation = tokens[1];
		if (MARKER.equals(operation)) {
			return Collections.emptyList();
		}
		return ImmutableList.of(new ChannelData(CHANNEL_BASE_NAME, operation, line));
	}
}
