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
package com.mgmtp.perfload.perfalyzer.util;

import static com.google.common.base.Preconditions.checkState;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Utility class for processing a file line by line. The line retrieved last is cached internally.
 * 
 * @author ctchinda
 */
public class LineCachingFileProcessor implements Closeable {

	private final File file;
	private FileInputStream inputStream;
	private Scanner scanner;

	private String cache;
	private boolean open;

	public LineCachingFileProcessor(final File file) {
		this.file = file;
	}

	public void open() throws IOException {
		if (inputStream == null) {
			inputStream = new FileInputStream(file);
			scanner = new Scanner(inputStream.getChannel());
			readNextLine();
			open = true;
		}
	}

	private void readNextLine() {
		cache = scanner.hasNext() ? scanner.nextLine() : null;
	}

	/**
	 * Checks whether a cached line is available.
	 * 
	 * @return {@code true} if no cached line is available
	 */
	public boolean isEmpty() {
		return cache == null;
	}

	/**
	 * Closes the internally used {@link Scanner} and {@link InputStream}.
	 */
	@Override
	public void close() throws IOException {
		open = false;

		if (scanner != null) {
			scanner.close();
		}
		if (inputStream != null) {
			inputStream.close();
		}
	}

	/**
	 * Returns the cache line.
	 * 
	 * @return the cache line, or {@code null} if none is available
	 */
	public String getCachedLine() {
		checkState(open, "not open");

		if (isEmpty()) {
			return null;
		}
		return cache;
	}

	/**
	 * Reads the next line and caches it.
	 * 
	 * @return the line
	 */
	public String readAndCacheNextLine() {
		String cacheLine = getCachedLine();
		readNextLine();
		return cacheLine;
	}
}
