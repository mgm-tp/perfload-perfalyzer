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

import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.writeLineToChannel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.lang3.text.StrTokenizer;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.mgmtp.perfload.perfalyzer.util.LineCachingFileProcessor;

/**
 * Merges a set of CVS files into a single one. The sorting is based on a timestamp column whose
 * format must be accepted by {@link DateTime#DateTime(Object)}.
 * 
 * @author ctchinda
 */
public class DateTimeBasedCsvFileSortMerger {

	private final Set<File> sourceFiles;
	private final File destFile;
	private final int sortCriteriaColumn;
	private final char delimiter;

	/**
	 * @param sourceFiles
	 *            the set of source files to be merged
	 * @param destFile
	 *            the destination file
	 * @param sortColumnIndex
	 *            the index of the timestamp column for sorting
	 * @param delimiter
	 *            the CSV delimiter
	 */
	public DateTimeBasedCsvFileSortMerger(final Set<File> sourceFiles, final File destFile, final int sortColumnIndex,
			final char delimiter) {
		this.sourceFiles = ImmutableSet.copyOf(sourceFiles);
		this.destFile = destFile;
		this.sortCriteriaColumn = sortColumnIndex;
		this.delimiter = delimiter;
	}

	/**
	 * Merges the files into a single one. Directories are created as necessary.
	 */
	public void mergeFiles() throws IOException {
		Files.createParentDirs(destFile);

		PriorityQueue<LineCachingFileProcessor> pq = new PriorityQueue<>(11, new QueueComparator(delimiter, sortCriteriaColumn));

		for (File file : sourceFiles) {
			if (file.length() > 0) {
				LineCachingFileProcessor lcfp = new LineCachingFileProcessor(file);
				lcfp.open();
				pq.add(lcfp);
			}
		}

		try (final FileOutputStream fis = new FileOutputStream(destFile)) {
			FileChannel writeChannel = fis.getChannel();
			while (pq.size() > 0) {
				LineCachingFileProcessor lcfp = pq.poll();
				String line = lcfp.readAndCacheNextLine();
				writeLineToChannel(writeChannel, line, Charset.forName("UTF-8"));

				if (lcfp.isEmpty()) {
					lcfp.close();
				} else {
					pq.add(lcfp);
				}
			}
		} finally {
			for (LineCachingFileProcessor lcfp : pq) {
				lcfp.close();
			}
		}
	}

	private static final class QueueComparator implements Comparator<LineCachingFileProcessor> {

		private final StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
		private final int sortCriteriaColumn;

		public QueueComparator(final char delimiter, final int sortCriteriaColumn) {
			this.sortCriteriaColumn = sortCriteriaColumn;
			tokenizer.setDelimiterChar(delimiter);
		}

		@Override
		public int compare(final LineCachingFileProcessor lineCache1, final LineCachingFileProcessor lineCache2) {
			tokenizer.reset(lineCache1.getCachedLine());
			String[] tokens = tokenizer.getTokenArray();
			DateTime dtFirst = new DateTime(tokens[sortCriteriaColumn]);

			tokenizer.reset(lineCache2.getCachedLine());
			tokens = tokenizer.getTokenArray();
			DateTime dtSecond = new DateTime(tokens[sortCriteriaColumn]);

			return dtFirst.compareTo(dtSecond);
		}
	}
}
