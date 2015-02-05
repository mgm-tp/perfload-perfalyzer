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
package com.mgmtp.perfload.perfalyzer.normalization;

import com.google.common.base.Charsets;
import com.mgmtp.perfload.perfalyzer.util.ChannelData;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.writeLineToChannel;
import static org.apache.commons.io.FilenameUtils.getPath;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.apache.commons.lang3.StringUtils.split;

/**
 * Performs normalization tasks.
 *
 * @author ctchinda
 */
public class Normalizer {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final File sourceDir;
	private final File destDir;
	private final NormalizingStrategy normalizingStrategy;

	/**
	 * @param sourceDir
	 * 		the source directory where normalized files are located
	 * @param destDir
	 * 		the destination directory
	 * @param normalizingStrategy
	 * 		the strategy that contains the normalizing logic
	 */
	public Normalizer(final File sourceDir, final File destDir, final NormalizingStrategy normalizingStrategy) {
		this.sourceDir = sourceDir;
		this.destDir = destDir;
		this.normalizingStrategy = normalizingStrategy;
	}

	public void normalize(final File file) throws IOException {
		checkState(!file.isAbsolute(), "'file' must be relative");

		String filePath = file.getPath();
		String[] pathElements = split(getPath(filePath), SystemUtils.FILE_SEPARATOR); // strip out dir

		StrBuilder sb = new StrBuilder();
		for (int i = 0; i < pathElements.length; ++i) {
			if (i == 1) {
				continue; // strip out dir, e. g. perfmon-logs, measuring-logs
			}
			sb.appendSeparator(SystemUtils.FILE_SEPARATOR);
			sb.append(pathElements[i]);
		}
		String dirPath = sb.toString();

		Map<String, FileChannel> channels = newHashMap();
		List<OutputStream> outputStreams = newArrayList();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(new File(sourceDir, filePath)); //relative to source dir
			for (Scanner scanner = new Scanner(fis.getChannel(), Charsets.UTF_8.name()); scanner.hasNext(); ) {
				String line = scanner.nextLine();
				if (trimToNull(line) == null || line.startsWith("#")) {
					continue;
				}
				List<ChannelData> channelDataList = normalizingStrategy.normalizeLine(line);
				for (ChannelData channelData : channelDataList) {

					FileChannel channel = channels.get(channelData.getChannelKey());
					if (channel == null) {
						String baseName = channelData.getChannelBaseName();
						String key = channelData.getChannelKey();
						String fileName = new File(dirPath, String.format("[%s][%s].csv", baseName, key)).getPath();
						File destFile = new File(destDir, fileName);
						destFile.getParentFile().mkdirs();
						FileOutputStream fos = new FileOutputStream(destFile);
						outputStreams.add(fos);
						channel = fos.getChannel();
						channels.put(channelData.getChannelKey(), channel);
					}

					writeLineToChannel(channel, channelData.getValue(), Charsets.UTF_8);
				}
			}
		} finally {
			outputStreams.forEach(IOUtils::closeQuietly);
			closeQuietly(fis);
		}
	}
}
