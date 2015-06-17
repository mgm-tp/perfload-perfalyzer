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

import org.apache.commons.lang3.text.StrBuilder;

/**
 * @author rnaegele
 */
public class StrBuilderUtils {

	private static final char CSV_QUOTE = '"';

	private StrBuilderUtils() {
	}

	/**
	 * <p>
	 * Encloses the given value into double-quotes. Quote characters are escaped with an additional
	 * quote character. Line breaks are replace with a space character. Multiple line breaks are
	 * collapsed to a single space.
	 * </p>
	 * <p>
	 * If the specified {@link StrBuilder} is non-empty, the delimiter is appended first.
	 * </p>
	 * 
	 * @param sb
	 *            the {@link StrBuilder} the escaped and quoted result is appended to
	 * @param delimiter
	 *            the delimiter character
	 * @param value
	 *            the value to append
	 * @param moreValues
	 *            more values to append
	 */
	public static void appendEscapedAndQuoted(final StrBuilder sb, final char delimiter, final String value,
			final String... moreValues) {
		appendEscapedAndQuoted(sb, delimiter, value);
		if (moreValues != null) {
			for (String val : moreValues) {
				appendEscapedAndQuoted(sb, delimiter, val);
			}
		}
	}

	public static void appendEscapedAndQuoted(final StrBuilder sb, final char delimiter, final String value) {
		boolean foundLineBreak = false;

		sb.appendSeparator(delimiter);

		sb.append(CSV_QUOTE);
		if (value != null) {
			for (int i = 0, len = value.length(); i < len; ++i) {
				char c = value.charAt(i);
				switch (c) {
					case CSV_QUOTE:
						if (foundLineBreak) {
							foundLineBreak = false;
							sb.append(' ');
						}
						sb.append(c); // escape double quote, i. e. add quote character again
						break;
					case '\r':
					case '\n':
						foundLineBreak = true;
						continue;
					default:
						if (foundLineBreak) {
							sb.append(' ');
							foundLineBreak = false;
						}
						break;
				}
				sb.append(c);
			}
		}
		sb.append(CSV_QUOTE);
	}

	/**
	 * Encloses the given value into double-quotes. Quote characters are escaped with an additional
	 * quote character. Line breaks are replace with a space character. Multiple line breaks are
	 * collapsed to a single space.
	 * 
	 * @param sb
	 *            the {@link StrBuilder} the escaped and quoted result is appended to
	 * @param value
	 *            the value to append
	 */
	public static void appendEscapedAndQuoted(final StrBuilder sb, final char delimiter, final long value) {
		appendEscapedAndQuoted(sb, delimiter, String.valueOf(value));
	}

	/**
	 * Encloses the given value into double-quotes. Quote characters are escaped with an additional
	 * quote character. Line breaks are replace with a space character. Multiple line breaks are
	 * collapsed to a single space.
	 * 
	 * @param sb
	 *            the {@link StrBuilder} the escaped and quoted result is appended to
	 * @param value
	 *            the value to append
	 */
	public static void appendEscapedAndQuoted(final StrBuilder sb, final char delimiter, final int value) {
		appendEscapedAndQuoted(sb, delimiter, String.valueOf(value));
	}

	/**
	 * Encloses the given value into double-quotes. Quote characters are escaped with an additional
	 * quote character. Line breaks are replace with a space character. Multiple line breaks are
	 * collapsed to a single space.
	 * 
	 * @param sb
	 *            the {@link StrBuilder} the escaped and quoted result is appended to
	 * @param value
	 *            the value to append
	 */
	public static void appendEscapedAndQuoted(final StrBuilder sb, final char delimiter, final double value) {
		appendEscapedAndQuoted(sb, delimiter, String.valueOf(value));
	}
}
