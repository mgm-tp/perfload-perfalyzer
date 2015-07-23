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

import static com.google.common.collect.Iterators.mergeSorted;
import static com.google.common.io.Files.createParentDirs;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.writeLineToChannel;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Merges multiple CSV files into a single one.
 *
 * @author rnaegele
 */
public class CsvFileSortMerger {
	private final Set<File> sourceFiles;
	private final File destFile;
	private final Comparator<String> comparator;

	/**
	 * @param sourceFiles
	 * 		the set of source files to be merged
	 * @param destFile
	 * 		the destination file
	 * @param comparator
	 * 		the comparator to use for sorting
	 */
	public CsvFileSortMerger(final Set<File> sourceFiles, final File destFile, final Comparator<String> comparator) {
		this.comparator = comparator;
		this.sourceFiles = ImmutableSet.copyOf(sourceFiles);
		this.destFile = destFile;
	}

	/**
	 * Merges the files specified in the constructor into the destination file.
	 */
	public void mergeFiles() throws IOException {
		createParentDirs(destFile);

		List<Slot> slots = new ArrayList<>(sourceFiles.size());
		for (File file : sourceFiles) {
			Slot slot = Slot.openSlot(file);
			slots.add(slot);
		}

		try (final FileOutputStream fis = new FileOutputStream(destFile)) {
			FileChannel writeChannel = fis.getChannel();
			mergeSorted(slots, comparator).forEachRemaining(line -> writeLineToChannel(writeChannel, line, Charset.forName("UTF-8")));
		} finally {
			slots.forEach(Slot::close);
		}
	}

	static class Slot implements Closeable, Iterator<String> {
		private final File file;
		private FileInputStream inputStream;
		private Scanner scanner;

		private Slot(final File file) {
			this.file = file;
		}

		public static Slot openSlot(final File file) {
			Slot slot = new Slot(file);
			slot.doOpen();
			return slot;
		}

		private void doOpen() {
			try {
				if (inputStream == null) {
					inputStream = new FileInputStream(file);
					scanner = new Scanner(inputStream);
				}
			} catch (FileNotFoundException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		/**
		 * Closes the internally used {@link Scanner} and {@link java.io.InputStream}.
		 */
		@Override
		public void close() {
			try {
				if (scanner != null) {
					scanner.close();
				}
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		@Override
		public boolean hasNext() {
			return scanner.hasNext();
		}

		@Override
		public String next() {
			return scanner.nextLine();
		}
	}
}
