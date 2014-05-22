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
package com.mgmtp.perfload.perfalyzer.workflow;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static com.mgmtp.perfload.perfalyzer.util.PerfPredicates.fileNameEquals;
import static com.mgmtp.perfload.perfalyzer.util.PerfPredicates.perfAlyzerFileNameContains;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.mgmtp.perfload.perfalyzer.PerfAlyzerException;
import com.mgmtp.perfload.perfalyzer.annotations.IntFormat;
import com.mgmtp.perfload.perfalyzer.binning.Binner;
import com.mgmtp.perfload.perfalyzer.binning.PerfMonBinningStrategy;
import com.mgmtp.perfload.perfalyzer.normalization.Normalizer;
import com.mgmtp.perfload.perfalyzer.normalization.PerfMonNormalizingStrategy;
import com.mgmtp.perfload.perfalyzer.reportpreparation.DisplayData;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PerfMonReportPreparationStrategy;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator;
import com.mgmtp.perfload.perfalyzer.reportpreparation.ReportPreparationStrategy;
import com.mgmtp.perfload.perfalyzer.reportpreparation.ReporterPreparator;
import com.mgmtp.perfload.perfalyzer.util.Marker;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;
import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;

/**
 * @author rnaegele
 */
@Singleton
public class PerfMonWorkflow extends AbstractWorkflow {

	@Inject
	public PerfMonWorkflow(final TimestampNormalizer timestampNormalizer, final List<Marker> markers,
			final @IntFormat Provider<NumberFormat> intNumberFormatProvider,
			final @IntFormat Provider<NumberFormat> numberFormatProvider1, final List<DisplayData> displayDataList,
			final ResourceBundle resourceBundle, final PlotCreator plotCreator, final TestMetadata testMetadata) {
		super(timestampNormalizer, markers, intNumberFormatProvider, numberFormatProvider1, displayDataList,
				resourceBundle, testMetadata, plotCreator);
	}

	@Override
	public List<Runnable> getNormalizationTasks(final File inputDir, final List<File> inputFiles, final File outputDir) {
		List<Runnable> tasks = newArrayList();

		for (final File file : from(inputFiles).filter(fileNameEquals("perfmon.out"))) {
			Runnable task = new Runnable() {
				@Override
				public void run() {
					try {
						log.info("Normalizing '{}'", file);
						PerfMonNormalizingStrategy strategy = new PerfMonNormalizingStrategy(timestampNormalizer, markers);
						final Normalizer normalizer = new Normalizer(inputDir, outputDir, strategy);
						normalizer.normalize(file);
					} catch (Exception ex) {
						throw new PerfAlyzerException("Error normalizing file: " + file, ex);
					}
				}
			};
			tasks.add(task);
		}

		return ImmutableList.copyOf(tasks);
	}

	@Override
	public List<Runnable> getBinningTasks(final File inputDir, final List<PerfAlyzerFile> inputFiles, final File outputDir) {
		List<Runnable> tasks = newArrayList();

		for (final PerfAlyzerFile file : from(inputFiles).filter(perfAlyzerFileNameContains("perfmon"))) {
			Runnable task = new Runnable() {
				@Override
				public void run() {
					try {
						log.info("Binning '{}'", file);
						PerfMonBinningStrategy strategy = new PerfMonBinningStrategy(intNumberFormatProvider.get(),
								floatNumberFormatProvider.get());
						final Binner binner = new Binner(inputDir, outputDir, strategy);
						binner.binFile(file);
					} catch (IOException ex) {
						throw new PerfAlyzerException("Error normalizing file: " + file, ex);
					}
				}
			};
			tasks.add(task);
		}

		return ImmutableList.copyOf(tasks);
	}

	@Override
	public List<Runnable> getReportPreparationTasks(final File inputDir, final List<PerfAlyzerFile> inputFiles,
			final File outputDir) {
		Runnable task = new Runnable() {
			@Override
			public void run() {
				log.info("Preparing report data...");

				try {
					ReportPreparationStrategy strategy = new PerfMonReportPreparationStrategy(intNumberFormatProvider.get(),
							floatNumberFormatProvider.get(), displayDataList, resourceBundle, plotCreator, testMetadata);
					final ReporterPreparator reporter = new ReporterPreparator(inputDir, outputDir, strategy);
					reporter.processFiles(from(inputFiles).filter(perfAlyzerFileNameContains("perfmon")).toList());
				} catch (IOException ex) {
					throw new PerfAlyzerException("Error creating perfMon report files", ex);
				} catch (ParseException ex) {
					throw new PerfAlyzerException("Error creating perfMon report files", ex);
				}
			}
		};

		return ImmutableList.of(task);
	}
}
