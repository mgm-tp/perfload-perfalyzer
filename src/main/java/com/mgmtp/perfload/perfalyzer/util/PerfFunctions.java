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

import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;


/**
 * @author rnaegele
 */
public class PerfFunctions {

	private PerfFunctions() {
	}

	public static Function<File, File> toParentFile() {
		return new ToParentFile();
	}

	public static Function<File, File> makeRelativeTo(final File dir) {
		return new MakeRelativeFunction(dir);
	}

	public static Function<File, File> makeAbsolute(final File dir) {
		return new MakeAbsoluteFunction(dir);
	}

	public static Function<Long, Double> longToDouble() {
		return new LongToDouble();
	}

	private static class MakeRelativeFunction implements Function<File, File> {

		private final File dir;

		public MakeRelativeFunction(final File dir) {
			this.dir = dir;
		}

		@Override
		public File apply(final File input) {
			try {
				String relativeFile = substringAfter(normalize(input.getCanonicalPath()), dir.getCanonicalPath());
				return new File(relativeFile);
			} catch (IOException ex) {
				throw new IllegalStateException("Error computing relative file name for " + input, ex);
			}
		}
	}

	private static class MakeAbsoluteFunction implements Function<File, File> {

		private final File dir;

		public MakeAbsoluteFunction(final File dir) {
			this.dir = dir;
		}

		@Override
		public File apply(final File input) {
			return new File(dir, input.getPath());
		}
	}

	private static class ToParentFile implements Function<File, File> {

		@Override
		public File apply(final File input) {
			return input.getParentFile();
		}
	}

	private static class LongToDouble implements Function<Long, Double> {

		@Override
		public Double apply(final Long input) {
			return input.doubleValue();
		}
	}
}
