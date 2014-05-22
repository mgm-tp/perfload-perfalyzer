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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.text.ParsePosition;
import java.util.Map;

/**
 * Utility class for resolving placeholders in a string. A placeholder starts with <code>${</code>
 * and ends with <code>}</code>, e. g. <code>${myParam}</code>. Nested placeholders are not
 * supported.
 * 
 * @author rnaegele
 */
public final class PlaceholderUtils {

	private PlaceholderUtils() {
		//
	}

	/**
	 * Replaces all placeholders in the specified string using the specified map. If a placeholder
	 * value cannot be found in the map, the placeholder is left as is.
	 * 
	 * @param input
	 *            the string to parse
	 * @param replacements
	 *            a map containing replacement values for placeholders
	 * @return the string with all placeholders resolved
	 */
	public static String resolvePlaceholders(final String input, final Map<String, String> replacements) {
		if (isBlank(input)) {
			return input;
		}

		final int len = input.length();
		ParsePosition pos = new ParsePosition(0);
		String result = resolveNextPlaceholder(input, pos, replacements);
		if (result != null && pos.getIndex() >= len) {
			// we are done if there was no next placeholder and the
			// parse position is already at the end of the string
			return result;
		}
		StringBuilder sb = new StringBuilder(len * 2);
		if (result == null) {
			// Add the character if no placeholder is at the current position
			// and increment the position
			sb.append(input.charAt(pos.getIndex()));
			pos.setIndex(pos.getIndex() + 1);
		} else {
			sb.append(result);
		}

		// loop as long as the parse position is less than the input string's length
		while (pos.getIndex() < len) {
			result = resolveNextPlaceholder(input, pos, replacements);
			if (result == null) {
				// Add the character if no placeholder is at the current position
				// and increment the position
				sb.append(input.charAt(pos.getIndex()));
				pos.setIndex(pos.getIndex() + 1);
			} else {
				sb.append(result);
			}
		}
		return sb.toString();
	}

	/**
	 * Parses the next placeholder using {@link #parseNextPlaceholderName(String, ParsePosition)}
	 * and resolves it from the specified map. If a placeholder value cannot be found in the map,
	 * the placeholder is left as is.
	 * 
	 * @param input
	 *            the string to parse
	 * @param pos
	 *            the position where parsing starts
	 * @param replacements
	 *            a map containing replacement values for placeholders
	 * @return the replaced value
	 */
	public static String resolveNextPlaceholder(final String input, final ParsePosition pos,
			final Map<String, String> replacements) {
		if (isBlank(input)) {
			return input;
		}

		final int start = pos.getIndex();
		if (start > input.length()) {
			return null;
		}

		String placeholderName = parseNextPlaceholderName(input, pos);
		if (placeholderName != null) {
			String result = replacements.get(placeholderName);
			if (result != null) {
				return result;
			}
			// Return the placeholder if no replacement was found
			return input.substring(start, pos.getIndex());
		}
		return null;
	}

	/**
	 * Returns the next placeholder that can be parsed from the specified position in the specified
	 * string.
	 * 
	 * @param input
	 *            the string to parse
	 * @param pos
	 *            the position where parsing starts
	 * @return the next placeholder available or null if none is found
	 */
	public static String parseNextPlaceholderName(final String input, final ParsePosition pos) {
		int index = pos.getIndex();
		if (input.length() - index >= 3 && '$' == input.charAt(index) && '{' == input.charAt(index + 1)) {
			int start = index + 2;
			int end = input.indexOf('}', start);
			if (end < 0) {
				throw new IllegalStateException("Invalid placeholder: " + input.substring(index));
			}
			pos.setIndex(end + 1);
			return start == end ? "" : input.substring(start, end);
		}
		return null;
	}
}
