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
package com.mgmtp.perfload.perfalyzer.hamcrest;

import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Hamcrest matchers that uses regular expressions.
 * 
 * @author rnaegele
 */
public final class RegexMatchers {

	private RegexMatchers() {
		// don't allow instantiation
	}

	private abstract static class BaseRegexMatcher extends TypeSafeMatcher<String> {
		protected final String regex;
		protected final Pattern compiledRegex;

		private BaseRegexMatcher(final String regex) {
			this.regex = regex;
			compiledRegex = Pattern.compile(regex);
		}
	}

	private static class MatchesRegexpMatcher extends BaseRegexMatcher {
		private MatchesRegexpMatcher(final String regex) {
			super(regex);
		}

		@Override
		public boolean matchesSafely(final String item) {
			return compiledRegex.matcher(item).matches();
		}

		@Override
		public void describeTo(final Description description) {
			description.appendText("matches regex ").appendValue(regex);
		}
	}

	private static class FindsRegexpMatcher extends BaseRegexMatcher {
		private FindsRegexpMatcher(final String regex) {
			super(regex);
		}

		@Override
		public boolean matchesSafely(final String item) {
			return compiledRegex.matcher(item).find();
		}

		@Override
		public void describeTo(final Description description) {
			description.appendText("finds match for regex ").appendValue(regex);
		}
	}

	/**
	 * Match the regexp against the whole input string
	 * 
	 * @param regex
	 *            the regular expression to match
	 * 
	 * @return a matcher which matches the whole input string
	 */
	public static Matcher<String> matches(final String regex) {
		return new MatchesRegexpMatcher(regex);
	}

	/**
	 * Match the regexp against any substring of the input string
	 * 
	 * @param regex
	 *            the regular expression to match
	 * 
	 * @return a matcher which matches anywhere in the input string
	 */
	public static Matcher<String> finds(final String regex) {
		return new FindsRegexpMatcher(regex);
	}
}
