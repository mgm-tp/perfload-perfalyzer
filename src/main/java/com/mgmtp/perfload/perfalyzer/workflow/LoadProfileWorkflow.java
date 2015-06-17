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

import com.google.common.collect.ImmutableList;
import com.mgmtp.perfload.perfalyzer.PerfAlyzerException;
import com.mgmtp.perfload.perfalyzer.annotations.IntFormat;
import com.mgmtp.perfload.perfalyzer.binning.Binner;
import com.mgmtp.perfload.perfalyzer.binning.BinningStrategy;
import com.mgmtp.perfload.perfalyzer.binning.LoadProfileBinningStrategy;
import com.mgmtp.perfload.perfalyzer.normalization.LoadProfileNormalizingStrategy;
import com.mgmtp.perfload.perfalyzer.normalization.Normalizer;
import com.mgmtp.perfload.perfalyzer.normalization.NormalizingStrategy;
import com.mgmtp.perfload.perfalyzer.reportpreparation.DisplayData;
import com.mgmtp.perfload.perfalyzer.reportpreparation.LoadProfileReportPreparationStrategy;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator;
import com.mgmtp.perfload.perfalyzer.reportpreparation.ReportPreparationStrategy;
import com.mgmtp.perfload.perfalyzer.reportpreparation.ReporterPreparator;
import com.mgmtp.perfload.perfalyzer.util.Marker;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import static com.mgmtp.perfload.perfalyzer.util.DirectoryLister.listFiles;
import static com.mgmtp.perfload.perfalyzer.util.DirectoryLister.listPerfAlyzerFiles;
import static com.mgmtp.perfload.perfalyzer.util.PerfPredicates.fileNameMatchesWildcard;
import static com.mgmtp.perfload.perfalyzer.util.PerfPredicates.perfAlyzerFileNameContains;
import static java.util.stream.Collectors.toList;

/**
 * @author rnaegele
 */
@Singleton
public class LoadProfileWorkflow extends AbstractWorkflow {

	@Inject
	public LoadProfileWorkflow(@IntFormat final Provider<NumberFormat> intNumberFormatProvider,
			@IntFormat final Provider<NumberFormat> numberFormatProvider1, final List<DisplayData> displayDataList,
			final ResourceBundle resourceBundle, final PlotCreator plotCreator, final TestMetadata testMetadata) {
		super(null, intNumberFormatProvider, numberFormatProvider1, displayDataList, resourceBundle, testMetadata, plotCreator);
	}

	@Override
	public List<Runnable> getNormalizationTasks(final File inputDir, final File outputDir) {
		List<File> inputFiles = listFiles(inputDir);
		return inputFiles.stream().filter(fileNameMatchesWildcard("*.perfload")).map(file -> {
			Runnable task = () -> {
				MDC.put("file", file.getPath());
				try {
					log.info("Normalizing '{}'", file);
					NormalizingStrategy strategy = new LoadProfileNormalizingStrategy();
					final Normalizer normalizer = new Normalizer(inputDir, outputDir, strategy);
					normalizer.normalize(file);
				} catch (Exception ex) {
					throw new PerfAlyzerException("Error normalizing file: " + file, ex);
				} finally {
					MDC.remove("file");
				}
			};
			return task;
		}).collect(toList());
	}

	@Override
	public List<Runnable> getBinningTasks(final File inputDir, final File outputDir, final Marker marker) {
		if (marker != null) {
			// markers con't apply here
			return Collections.emptyList();
		}
		List<PerfAlyzerFile> inputFiles = listPerfAlyzerFiles(inputDir);
		return inputFiles.stream().filter(perfAlyzerFileNameContains("[loadprofile]")).map(file -> {
			Runnable task = () -> {
				MDC.put("file", file.getFile().getPath());
				try {
					log.info("Binning '{}'", file);
					BinningStrategy strategy = new LoadProfileBinningStrategy(intNumberFormatProvider.get(),
							floatNumberFormatProvider.get());
					final Binner binner = new Binner(inputDir, outputDir, strategy);
					binner.binFile(file);
				} catch (IOException ex) {
					throw new PerfAlyzerException("Error binning file: " + file, ex);
				} finally {
					MDC.remove("file");
				}
			};
			return task;
		}).collect(toList());
	}

	@Override
	public List<Runnable> getReportPreparationTasks(final File inputDir, final File outputDir, final Marker marker) {
		if (marker != null) {
			// markers con't apply here
			return Collections.emptyList();
		}
		Runnable task = () -> {
			log.info("Preparing report data...");

			try {
				ReportPreparationStrategy strategy = new LoadProfileReportPreparationStrategy(intNumberFormatProvider.get(),
						floatNumberFormatProvider.get(), displayDataList, resourceBundle, plotCreator, testMetadata, rangeFromMarker(marker));
				final ReporterPreparator reporter = new ReporterPreparator(inputDir, outputDir, strategy);

				List<PerfAlyzerFile> inputFiles = listPerfAlyzerFiles(inputDir, marker);
				reporter.processFiles(inputFiles.stream().filter(perfAlyzerFileNameContains("[loadprofile]")).collect(toList()));
			} catch (IOException ex) {
				throw new PerfAlyzerException("Error creating perfMon report files", ex);
			}
		};

		return ImmutableList.of(task);
	}
}
