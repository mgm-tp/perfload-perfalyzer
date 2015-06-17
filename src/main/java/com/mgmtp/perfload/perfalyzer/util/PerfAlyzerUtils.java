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

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.google.common.io.Files.newReader;
import static com.google.common.io.Files.readLines;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.text.StrTokenizer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.mgmtp.perfload.perfalyzer.reportpreparation.DisplayData;
import com.mgmtp.perfload.perfalyzer.reportpreparation.NumberDataSet.SeriesPoint;

/**
 * @author rnaegele
 */
public class PerfAlyzerUtils {
	/**
	 * Regular expression that matches a part of a perfAlyzer file. File name parts are enclosed by
	 * square brackets, e. g. {@code [perfmon][cpu_X].out}.
	 */
	private static final Pattern PATTERN_FILE_NAME_PARTS = Pattern.compile("\\[([^]]+)\\]");

	private PerfAlyzerUtils() {
	}

	/**
	 * Extracts file name parts as a list. File name parts are enclosed by square brackets, e. g.
	 * {@code [perfmon][cpu_X].out}.
	 * 
	 * @param fileName
	 *            the file name
	 * @return the list of file name parts including the square brackets
	 */
	public static List<String> extractFileNameParts(final String fileName) {
		return extractFileNameParts(fileName, false);
	}

	/**
	 * Extracts file name parts as a list. File name parts are enclosed by square brackets, e. g.
	 * {@code [perfmon][cpu_X].out}.
	 * 
	 * @param fileName
	 *            the file name
	 * @param stripBrackets
	 *            strips off the square brakets from the file name parts
	 * @return the list of file name parts
	 */
	public static List<String> extractFileNameParts(final String fileName, final boolean stripBrackets) {
		List<String> result = newArrayListWithExpectedSize(3);
		Matcher m = PATTERN_FILE_NAME_PARTS.matcher(fileName);
		while (m.find()) {
			result.add(stripBrackets ? m.group(1) : m.group());
		}
		return result;
	}

	/**
	 * Strips square brackets from a string, e. g. <br />
	 * {@code [foo] --> foo}
	 */
	public static String stripBrackets(final String s) {
		Matcher m = PATTERN_FILE_NAME_PARTS.matcher(s);
		return m.find() ? m.group(1) : m.group();
	}

	/**
	 * Reads a semicolon-delimited CSV file into a list. Each line in the result list will be
	 * another list of {@link Number} objects. The file is expected to have two numberic columns
	 * which are parsed using the specified number format.
	 * 
	 * @param file
	 *            the file
	 * @param charset
	 *            the character set to read the file
	 * @param numberFormat
	 *            the number format for parsing the column values
	 * @return the immutable result list
	 */
	public static List<SeriesPoint> readDataFile(final File file, final Charset charset, final NumberFormat numberFormat)
			throws IOException {
		final StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
		tokenizer.setDelimiterChar(';');

		try (BufferedReader br = newReader(file, charset)) {
			boolean headerLine = true;
			List<SeriesPoint> result = newArrayListWithExpectedSize(200);

			for (String line; (line = br.readLine()) != null;) {
				try {
					if (headerLine) {
						headerLine = false;
					} else {
						tokenizer.reset(line);
						String[] tokens = tokenizer.getTokenArray();
						double x = numberFormat.parse(tokens[0]).doubleValue();
						double y = numberFormat.parse(tokens[1]).doubleValue();

						if (!result.isEmpty()) {
							// additional point for histogram
							SeriesPoint previousPoint = getLast(result);
							result.add(new SeriesPoint(x, previousPoint.getY()));
						}
						tokenizer.reset(line);
						result.add(new SeriesPoint(x, y));
					}
				} catch (ParseException ex) {
					throw new IOException("Error parsing number in file: " + file, ex);
				}
			}

			int size = result.size();
			if (size > 2) {
				// additional point at end for histogram
				SeriesPoint nextToLast = result.get(size - 3);
				SeriesPoint last = result.get(size - 1);
				double dX = last.getX().doubleValue() - nextToLast.getX().doubleValue();
				result.add(new SeriesPoint(last.getX().doubleValue() + dX, last.getY()));
			}
			return ImmutableList.copyOf(result);
		}
	}

	/**
	 * Reads a semicolon-delimited CSV file into a map of lists series values. Values for each
	 * column are return as list of lists in the map, the key being the column header.
	 * 
	 * @param file
	 *            the file
	 * @param charset
	 *            the character set to read the file
	 * @param numberFormat
	 *            the number format for formatting the column values
	 * @param columnNames
	 *            the columns to consider
	 * 
	 * @return an immutable map of lists of series values
	 */
	public static Map<String, List<SeriesPoint>> readDataFile(final File file, final Charset charset,
			final NumberFormat numberFormat,
			final Set<String> columnNames) throws IOException {
		final StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
		tokenizer.setDelimiterChar(';');

		return readLines(file, charset, new LineProcessor<Map<String, List<SeriesPoint>>>() {
			private String[] headers;
			private final Map<String, List<SeriesPoint>> result = newHashMapWithExpectedSize(4);
			private int colCount;

			@Override
			public boolean processLine(final String line) throws IOException {
				try {
					tokenizer.reset(line);
					String[] tokens = tokenizer.getTokenArray();

					if (headers == null) {
						headers = tokens;
						colCount = tokens.length;
					} else {
						Integer counter = Integer.valueOf(tokens[0]);
						for (int i = 1; i < colCount; ++i) {
							String header = headers[i];
							if (columnNames.contains(header)) {
								List<SeriesPoint> colValues = result.get(header);
								if (colValues == null) {
									colValues = newArrayListWithExpectedSize(50);
									result.put(header, colValues);
								}
								colValues.add(new SeriesPoint(counter, numberFormat.parse(tokens[i])));
							}
						}
					}
					return true;
				} catch (ParseException ex) {
					throw new IOException("Error parsing number in file: " + file, ex);
				}
			}

			@Override
			public Map<String, List<SeriesPoint>> getResult() {
				return ImmutableMap.copyOf(result);
			}
		});
	}

	public static Map<String, String> readAggregatedMap(final File executionsFile, final Charset charset) throws IOException {
		final StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
		tokenizer.setDelimiterChar(';');

		Map<String, String> result = newHashMapWithExpectedSize(11);

		List<String> lines = Files.readLines(executionsFile, charset);
		String[] headers = null;

		for (String line : lines) {
			tokenizer.reset(line);
			String[] tokens = tokenizer.getTokenArray();

			if (headers == null) {
				headers = tokens;
			} else {

				String[] data = tokenizer.getTokenArray();

				String operation = data[0];
				for (int i = 1; i < headers.length; ++i) {
					result.put(operation + "." + headers[i], data[i]);
				}
			}
		}

		return result;
	}

	public static DisplayData selectDisplayData(final File file, final List<DisplayData> displayDataList) {
		for (DisplayData displayData : displayDataList) {
			Matcher matcher = displayData.getPattern().matcher(file.getName());
			if (matcher.matches()) {
				return displayData;
			}
		}
		throw new IllegalStateException("No display data matched for file: " + file);
	}
}
