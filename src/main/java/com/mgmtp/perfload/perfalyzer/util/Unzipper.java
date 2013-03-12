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
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.computeNormalizedPath;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.zip.ZipFile;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recursively copies or unzips files to a destination directory.
 * 
 * @author rnaegele
 */
public class Unzipper extends DirectoryWalker<File> {
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
	public Unzipper(final File baseDir, final File destDir) throws IOException {
		this.baseDir = baseDir;
		this.normalizedBaseDirPath = computeNormalizedPath(baseDir);
		this.normalizedDestDirPath = computeNormalizedPath(destDir);
	}

	/**
	 *
	 */
	public void unzip() throws IOException {
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
	 * Processes a file. If the file is a zip, it is extracted to a dirctory with the name of the
	 * zip file. Otherwise, the file is copied to the destination dirctory.
	 */
	@Override
	protected void handleFile(final File file, final int depth, final Collection<File> results) throws IOException {

		String fileName = file.getName();

		File targetDir = new File(normalizedDestDirPath, currentNormalizedRelativeDirPath);
		if (!targetDir.exists()) {
			checkState(targetDir.mkdirs(), "Could not create directory: " + targetDir);
		}

		if (fileName.endsWith(".zip")) {
			log.debug("Unzipping file: {}", file);
			String baseName = getBaseName(fileName);
			IoUtilities.unzip(new ZipFile(file), new File(targetDir, baseName));
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
