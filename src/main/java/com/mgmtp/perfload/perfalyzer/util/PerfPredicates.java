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

import static org.apache.commons.io.FilenameUtils.wildcardMatch;

import java.io.File;

import com.google.common.base.Predicate;

/**
 * @author rnaegele
 */
public class PerfPredicates {

	public static Predicate<File> parentFileNameEquals(final String fileName) {
		return new ParentFileNamePredicate(fileName);
	}

	public static Predicate<File> fileNameEquals(final String s) {
		return new FileNameEqualsPredicate(s);
	}

	public static Predicate<File> fileNameContains(final String s) {
		return new FileNameContainsPredicate(s);
	}

	public static Predicate<File> fileNameStartsWith(final String s) {
		return new FileNameStartsWithPredicate(s);
	}

	public static Predicate<PerfAlyzerFile> perfAlyzerFileNameContains(final String s) {
		return new PerfAlyzerFileNameContainsPredicate(s);
	}

	public static Predicate<PerfAlyzerFile> perfAlyzerFileNameMatchesWildcard(final String s) {
		return new PerfAlyzerFileNameMatchesWildcardPredicate(s);
	}

	public static Predicate<File> fileNameMatchesWildcard(final String s) {
		return new FileNameMatchesWildcardPredicate(s);
	}

	private static class ParentFileNamePredicate implements Predicate<File> {
		private final String fileName;

		public ParentFileNamePredicate(final String fileName) {
			this.fileName = fileName;
		}

		@Override
		public boolean apply(final File input) {
			return input.getParentFile().getName().equals(fileName);
		}
	}

	private static class FileNameEqualsPredicate implements Predicate<File> {
		private final String s;

		public FileNameEqualsPredicate(final String s) {
			this.s = s;
		}

		@Override
		public boolean apply(final File input) {
			return input.getName().equals(s);
		}
	}

	private static class FileNameContainsPredicate implements Predicate<File> {
		private final String s;

		public FileNameContainsPredicate(final String s) {
			this.s = s;
		}

		@Override
		public boolean apply(final File input) {
			return input.getName().contains(s);
		}
	}

	private static class FileNameStartsWithPredicate implements Predicate<File> {
		private final String s;

		public FileNameStartsWithPredicate(final String s) {
			this.s = s;
		}

		@Override
		public boolean apply(final File input) {
			return input.getName().startsWith(s);
		}
	}

	private static class PerfAlyzerFileNameContainsPredicate implements Predicate<PerfAlyzerFile> {
		private final String s;

		public PerfAlyzerFileNameContainsPredicate(final String s) {
			this.s = s;
		}

		@Override
		public boolean apply(final PerfAlyzerFile input) {
			return input.getFile().getName().contains(s);
		}
	}

	private static class PerfAlyzerFileNameMatchesWildcardPredicate implements Predicate<PerfAlyzerFile> {
		private final String s;

		public PerfAlyzerFileNameMatchesWildcardPredicate(final String s) {
			this.s = s;
		}

		@Override
		public boolean apply(final PerfAlyzerFile input) {
			return wildcardMatch(input.getFile().getName(), s);
		}
	}

	private static class FileNameMatchesWildcardPredicate implements Predicate<File> {
		private final String s;

		public FileNameMatchesWildcardPredicate(final String s) {
			this.s = s;
		}

		@Override
		public boolean apply(final File input) {
			return wildcardMatch(input.getName(), s);
		}
	}

}
