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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.util.PerfPredicates.perfAlyzerFilePartsMatchWildcards;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.writeLines;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.io.Files;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;

/**
 * @author rnaegele
 */
public class RequestFilesMerger {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final File binnedDir;

	public RequestFilesMerger(final File binnedDir) {
		this.binnedDir = binnedDir;
	}

	public void mergeFiles(final List<PerfAlyzerFile> inputFiles) throws IOException {
		Predicate<PerfAlyzerFile> predicate1 = perfAlyzerFilePartsMatchWildcards("measuring", "*", "requestsPerInterval");
		Predicate<PerfAlyzerFile> predicate2 = perfAlyzerFilePartsMatchWildcards("measuring", "*", "aggregatedResponseTimes");

		Predicate<PerfAlyzerFile> predicateOr = predicate1.or(predicate2);

		Set<PerfAlyzerFile> paFiles = inputFiles.stream().filter(predicateOr).collect(toSet());
		ListMultimap<String, PerfAlyzerFile> byOperationMultimap = ArrayListMultimap.create();

		for (PerfAlyzerFile paf : paFiles) {
			byOperationMultimap.put(paf.getFileNameParts().get(1), paf);
		}

		StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
		tokenizer.setDelimiterChar(DELIMITER);

		for (String operation : byOperationMultimap.keySet()) {
			List<PerfAlyzerFile> list = byOperationMultimap.get(operation);

			checkState(list.size() == 2, "Two files are required by operation but found %d for '%s'", list.size(), operation);

			List<String> resultLines = newArrayListWithCapacity(2);

			PerfAlyzerFile paf1 = list.stream().filter(predicate1).findFirst().get();
			File file1 = new File(binnedDir, paf1.getFile().getPath());
			List<String> lines1 = Files.readLines(file1, Charsets.UTF_8);

			PerfAlyzerFile paf2 = list.stream().filter(predicate2).findFirst().get();
			File file2 = new File(binnedDir, paf2.getFile().getPath());
			List<String> lines2 = Files.readLines(file2, Charsets.UTF_8);

			if (lines1.size() == lines2.size()) {
				File resultFile = new File(binnedDir, paf1.copy().removeFileNamePart(2).addFileNamePart("aggregated").getFile().getPath());

				for (int i = 0; i < lines1.size(); ++i) {
					String line1 = get(lines1, i);
					String line2 = get(lines2, i);
					resultLines.add(line1 + DELIMITER + line2);
				}

				writeLines(resultFile, Charsets.UTF_8.name(), resultLines);

				deleteQuietly(file1);
				deleteQuietly(file2);
			} else {
				log.warn("Files to merge must have the same number of lines. Merging not possible: {}", list);
			}
		}
	}
}
