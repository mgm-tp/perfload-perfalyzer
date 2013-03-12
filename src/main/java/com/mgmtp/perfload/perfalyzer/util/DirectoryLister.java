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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.makeRelative;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.DirectoryWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * Recursively walks a directory adding all files in the tree to a list. The files in the list will
 * be relative to the specified base directory.
 * 
 * @author rnaegele
 */
public abstract class DirectoryLister<T> extends DirectoryWalker<T> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final File baseDir;
	private List<T> files;

	/**
	 * @param baseDir
	 *            the directory to analyze recursively
	 */
	private DirectoryLister(final File baseDir) {
		this.baseDir = baseDir;
	}

	/**
	 * Transforms the specified file to one relative to the base directory where processing started
	 * and adds it to the results collection. Thee {@code depth} parameter is not used.
	 */
	@Override
	protected void handleFile(final File file, final int depth, final Collection<T> results) throws IOException {
		log.debug("Adding file to list: {}", file);

		File relativeFile = makeRelative(baseDir, file);
		addResult(results, relativeFile);
	}

	protected abstract void addResult(Collection<T> results, final File relativeFile);

	/**
	 * Walks the directory tree starting with the base directory specified in the constructor.
	 * 
	 * @see #handleFile(File, int, Collection)
	 */
	protected void walkDirectories() throws IOException {
		Collection<T> tmpFiles = newArrayListWithExpectedSize(50);
		walk(baseDir, tmpFiles);
		files = ImmutableList.copyOf(tmpFiles);
	}

	/**
	 * Returns the collected files, all of which being relative to the base directory specified in
	 * the constructor.
	 * 
	 * @return the list of files collected
	 * @throws NullPointerException
	 *             if the internal list of files is null, i. e. {@link #walkDirectories()} has not
	 *             been called before calling this method
	 */
	protected List<T> getFiles() {
		return checkNotNull(files, "No processing done yet. Please call 'walkDirectories()' first.");
	}

	public static List<File> listFiles(final File baseDir) throws IOException {
		DirectoryLister<File> rdl = new DirectoryLister<File>(baseDir) {
			@Override
			protected void addResult(final Collection<File> result, final File relativeFile) {
				result.add(relativeFile);
			}
		};
		rdl.walkDirectories();
		return rdl.getFiles();
	}

	public static List<PerfAlyzerFile> listPerfAlyzerFiles(final File baseDir) throws IOException {
		DirectoryLister<PerfAlyzerFile> rdl = new DirectoryLister<PerfAlyzerFile>(baseDir) {
			@Override
			protected void addResult(final Collection<PerfAlyzerFile> result, final File relativeFile) {
				result.add(PerfAlyzerFile.create(relativeFile));
			}
		};
		rdl.walkDirectories();
		return rdl.getFiles();
	}
}
