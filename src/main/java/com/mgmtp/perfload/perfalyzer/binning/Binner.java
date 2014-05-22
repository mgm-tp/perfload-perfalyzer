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
package com.mgmtp.perfload.perfalyzer.binning;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.mgmtp.perfload.perfalyzer.util.ChannelManager;
import com.mgmtp.perfload.perfalyzer.util.FileNamingStrategy;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;

/**
 * Performs binning and aggregation tasks.
 * 
 * @author ctchinda
 */
public class Binner {

	private final File sourceDir;
	private final File destDir;
	private final BinningStrategy binningStrategy;

	/**
	 * @param sourceDir
	 *            the source directory where normalized files are located
	 * @param destDir
	 *            the destination directory
	 * @param binningStrategy
	 *            the strategy that contains the binning logic
	 */
	public Binner(final File sourceDir, final File destDir, final BinningStrategy binningStrategy) {
		this.sourceDir = sourceDir;
		this.destDir = destDir;
		this.binningStrategy = binningStrategy;
	}

	/**
	 * Performs the binning operation on the specified file.
	 * 
	 * @param file
	 *            the file to be binned and/or aggregated; must be relative to the source directory
	 */
	public void binFile(final PerfAlyzerFile file) throws IOException {
		FileInputStream fis = null;
		FileOutputStream fos = null;
		ChannelManager channelManager = new ChannelManager(destDir, new FileNamingStrategy() {
			@Override
			public PerfAlyzerFile createFileName(final String channelKey) {
				return file.copy().addFileNamePart(channelKey);
			}
		});
		try {
			fis = new FileInputStream(new File(sourceDir, file.getFile().getPath()));
			Scanner scanner = new Scanner(fis.getChannel(), Charsets.UTF_8.name());

			if (binningStrategy.needsBinning()) {

				File destFile = new File(destDir, binningStrategy.transformDefautBinnedFilePath(file));
				Files.createParentDirs(destFile);
				fos = new FileOutputStream(destFile);

				binningStrategy.binData(scanner, fos.getChannel());
			} else {

				// if binning is not necessary, no channel is provided
				binningStrategy.binData(scanner, null);
			}

			binningStrategy.aggregateData(channelManager);
		} finally {
			closeQuietly(fos);
			closeQuietly(channelManager);
			closeQuietly(fis);
		}
	}
}
