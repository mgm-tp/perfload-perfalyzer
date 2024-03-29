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
package com.mgmtp.perfload.perfalyzer;

import static com.google.common.base.Preconditions.checkState;
import static com.mgmtp.perfload.perfalyzer.util.DirectoryLister.listAllPerfAlyzerFiles;
import static com.mgmtp.perfload.perfalyzer.util.DirectoryLister.listPerfAlyzerFiles;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.writeLineToChannel;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newByteChannel;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.apache.commons.io.FileUtils.copyDirectoryToDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Stopwatch;
import com.mgmtp.perfload.perfalyzer.annotations.BinnedDir;
import com.mgmtp.perfload.perfalyzer.annotations.DoBinning;
import com.mgmtp.perfload.perfalyzer.annotations.DoNormalization;
import com.mgmtp.perfload.perfalyzer.annotations.DoReportPreparation;
import com.mgmtp.perfload.perfalyzer.annotations.NormalizedDir;
import com.mgmtp.perfload.perfalyzer.annotations.ReportDir;
import com.mgmtp.perfload.perfalyzer.annotations.ReportPreparationDir;
import com.mgmtp.perfload.perfalyzer.annotations.UnzippedDir;
import com.mgmtp.perfload.perfalyzer.reporting.ReportCreator;
import com.mgmtp.perfload.perfalyzer.reporting.email.EmailReporter;
import com.mgmtp.perfload.perfalyzer.util.Marker;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.workflow.WorkflowExecutor;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PerfAlyzer class is the entry point for the application.
 *
 * @author ctchinda
 * 
 * @version 2
 * @since 19.10.2022
 * @coauthor aneugebauer
 * 
 *           Changes: Removed Guice injection.
 */
public class PerfAlyzer {

	private static final Logger LOG = LoggerFactory.getLogger(PerfAlyzer.class);

	private final boolean doReportPreparation;
	private final boolean doNormalization;
	private final boolean doBinning;

	private final File unzippedDir;
	private final File normalizedDir;
	private final File binnedDir;
	private final File reportPreparationDir;
	private final File reportDir;

	private final WorkflowExecutor workflowExecutor;
	private final ReportCreator reportCreator;
	private final EmailReporter emailReporter;
	private final List<Marker> markers;

	public PerfAlyzer(@UnzippedDir final File unzippedDir,
			@BinnedDir final File binningDir,
			@NormalizedDir final File normalizedDir,
			@ReportPreparationDir final File reportPreparationDir,
			@ReportDir final File reportDir,
			@DoNormalization final boolean doNormalization,
			@DoBinning final boolean doBinning,
			@DoReportPreparation final boolean doReportPreparation,
			final WorkflowExecutor workflowExecutor,
			final ReportCreator reportCreator,
			@Nullable final EmailReporter emailReporter,
			final List<Marker> markers) {

		this.unzippedDir = unzippedDir;
		this.binnedDir = binningDir;
		this.normalizedDir = normalizedDir;
		this.reportPreparationDir = reportPreparationDir;
		this.reportDir = reportDir;

		this.doNormalization = doNormalization;
		this.doBinning = doBinning;
		this.doReportPreparation = doReportPreparation;

		this.workflowExecutor = workflowExecutor;
		this.reportCreator = reportCreator;
		this.emailReporter = emailReporter;

		this.markers = markers;
	}

	public static void main(final String[] args) {
		JCommander jCmd = null;
		try {
			Stopwatch stopwatch = Stopwatch.createStarted();
			LOG.info("Starting perfAlyzer...");

			LOG.info("Arguments:");
			for (String arg : args) {
				LOG.info(arg);
			}
			PerfAlyzerArgs perfAlyzerArgs = new PerfAlyzerArgs();

			jCmd = new JCommander(perfAlyzerArgs);
			jCmd.parse(args);

			PerfAlyzerFactory perfAlyzerFactory = new PerfAlyzerFactory(perfAlyzerArgs);
			PerfAlyzer perfAlyzer = perfAlyzerFactory.getPerfAlyzer();
			perfAlyzer.runPerfAlyzer();

			ExecutorService executorService = perfAlyzerFactory.getExecutorService();
			executorService.shutdownNow();

			stopwatch.stop();
			LOG.info("Done.");
			LOG.info("Total execution time: {}", stopwatch);
		} catch (ParameterException ex) {
			LOG.error(ex.getMessage());
			if (jCmd != null) {
				StringBuilder sb = new StringBuilder(200);
				jCmd.usage(sb);
				LOG.info(sb.toString());
			}
			System.exit(1);
		} catch (Exception ex) {
			LOG.error(ex.getMessage(), ex);
			System.exit(1);
		}
	}

	public void runPerfAlyzer() throws IOException {
		checkDirs();
		executeWorkflows();
		createReport();
	}

	private void checkDirs() throws IOException {
		if (doNormalization) {
			if (normalizedDir.isDirectory()) {
				LOG.info("Directory '{}' already exists. Deleting it...",
						normalizedDir);
				deleteDirectory(normalizedDir);
			}
		} else {
			checkState(
					normalizedDir.isDirectory(),
					"Normalization was turned off, but directory with normalized files does not exist or is not a directory: %s",
					normalizedDir);
		}

		if (doBinning) {
			if (binnedDir.isDirectory()) {
				LOG.info("Directory '{}' already exists. Deleting it...", binnedDir);
				deleteDirectory(binnedDir);
			}
		} else {
			checkState(
					binnedDir.isDirectory(),
					"Binning was turned off, but directory with binned files does not exist or is not a directory: %s",
					binnedDir);
		}

		if (doReportPreparation) {
			if (reportPreparationDir.isDirectory()) {
				LOG.info("Directory '{}' already exists. Deleting it...",
						reportPreparationDir);
				deleteDirectory(reportPreparationDir);
			}
		} else {
			checkState(
					reportPreparationDir.isDirectory(),
					"Report preparation was turned off, but directory with report preparation files does not exist or is not a directory: %s",
					reportPreparationDir);
		}

		if (reportDir.isDirectory()) {
			LOG.info("Directory '{}' already exists. Deleting it...", reportDir);
			deleteDirectory(reportDir);
		}
	}

	private void executeWorkflows() {
		LOG.info("Executing workflows...");

		if (doNormalization) {
			workflowExecutor.executeNormalizationTasks(unzippedDir, normalizedDir);
			extractFilesForMarkers();
		}

		if (doBinning) {
			workflowExecutor.executeBinningTasks(normalizedDir, binnedDir);
		}

		if (doReportPreparation) {
			workflowExecutor.executeReportPreparationTasks(binnedDir,
					reportPreparationDir);
		}
	}

	private void extractFilesForMarkers() {
		if (!markers.isEmpty()) {
			listPerfAlyzerFiles(normalizedDir)
					.stream()
					.filter(perfAlyzerFile -> {
						// GC logs cannot split up here and need to explicitly handle
						// markers later.
						// Load profiles contains the markers themselves and thus need
						// to be filtered out as well.
						String fileName = perfAlyzerFile.getFile().getName();
						return !fileName.contains("gclog") &
								!fileName.contains("[loadprofile]");
					})
					.forEach(perfAlyzerFile -> markers.forEach(marker -> {
						PerfAlyzerFile markerFile = perfAlyzerFile.copy();
						markerFile.setMarker(marker.getName());
						Path destPath = normalizedDir.toPath().resolve(markerFile.getFile().toPath());

						try (WritableByteChannel destChannel = newByteChannel(destPath, CREATE, WRITE)) {
							Path srcPath = normalizedDir.toPath().resolve(
									perfAlyzerFile.getFile().toPath());
							StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
							tokenizer.setDelimiterChar(';');
							try (Stream<String> lines = Files.lines(srcPath, UTF_8);) {
								lines
										.filter(line -> {
											try {
												tokenizer.reset(line);
												String timestampString = tokenizer.nextToken();
												long timestamp = Long.parseLong(timestampString);
												return marker.getLeftMillis() <= timestamp &&
														marker.getRightMillis() > timestamp;
											} catch (NumberFormatException ex) {
												LOG.error("Invalid data line: {}", line);
												return false;
											}
										})
										.forEach(
												line -> writeLineToChannel(destChannel, line, UTF_8));
							}
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}));
		}
	}

	private void createReport() throws IOException {
		LOG.info("Writing report...");
		reportCreator.createReport(listAllPerfAlyzerFiles(reportPreparationDir));

		// copy assets
		copyDirectoryToDirectory(new File("assets"), reportDir);

		if (emailReporter != null) {
			LOG.info("Creating e-mail report...");
			emailReporter.createAndSendReportMail();
		}
	}
}
