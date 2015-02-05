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
package com.mgmtp.perfload.perfalyzer.reporting;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.io.Resources;
import com.mgmtp.perfload.perfalyzer.annotations.ReportDir;
import com.mgmtp.perfload.perfalyzer.annotations.ReportPreparationDir;
import com.mgmtp.perfload.perfalyzer.annotations.ReportTabNames;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Files.newReader;
import static com.google.common.io.Files.newWriter;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.util.PerfAlyzerUtils.extractFileNameParts;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.substringBefore;

/**
 * @author rnaegele
 */
@Singleton
public class ReportCreator {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final TestMetadata testMetadata;
	private final File soureDir;
	private final File destDir;
	private final Map<String, List<Pattern>> reportContentsConfigMap;
	private final StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
	private final ResourceBundle resourceBundle;

	private final Locale locale;
	private final List<String> tabNames;

	@Inject
	public ReportCreator(final TestMetadata testMetadata, @ReportPreparationDir final File soureDir,
			@ReportDir final File destDir, final Map<String, List<Pattern>> reportContentsConfigMap,
			final ResourceBundle resourceBundle, final Locale locale, @ReportTabNames final List<String> tabNames) {
		this.testMetadata = testMetadata;
		this.soureDir = soureDir;
		this.destDir = destDir;
		this.reportContentsConfigMap = reportContentsConfigMap;
		this.resourceBundle = resourceBundle;
		this.locale = locale;
		this.tabNames = tabNames;
		tokenizer.setDelimiterChar(DELIMITER);
	}

	public void createReport(final List<PerfAlyzerFile> files) throws IOException {
		Function<PerfAlyzerFile, String> classifier = perfAlyzerFile -> {
			String marker = perfAlyzerFile.getMarker();
			return marker == null ? "Overall" : marker;
		};
		Supplier<Map<String, List<PerfAlyzerFile>>> mapFactory = () -> new TreeMap<>(Ordering.explicit(tabNames));

		Map<String, List<PerfAlyzerFile>> filesByMarker = files.stream().collect(Collectors.groupingBy(classifier, mapFactory, toList()));

		Map<String, SortedSetMultimap<String, PerfAlyzerFile>> contentItemFiles = new LinkedHashMap<>();

		for (Entry<String, List<PerfAlyzerFile>> entry : filesByMarker.entrySet()) {
			SortedSetMultimap<String, PerfAlyzerFile> contentItemFilesByMarker =
					contentItemFiles.computeIfAbsent(entry.getKey(), s -> TreeMultimap.create(
							new ItemComparator(reportContentsConfigMap.get("priorities")),
							Ordering.natural()));

			for (PerfAlyzerFile perfAlyzerFile : entry.getValue()) {
				File file = perfAlyzerFile.getFile();
				String groupKey = removeExtension(file.getPath());
				boolean excluded = false;
				for (Pattern pattern : reportContentsConfigMap.get("exclusions")) {
					Matcher matcher = pattern.matcher(groupKey);
					if (matcher.matches()) {
						excluded = true;
						log.debug("Excluded from report: {}", groupKey);
						break;
					}
				}
				if (!excluded) {
					contentItemFilesByMarker.put(groupKey, perfAlyzerFile);
				}
			}
		}

		// explicitly copy it because it is otherwise filtered from the report in order to only show in the overview
		String loadProfilePlot = new File("console", "[loadprofile].png").getPath();
		copyFile(new File(soureDir, loadProfilePlot), new File(destDir, loadProfilePlot));

		Map<String, List<ContentItem>> tabItems = new LinkedHashMap<>();
		Map<String, QuickJump> quickJumps = new HashMap<>();
		Set<String> tabNames = contentItemFiles.keySet();

		for (Entry<String, SortedSetMultimap<String, PerfAlyzerFile>> tabEntry : contentItemFiles.entrySet()) {
			String tab = tabEntry.getKey();
			SortedSetMultimap<String, PerfAlyzerFile> filesForTab = tabEntry.getValue();

			List<ContentItem> contentItems = tabItems.computeIfAbsent(tab, list -> new ArrayList<>());
			Map<String, String> quickJumpMap = new LinkedHashMap<>();
			quickJumps.put(tab, new QuickJump(tab, quickJumpMap));

			int itemIndex = 0;
			for (Entry<String, Collection<PerfAlyzerFile>> itemEntry : filesForTab.asMap().entrySet()) {
				String title = itemEntry.getKey();
				Collection<PerfAlyzerFile> itemFiles = itemEntry.getValue();

				TableData tableData = null;
				String plotSrc = null;
				for (PerfAlyzerFile file : itemFiles) {
					if ("png".equals(getExtension(file.getFile().getName()))) {
						plotSrc = file.getFile().getPath();
						copyFile(new File(soureDir, plotSrc), new File(destDir, plotSrc));
					} else {
						tableData = createTableData(file.getFile());
					}
				}

				// strip off potential marker
				title = substringBefore(title, "{");

				String[] titleParts = split(title, SystemUtils.FILE_SEPARATOR);
				StringBuilder sb = new StringBuilder(50);
				String separator = " - ";
				sb.append(resourceBundle.getString(titleParts[0]));
				sb.append(separator);
				sb.append(resourceBundle.getString(titleParts[1]));

				List<String> fileNameParts = extractFileNameParts(titleParts[1], true);
				if (titleParts[1].contains("[distribution]")) {
					String operation = fileNameParts.get(1);
					sb.append(separator);
					sb.append(operation);
				} else if ("comparison".equals(titleParts[0])) {
					String operation = fileNameParts.get(1);
					sb.append(separator);
					sb.append(operation);
				} else if (titleParts[1].contains("[gclog]")) {
					if (fileNameParts.size() > 1) {
						sb.append(separator);
						sb.append(fileNameParts.get(1));
					}
				}

				title = sb.toString();
				ContentItem item = new ContentItem(tab, itemIndex, title, tableData, plotSrc,
						resourceBundle.getString("report.topLink"));
				contentItems.add(item);

				quickJumpMap.put(tab + "_" + itemIndex, title);
				itemIndex++;
			}
		}

		NavBar navBar = new NavBar(tabNames, quickJumps);
		String testName = removeExtension(testMetadata.getTestPlanFile());
		OverviewItem overviewItem = new OverviewItem(testMetadata, resourceBundle, locale);
		Content content = new Content(tabItems);

		String perfAlyzerVersion;
		try {
			perfAlyzerVersion = Resources.toString(Resources.getResource("perfAlyzer.version"), Charsets.UTF_8);
		} catch (IOException ex) {
			log.error("Could not read perfAlyzer version from classpath resource 'perfAlyzer.version'", ex);
			perfAlyzerVersion = "";
		}

		String dateTimeString = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withLocale(locale).format(ZonedDateTime.now());
		String createdString = String.format(resourceBundle.getString("footer.created"), perfAlyzerVersion, dateTimeString);
		HtmlSkeleton html = new HtmlSkeleton(testName, createdString, navBar, overviewItem, content);
		writeReport(html);
	}

	private void writeReport(final HtmlSkeleton html) throws IOException {
		try (Writer wr = newWriter(new File(destDir, "report.html"), Charsets.UTF_8)) {
			html.write(wr);
		}
	}

	private TableData createTableData(final File file) throws IOException {
		try (BufferedReader br = newReader(new File(soureDir, file.getPath()), Charsets.UTF_8)) {
			List<String> headers = null;
			List<List<String>> rows = newArrayList();
			int valueColumnsCount = 0;

			String fileName = file.getName();
			for (String line; (line = br.readLine()) != null; ) {
				tokenizer.reset(line);

				List<String> tokenList = tokenizer.getTokenList();
				if (headers == null) {
					headers = Lists.transform(tokenList, resourceBundle::getString);
					valueColumnsCount = tokenList.size() - 1;
					// TODO make that nicer
					if (file.getPath().startsWith("global") && !fileName.startsWith("[measuring][executions]") &&
							!fileName.contains("[distribution]")) {
						valueColumnsCount--;
					} else if (fileName.contains("[distribution]")) {
						valueColumnsCount -= 2;
					}
				} else {
					rows.add(tokenList);
				}
			}

			boolean imageInNewRow = fileName.contains("[distribution]") || fileName.contains("[executions]") || fileName.contains("[gclog]");
			return new TableData(headers, rows, valueColumnsCount, imageInNewRow);
		}
	}

	static class ItemComparator implements Comparator<String> {
		private final Logger log = LoggerFactory.getLogger(getClass());

		List<Pattern> priorityPatterns;
		int size;

		public ItemComparator(final List<Pattern> priorityPatterns) {
			this.priorityPatterns = priorityPatterns;
			this.size = priorityPatterns.size();
		}

		@Override
		public int compare(final String o1, final String o2) {
			int priority1 = getPriority(o1);
			int priority2 = getPriority(o2);

			int result = priority1 - priority2;
			if (result == 0) {
				result = o1.compareTo(o2);
			}
			log.debug("compareTo({}, {}): {}", priority1, priority2, result);
			return result;
		}

		int getPriority(final String s) {
			int result = Integer.MIN_VALUE + size - 1;

			// we must iterate in reverse order because the last element has the highest priority
			for (int i = size - 1; i >= 0; --i) {
				Pattern pattern = priorityPatterns.get(i);
				Matcher matcher = pattern.matcher(s);
				if (matcher.matches()) {
					result = result - i;
					log.debug("Priority match: [s={},pattern={},priority={}", s, pattern, result);
					return result; // negate, so it is sorted up in the set
				}
			}
			return 0;
		}
	}
}
