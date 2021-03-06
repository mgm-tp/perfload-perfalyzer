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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.lang3.text.StrTokenizer;

import com.google.common.base.Charsets;

/**
 * @author ctchinda
 */
public class BinnedFilesMerger {

	private final File inputDir;
	public static final char DELIMITER = '\t';
	private final File outputDir;
	private final int sortCriteriaColumn;
	public static final String FILE_TYPE = "measuring";

	public BinnedFilesMerger(final File inputDir, final File outputDir, final int sortCriteriaColumn) {
		this.inputDir = inputDir;
		this.outputDir = outputDir;
		this.sortCriteriaColumn = sortCriteriaColumn;
	}

	public void mergeFiles() throws IOException {
		if (!inputDir.isDirectory()) {
			throw new IllegalArgumentException("The input File must be a directory");
		}

		StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
		tokenizer.setDelimiterChar(DELIMITER);
		Map<String, FileChannel> destChannels = newHashMap();
		List<OutputStream> outputStreams = newArrayList();
		File[] filesInInputDirectory = inputDir.listFiles();

		try {
			for (File file : filesInInputDirectory) {
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(file);
					for (Scanner scanner = new Scanner(fis, Charsets.UTF_8.name()); scanner.hasNext();) {
						String line = scanner.nextLine();
						tokenizer.reset(line);

						List<String> tokenList = tokenizer.getTokenList();
						String key = tokenList.get(sortCriteriaColumn);
						FileChannel destChannel = destChannels.get(key);
						if (destChannel == null) {
							FileOutputStream fos = new FileOutputStream(new File(outputDir, FILE_TYPE + "_" + key + ".out"));
							outputStreams.add(fos);
							destChannel = fos.getChannel();
							destChannels.put(key, destChannel);

							//Write the Header...... Has to be improved
							IoUtilities.writeLineToChannel(destChannel, getHeader(), Charsets.UTF_8);
						}

						StrBuilder outputLine = new StrBuilder();
						for (String s : tokenList) {
							StrBuilderUtils.appendEscapedAndQuoted(outputLine, DELIMITER, s);
						}
						IoUtilities.writeLineToChannel(destChannel, outputLine.toString(), Charsets.UTF_8);
					}
				} finally {
					closeQuietly(fis);
				}
			}
		} finally {
			outputStreams.forEach(IOUtils::closeQuietly);
		}

	}

	public String getHeader() {
		return "OPERATION" + DELIMITER + "Target" + DELIMITER + "Q10" + DELIMITER + "Q50" + DELIMITER + "Q90" + DELIMITER + "Min"
				+ DELIMITER + "Max";
	}

}
