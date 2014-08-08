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

import static com.google.common.base.Preconditions.checkState;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.computeNormalizedPath;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recursively copies or extracts files to a destination directory.
 *
 * @author rnaegele
 */
public class ArchiveExtracter extends DirectoryWalker<File> {
	private static final Pattern ARCHIVE_PATTERN = Pattern.compile("\\.(zip|tar\\.gz|tgz)$");

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final File baseDir;
	private final String normalizedBaseDirPath;
	private final String normalizedDestDirPath;

	private String currentNormalizedRelativeDirPath;

	/**
	 * @param baseDir
	 *            the base directory
	 * @param destDir
	 *            the destination directory
	 */
	public ArchiveExtracter(final File baseDir, final File destDir) throws IOException {
		this.baseDir = baseDir;
		this.normalizedBaseDirPath = computeNormalizedPath(baseDir);
		this.normalizedDestDirPath = computeNormalizedPath(destDir);
	}

	/**
	 * Walk the directory extracting or copying files.
	 */
	public void extract() throws IOException {
		walk(baseDir, null);
	}

	/**
	 * Computes and saves the current, normalized, relative directory.
	 */
	@Override
	protected void handleDirectoryStart(final File directory, final int depth, final Collection<File> results) throws IOException {
		String normalizedDirPath = computeNormalizedPath(directory);
		currentNormalizedRelativeDirPath = substringAfter(normalizedDirPath, normalizedBaseDirPath);
	}

	/**
	 * Processes a file. If the file is an archive (zip, tar.gz, tgz), it is extracted to a
	 * directory with the name of the archive file. Otherwise, the file is copied to the destination
	 * directory.
	 */
	@Override
	protected void handleFile(final File file, final int depth, final Collection<File> results) throws IOException {
		String fileName = file.getName();

		File targetDir = new File(normalizedDestDirPath, currentNormalizedRelativeDirPath);
		if (!targetDir.exists()) {
			checkState(targetDir.mkdirs(), "Could not create directory: " + targetDir);
		}

		Matcher matcher = ARCHIVE_PATTERN.matcher(fileName);
		if (matcher.find()) {
			log.debug("Extracting file: {}", file);

			try {
				String extension = matcher.group(1);
				String baseName = StringUtils.substringBeforeLast(fileName, extension);
				Archiver archiver = ArchiverFactory.createArchiver(file);
				archiver.extract(file, new File(targetDir, baseName));
			} catch (IOException ex) {
				log.error("Error extracting file: " + file, ex);
			}
		} else {
			log.debug("Copying file: {}", file);

			// We skip supervisor.log because the Supervisor might be running perfAlyzer
			// and the file could be locked. The file is not needed anyways.
			if (!"supervisor.log".equals(file.getName())) {
				FileUtils.copyFile(file, new File(targetDir, fileName));
			}
		}
	}
}
