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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.io.Files.newReader;
import static com.google.common.io.Files.newWriter;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.util.PerfAlyzerUtils.extractFileNameParts;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.StringUtils.split;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.io.Resources;
import com.mgmtp.perfload.perfalyzer.annotations.ReportDir;
import com.mgmtp.perfload.perfalyzer.annotations.ReportPreparationDir;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;

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

	@Inject
	public ReportCreator(final TestMetadata testMetadata, @ReportPreparationDir final File soureDir,
			@ReportDir final File destDir, final Map<String, List<Pattern>> reportContentsConfigMap,
			final ResourceBundle resourceBundle, final Locale locale) {
		this.testMetadata = testMetadata;
		this.soureDir = soureDir;
		this.destDir = destDir;
		this.reportContentsConfigMap = reportContentsConfigMap;
		this.resourceBundle = resourceBundle;
		this.locale = locale;
		tokenizer.setDelimiterChar(DELIMITER);
	}

	public void createReport(final List<File> files) throws IOException {
		SortedSetMultimap<String, File> contentItemFiles = TreeMultimap.create(
				new ItemComparator(reportContentsConfigMap.get("priorities")),
				Ordering.natural());

		for (File file : files) {
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
				contentItemFiles.put(groupKey, file);
			}
		}

		List<ContentItem> contentItems = newArrayList();
		Map<String, String> tocMap = newLinkedHashMap();

		int itemIndex = 0;
		for (Entry<String, Collection<File>> entry : contentItemFiles.asMap().entrySet()) {
			String title = entry.getKey();
			Collection<File> itemFiles = entry.getValue();

			TableData tableData = null;
			String plotSrc = null;
			for (File file : itemFiles) {
				if (getExtension(file.getName()).equals("png")) {
					plotSrc = file.getPath();
					copyFile(new File(soureDir, plotSrc), new File(destDir, plotSrc));
				} else {
					tableData = createTableData(file);
				}
			}

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
				sb.append(resourceBundle.getString(operation));
			} else if (titleParts[0].equals("comparison")) {
				String operation = fileNameParts.get(1);
				sb.append(separator);
				sb.append(resourceBundle.getString(operation));
			} else if (titleParts[1].contains("[gclog]")) {
				if (fileNameParts.size() > 1) {
					sb.append(separator);
					sb.append(fileNameParts.get(1));
				}
			}

			title = sb.toString();
			ContentItem item = new ContentItem(itemIndex++, title, tableData, plotSrc);
			contentItems.add(item);

			tocMap.put(String.valueOf(item.getItemIndex()), title);
		}

		String testName = removeExtension(testMetadata.getTestPlanFile());
		NavBar navBar = new NavBar(testName, tocMap);
		OverviewItem overviewItem = new OverviewItem(testMetadata, resourceBundle, locale);

		Content content = new Content(ImmutableMap.of("overall", contentItems));

		String perfAlyzerVersion;
		try {
			perfAlyzerVersion = Resources.toString(Resources.getResource("perfAlyzer.version"), Charsets.UTF_8);
		} catch (IOException ex) {
			log.error("Could not read perfAlyzer version from classpath resource 'perfAlyzer.version'", ex);
			perfAlyzerVersion = "";
		}

		String dateTimeString = DateTimeFormat.forStyle("FF").withLocale(locale).print(new DateTime());
		String createdString = String.format(resourceBundle.getString("footer.created"), perfAlyzerVersion, dateTimeString);
		HtmlSkeleton html = new HtmlSkeleton(testName, createdString, navBar, overviewItem, content,
				resourceBundle.getString("report.topLink"));
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
			for (String line = null; (line = br.readLine()) != null;) {
				tokenizer.reset(line);

				List<String> tokenList = tokenizer.getTokenList();
				if (headers == null) {
					headers = Lists.transform(tokenList, new Function<String, String>() {
						@Override
						public String apply(final String input) {
							return resourceBundle.getString(input);
						}
					});
					valueColumnsCount = tokenList.size() - 1;
					// TODO make that nicer
					if (file.getPath().startsWith("global") && !fileName.startsWith("[measuring][executions]")
							&& !fileName.contains("[distribution]")) {
						valueColumnsCount--;
					} else if (fileName.contains("[distribution]")) {
						valueColumnsCount -= 2;
					}
				} else {
					rows.add(tokenList);
				}
			}

			boolean imageInNewRow = fileName.contains("[distribution]") || fileName.contains("[executions]")
					|| fileName.contains("[gclog]");
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
			log.debug("compareTo({}, {}): {}", new Object[] { priority1, priority2, result });
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
					log.debug("Priority match: [s={},pattern={},priority={}", new Object[] { s, pattern, result });
					return result; // negate, so it is sorted up in the set
				}
			}
			return 0;
		}
	}
}
