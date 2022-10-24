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

import static com.google.common.collect.Lists.newArrayList;
import static com.mgmtp.perfload.perfalyzer.util.DirectoryLister.listPerfAlyzerFiles;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.createTempDir;
import static com.mgmtp.perfload.perfalyzer.util.PerfFunctions.makeAbsolute;
import static com.mgmtp.perfload.perfalyzer.util.PerfPredicates.fileNameContains;
import static com.mgmtp.perfload.perfalyzer.util.PerfPredicates.perfAlyzerFileNameContains;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.deleteQuietly;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.slf4j.MDC;

import com.google.common.collect.ImmutableList;
import com.mgmtp.perfload.perfalyzer.PerfAlyzerException;
import com.mgmtp.perfload.perfalyzer.annotations.FloatFormat;
import com.mgmtp.perfload.perfalyzer.annotations.IntFormat;
import com.mgmtp.perfload.perfalyzer.annotations.MaxHistoryItems;
import com.mgmtp.perfload.perfalyzer.binning.Binner;
import com.mgmtp.perfload.perfalyzer.binning.ErrorCountBinningStragegy;
import com.mgmtp.perfload.perfalyzer.binning.MeasuringAggregatedRequestsBinningStrategy;
import com.mgmtp.perfload.perfalyzer.binning.MeasuringRequestsBinningStrategy;
import com.mgmtp.perfload.perfalyzer.binning.MeasuringResponseTimesBinningStrategy;
import com.mgmtp.perfload.perfalyzer.binning.RequestFilesMerger;
import com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants;
import com.mgmtp.perfload.perfalyzer.normalization.MeasuringNormalizingStrategy;
import com.mgmtp.perfload.perfalyzer.normalization.Normalizer;
import com.mgmtp.perfload.perfalyzer.reportpreparation.DisplayData;
import com.mgmtp.perfload.perfalyzer.reportpreparation.MeasuringReportPreparationStrategy;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator;
import com.mgmtp.perfload.perfalyzer.reportpreparation.ReportPreparationStrategy;
import com.mgmtp.perfload.perfalyzer.reportpreparation.ReporterPreparator;
import com.mgmtp.perfload.perfalyzer.util.CsvFileSortMerger;
import com.mgmtp.perfload.perfalyzer.util.CsvTimestampColumnComparator;
import com.mgmtp.perfload.perfalyzer.util.DirectoryLister;
import com.mgmtp.perfload.perfalyzer.util.Marker;
import com.mgmtp.perfload.perfalyzer.util.NumberFormatProvider;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;
import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;

/**
 * @author ctchinda
 */
public class MeasuringWorkflow extends AbstractWorkflow {

	private final int maxHistoryItems;

	public MeasuringWorkflow(final TimestampNormalizer timestampNormalizer, @IntFormat final NumberFormatProvider intProvider,
			@FloatFormat final NumberFormatProvider floatNumberFormatProvider, final List<DisplayData> displayDataList,
			final ResourceBundle resourceBundle, final PlotCreator plotCreator, final TestMetadata testMetadata,
			@MaxHistoryItems final int maxHistoryItems) {
		super(timestampNormalizer, intProvider, floatNumberFormatProvider, displayDataList, resourceBundle, testMetadata, plotCreator);
		this.maxHistoryItems = maxHistoryItems;
	}

	@Override
	public List<Runnable> getNormalizationTasks(final File inputDir, final File outputDir) {
		Runnable task = () -> {
			List<File> inputFiles = DirectoryLister.listFiles(inputDir);
			Set<File> fileSet = inputFiles.stream().filter(fileNameContains("measuring")).map(makeAbsolute(inputDir)).collect(Collectors.toSet());
			final File sortMergeOutputDir = createTempDir();
			File mergedMeasuringLog = new File("global/measuring-logs/measuring.csv");
			try {
				log.info("Merging measuring logs to '{}'", mergedMeasuringLog);
				CsvFileSortMerger merger = new CsvFileSortMerger(fileSet, new File(sortMergeOutputDir, mergedMeasuringLog.getPath()),
						new CsvTimestampColumnComparator(';', 3));
				merger.mergeFiles();

				MeasuringNormalizingStrategy strat = new MeasuringNormalizingStrategy(timestampNormalizer);
				Normalizer normalizer = new Normalizer(sortMergeOutputDir, outputDir, strat);

				log.info("Normalizing '{}'", mergedMeasuringLog);
				normalizer.normalize(mergedMeasuringLog);
			} catch (Exception ex) {
				throw new PerfAlyzerException("Error normalizing file: " + mergedMeasuringLog, ex);
			} finally {
				deleteQuietly(sortMergeOutputDir);
			}
		};

		return ImmutableList.of(task);
	}

	@Override
	public List<Runnable> getBinningTasks(final File inputDir, final File outputDir, final Marker marker) {
		List<Runnable> tasks = newArrayList();

		final LatchProvider latchProvider = new LatchProvider();
		final long startOfFirstBin = marker != null ? marker.getLeftMillis() : 0;

		List<PerfAlyzerFile> inputFiles = listPerfAlyzerFiles(inputDir, marker);
		inputFiles.stream().filter(perfAlyzerFileNameContains("measuring")).forEach(file -> {
			tasks.add(() -> {
				MDC.put("file", file.getFile().getPath());
				try {
					log.info("Binning response times: '{}'", file);

					MeasuringResponseTimesBinningStrategy strategy = new MeasuringResponseTimesBinningStrategy(startOfFirstBin,
							intNumberFormatProvider.get(), floatNumberFormatProvider.get());
					final Binner binner = new Binner(inputDir, outputDir, strategy);
					binner.binFile(file);

					latchProvider.get().countDown();
				} catch (Exception ex) {
					throw new PerfAlyzerException("Error binning response times: " + file, ex);
				} finally {
					MDC.remove("file");
				}
			});
			tasks.add(() -> {
				MDC.put("file", file.getFile().getPath());
				try {
					log.info("Binning requests: '{}'", file);

					MeasuringRequestsBinningStrategy strategy = new MeasuringRequestsBinningStrategy(startOfFirstBin,
							PerfAlyzerConstants.BIN_SIZE_MILLIS_1_MINUTE, intNumberFormatProvider.get(),
							floatNumberFormatProvider.get());
					final Binner binner = new Binner(inputDir, outputDir, strategy);
					binner.binFile(file);

					latchProvider.get().countDown();
				} catch (Exception ex) {
					throw new PerfAlyzerException("Error binning requests: " + file, ex);
				} finally {
					MDC.remove("file");
				}
			});
			tasks.add(() -> {
				MDC.put("file", file.getFile().getPath());
				try {
					log.info("Binning requests: '{}'", file);

					MeasuringRequestsBinningStrategy strategy = new MeasuringRequestsBinningStrategy(startOfFirstBin,
							PerfAlyzerConstants.BIN_SIZE_MILLIS_1_SECOND, intNumberFormatProvider.get(),
							floatNumberFormatProvider.get());
					final Binner binner = new Binner(inputDir, outputDir, strategy);
					binner.binFile(file);

					latchProvider.get().countDown();
				} catch (Exception ex) {
					throw new PerfAlyzerException("Error binning requests: " + file, ex);
				} finally {
					MDC.remove("file");
				}
			});
			tasks.add(() -> {
				MDC.put("file", file.getFile().getPath());
				try {
					log.info("Binning requests: '{}'", file);
					MeasuringAggregatedRequestsBinningStrategy strategy = new MeasuringAggregatedRequestsBinningStrategy(
							startOfFirstBin, intNumberFormatProvider.get(), floatNumberFormatProvider.get());
					final Binner binner = new Binner(inputDir, outputDir, strategy);
					binner.binFile(file);

					latchProvider.get().countDown();
				} catch (Exception ex) {
					throw new PerfAlyzerException("Error binning requests: " + file, ex);
				} finally {
					MDC.remove("file");
				}
			});
			tasks.add(() -> {
				MDC.put("file", file.getFile().getPath());
				try {
					log.info("Binning errors: '{}'", file);
					ErrorCountBinningStragegy strategy = new ErrorCountBinningStragegy(startOfFirstBin,
							intNumberFormatProvider.get(), floatNumberFormatProvider.get());
					final Binner binner = new Binner(inputDir, outputDir, strategy);
					binner.binFile(file);

					latchProvider.get().countDown();
				} catch (Exception ex) {
					throw new PerfAlyzerException("Error binning requests: " + file, ex);
				} finally {
					MDC.remove("file");
				}
			});
		});

		// this makes sure that the next tasks has to wai until the already added ones have finished
		latchProvider.latch = new CountDownLatch(tasks.size());

		tasks.add(() -> {
			try {
				latchProvider.get().await();
				RequestFilesMerger merger = new RequestFilesMerger(outputDir);
				merger.mergeFiles(listPerfAlyzerFiles(outputDir, marker));
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				log.error(ex.getMessage(), ex);
			} catch (Exception ex) {
				throw new PerfAlyzerException("Error merging files", ex);
			}
		});

		return ImmutableList.copyOf(tasks);
	}

	@Override
	public List<Runnable> getReportPreparationTasks(final File inputDir, final File outputDir, final Marker marker) {
		Runnable task = () -> {
			try {
				log.info("Preparing report data...");

				ReportPreparationStrategy strategy = new MeasuringReportPreparationStrategy(intNumberFormatProvider.get(),
						floatNumberFormatProvider.get(), displayDataList, resourceBundle, plotCreator, testMetadata,
						rangeFromMarker(marker), maxHistoryItems);
				final ReporterPreparator reporter = new ReporterPreparator(inputDir, outputDir, strategy);

				List<PerfAlyzerFile> inputFiles = listPerfAlyzerFiles(inputDir, marker);
				reporter.processFiles(inputFiles.stream().filter(perfAlyzerFileNameContains("measuring")).collect(toList()));
			} catch (Exception ex) {
				throw new PerfAlyzerException("Error creating measuring report files", ex);
			}
		};

		return ImmutableList.of(task);
	}

	static class LatchProvider {

		CountDownLatch latch;

		public CountDownLatch get() {
			return latch;
		}
	}
}
