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

import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Comparator;

/**
 * A comparator for comparing lines of CSV files by a timestamp column.
 *
 * @author rnaegele
 */
public final class CsvTimestampColumnComparator implements Comparator<String> {
	private static final Logger LOGGER = LoggerFactory.getLogger(CsvTimestampColumnComparator.class);

	private final StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
	private final int column;

	/**
	 * @param delimiter
	 * 		the CSV delimiter
	 * @param column
	 * 		the column that contains the timestamp to compare
	 */
	public CsvTimestampColumnComparator(final char delimiter, final int column) {
		this.column = column;
		tokenizer.setDelimiterChar(delimiter);
	}

	@Override
	public int compare(final String line1, final String line2) {
		tokenizer.reset(line1);
		String[] tokens = tokenizer.getTokenArray();
		ZonedDateTime dtFirst = ZonedDateTime.parse(tokens[column]);

		tokenizer.reset(line2);
		tokens = tokenizer.getTokenArray();
		ZonedDateTime dtSecond = ZonedDateTime.parse(tokens[column]);

		int result = dtFirst.compareTo(dtSecond);
		LOGGER.debug("Date Comparision: {} {} {}", dtFirst, dtSecond, result);
		return result;
	}
}
