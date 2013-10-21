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
package com.mgmtp.perfload.perfalyzer.reportpreparation;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.util.PerfAlyzerUtils.readDataFile;
import static com.mgmtp.perfload.perfalyzer.util.StrBuilderUtils.appendEscapedAndQuoted;
import static org.apache.commons.io.FileUtils.readLines;
import static org.apache.commons.io.FileUtils.writeLines;
import static org.apache.commons.io.FilenameUtils.getPath;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.StringUtils.split;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.lang3.text.StrTokenizer;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.mgmtp.perfload.perfalyzer.reportpreparation.NumberDataSet.SeriesPoint;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator.AxisType;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator.ChartDimensions;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator.RendererType;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;

/**
 * @author ctchinda
 */
public class PerfMonReportPreparationStrategy extends AbstractReportPreparationStrategy {

	public PerfMonReportPreparationStrategy(final NumberFormat intNumberFormat,
			final NumberFormat floatNumberFormat, final List<DisplayData> displayDataList,
			final ResourceBundle resourceBundle, final PlotCreator plotCreator, final TestMetadata testMetadata) {
		super(intNumberFormat, floatNumberFormat, displayDataList, resourceBundle, plotCreator, testMetadata);
	}

	@Override
	public void processFiles(final File sourceDir, final File destDir, final List<PerfAlyzerFile> files) throws IOException,
			ParseException {

		ListMultimap<String, PerfAlyzerFile> byTypeAndHostMultimapAggregated = ArrayListMultimap.create();
		ListMultimap<String, PerfAlyzerFile> byTypeAndHostMultimap = ArrayListMultimap.create();

		for (PerfAlyzerFile f : files) {
			log.info("Processing file '{}'...", f);

			File file = f.getFile();
			String name = file.getName();

			// strip all numbers in order to group by type, e. g.
			// java_0_ --> java
			// java_1_ --> java
			// io_0_w --> io_w
			// io_0_r --> io_r
			String key = getPath(file.getPath()) + name.replaceAll("_\\d+", "");

			if ("aggregated".equals(getLast(f.getFileNameParts()))) {
				// CSV for HTML tables
				key = key.replace("[aggregated]", "");
				byTypeAndHostMultimapAggregated.put(key, f);
			} else {
				// PNGs
				// already change extension here
				key = removeExtension(key) + ".png";
				byTypeAndHostMultimap.put(key, f);
			}
		}

		createCsvFiles(sourceDir, destDir, byTypeAndHostMultimapAggregated);
		createPlots(sourceDir, destDir, byTypeAndHostMultimap);
	}

	private void createCsvFiles(final File sourceDir, final File destDir,
			final ListMultimap<String, PerfAlyzerFile> byTypeAndHostMultimap)
			throws IOException {

		ListMultimap<String, String> globalContentListMultimap = LinkedListMultimap.create();
		StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
		tokenizer.setDelimiterChar(DELIMITER);

		for (String key : byTypeAndHostMultimap.keySet()) {
			List<PerfAlyzerFile> filesByType = byTypeAndHostMultimap.get(key);
			File destFile = new File(destDir, key);

			String[] split = split(key, SystemUtils.FILE_SEPARATOR, 2);
			String host = split[0];
			String keyWithoutHost = split[1];

			List<String> contentList = newLinkedList();

			for (PerfAlyzerFile f : filesByType) {
				String type = f.getFileNameParts().get(1);

				// aggregated files always have two lines, a header and a data line
				List<String> lines = readLines(new File(sourceDir, f.getFile().getPath()), Charsets.UTF_8);
				if (lines.size() < 2) {
					// needs at least header and one content line
					continue;
				}
				if (contentList.isEmpty()) {
					// write header
					contentList.add(0, "\"type\"" + DELIMITER + lines.get(0));
				}
				String line = lines.get(1);
				tokenizer.reset(line);

				String[] columns = tokenizer.getTokenArray();

				StrBuilder sb = new StrBuilder(10 + line.length());
				appendEscapedAndQuoted(sb, DELIMITER, type);
				for (String column : columns) {
					appendEscapedAndQuoted(sb, DELIMITER, column);
				}

				line = sb.toString();
				contentList.add(line);

				List<String> globalContentList = globalContentListMultimap.get(keyWithoutHost);
				if (globalContentList.isEmpty()) {
					globalContentList.add("\"host\"" + DELIMITER + "\"type\"" + DELIMITER + lines.get(0));
				}
				globalContentList.add("\"" + host + "\"" + DELIMITER + line);
			}

			// exclude header line from sorting
			Collections.sort(contentList.subList(1, contentList.size()));

			writeLines(destFile, Charsets.UTF_8.name(), contentList);
		}

		for (String key : globalContentListMultimap.keySet()) {
			List<String> globalContentList = globalContentListMultimap.get(key);

			// exclude header line from sorting
			Collections.sort(globalContentList.subList(1, globalContentList.size()));

			writeLines(new File(destDir, "global" + SystemUtils.FILE_SEPARATOR + key), Charsets.UTF_8.name(), globalContentList);
		}
	}

	private void createPlots(final File sourceDir, final File destDir,
			final ListMultimap<String, PerfAlyzerFile> byTypeAndHostMultimap)
			throws IOException {

		Map<String, NumberDataSet> globalDataSets = newHashMap();

		for (String key : byTypeAndHostMultimap.keySet()) {
			List<PerfAlyzerFile> filesByType = byTypeAndHostMultimap.get(key);
			File destFile = new File(destDir, key);

			String[] split = split(key, SystemUtils.FILE_SEPARATOR, 2);
			String host = split[0];
			String keyWithoutHost = split[1];

			NumberDataSet dataSet = new NumberDataSet();

			for (PerfAlyzerFile f : filesByType) {
				String type = f.getFileNameParts().get(1);
				List<SeriesPoint> dataList = readDataFile(new File(sourceDir, f.getFile().getPath()), Charsets.UTF_8,
						intNumberFormat);
				dataSet.addSeries(type, dataList);

				NumberDataSet globalDataSet = globalDataSets.get(keyWithoutHost);
				if (globalDataSet == null) {
					globalDataSet = new NumberDataSet();
					globalDataSets.put(keyWithoutHost, globalDataSet);
				}

				globalDataSet.addSeries(host + ":" + type, dataList);
			}

			plotCreator.writePlotFile(destFile, AxisType.LINEAR, AxisType.LINEAR, RendererType.STEPS, ChartDimensions.DEFAULT,
					dataSet);
		}

		for (Entry<String, NumberDataSet> entry : globalDataSets.entrySet()) {
			NumberDataSet dataSet = entry.getValue();
			File destFile = new File(destDir, "global" + SystemUtils.FILE_SEPARATOR + entry.getKey());
			plotCreator.writePlotFile(destFile, AxisType.LINEAR, AxisType.LINEAR, RendererType.STEPS, ChartDimensions.DEFAULT,
					dataSet);
		}
	}
}
