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
package com.mgmtp.perfload.perfalyzer.workflow;

import static com.mgmtp.perfload.perfalyzer.util.DirectoryLister.listFiles;
import static com.mgmtp.perfload.perfalyzer.util.DirectoryLister.listPerfAlyzerFiles;
import static com.mgmtp.perfload.perfalyzer.util.PerfPredicates.fileNameStartsWith;
import static com.mgmtp.perfload.perfalyzer.util.PerfPredicates.perfAlyzerFileNameContains;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.getPath;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrBuilder;

import com.google.common.collect.ImmutableList;
import com.mgmtp.perfload.perfalyzer.PerfAlyzerException;
import com.mgmtp.perfload.perfalyzer.annotations.FloatFormat;
import com.mgmtp.perfload.perfalyzer.annotations.IntFormat;
import com.mgmtp.perfload.perfalyzer.reportpreparation.DisplayData;
import com.mgmtp.perfload.perfalyzer.reportpreparation.GcLogReportPreparationStrategy;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator;
import com.mgmtp.perfload.perfalyzer.reportpreparation.ReporterPreparator;
import com.mgmtp.perfload.perfalyzer.util.Marker;
import com.mgmtp.perfload.perfalyzer.util.MemoryFormatProvider;
import com.mgmtp.perfload.perfalyzer.util.NumberFormatProvider;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;
import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;

/**
 * @author rnaegele
 */
public class GcLogWorkflow extends AbstractWorkflow {

	private final MemoryFormatProvider memoryFormatProvider;

	public GcLogWorkflow(final TimestampNormalizer timestampNormalizer, @IntFormat final NumberFormatProvider intProvider,
			@FloatFormat final NumberFormatProvider floatNumberFormatProvider, final List<DisplayData> displayDataList,
			final ResourceBundle resourceBundle, final PlotCreator plotCreator, final TestMetadata testMetadata,
			final MemoryFormatProvider memoryFormatProvider) {
		super(timestampNormalizer, intProvider, floatNumberFormatProvider, displayDataList, resourceBundle, testMetadata, plotCreator);
		this.memoryFormatProvider = memoryFormatProvider;
	}

	@Override
	public List<Runnable> getNormalizationTasks(final File inputDir, final File outputDir) {
		List<File> inputFiles = listFiles(inputDir);
		return inputFiles.stream().filter(fileNameStartsWith("gclog")).map(file -> {
			Runnable task = () -> {
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

				String s = trimToNull(substringAfter(getBaseName(filePath), "gclog"));
				File destFile = new File(outputDir, dirPath + SystemUtils.FILE_SEPARATOR + "[gclog]"
						+ (s != null ? "[" + s + "]." : ".")
						+ getExtension(filePath));

				try {
					copyFile(new File(inputDir, file.getPath()), destFile);
				} catch (IOException ex) {
					throw new PerfAlyzerException("Error copying file: " + file, ex);
				}
			};
			return task;
		}).collect(toList());
	}

	@Override
	public List<Runnable> getBinningTasks(final File inputDir, final File outputDir, final Marker marker) {
		if (marker != null) {
			// markers need to be treated in report preparation task for GC logs
			return Collections.emptyList();
		}
		List<PerfAlyzerFile> inputFiles = listPerfAlyzerFiles(inputDir);
		return inputFiles.stream().filter(perfAlyzerFileNameContains("[gclog]")).map(file -> {
			Runnable task = () -> {
				try {
					copyFile(new File(inputDir, file.getFile().getPath()), new File(outputDir, file.getFile().getPath()));
				} catch (IOException ex) {
					throw new PerfAlyzerException("Error copying file: " + file, ex);
				}
			};
			return task;
		}).collect(toList());
	}

	@Override
	public List<Runnable> getReportPreparationTasks(final File inputDir, final File outputDir, final Marker marker) {
		Runnable task = () -> {
			log.info("Preparing report data...");

			try {
				GcLogReportPreparationStrategy strategy = new GcLogReportPreparationStrategy(
						intNumberFormatProvider.get(), floatNumberFormatProvider.get(), displayDataList, resourceBundle,
						plotCreator, testMetadata, timestampNormalizer, memoryFormatProvider.get(), marker,
						rangeFromMarker(marker)
				);
				ReporterPreparator reporter = new ReporterPreparator(inputDir, outputDir, strategy);

				List<PerfAlyzerFile> inputFiles = listPerfAlyzerFiles(inputDir);
				reporter.processFiles(inputFiles.stream().filter(perfAlyzerFileNameContains("[gclog]")).collect(toList()));
			} catch (IOException ex) {
				throw new PerfAlyzerException("Error creating perfMon report files", ex);
			}
		};

		return ImmutableList.of(task);
	}
}
