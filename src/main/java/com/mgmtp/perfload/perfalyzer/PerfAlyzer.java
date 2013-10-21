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
package com.mgmtp.perfload.perfalyzer;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.io.FileUtils.copyDirectoryToDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Stopwatch;
import com.google.inject.Guice;
import com.google.inject.Injector;
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
import com.mgmtp.perfload.perfalyzer.util.DirectoryLister;
import com.mgmtp.perfload.perfalyzer.workflow.WorkflowExecutor;

/**
 * The PerfAlyzer class is the entry point for the application.
 * 
 * @author ctchinda
 */
@Singleton
public class PerfAlyzer {

	private final static Logger LOG = LoggerFactory.getLogger(PerfAlyzer.class);

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

	@Inject
	public PerfAlyzer(@UnzippedDir final File unzippedDir, @BinnedDir final File binningDir,
			@NormalizedDir final File normalizedDir, @ReportPreparationDir final File reportPreparationDir,
			@ReportDir final File reportDir, @DoNormalization final boolean doNormalization,
			@DoBinning final boolean doBinning, @DoReportPreparation final boolean doReportPreparation,
			final WorkflowExecutor workflowExecutor, final ReportCreator reportCreator,
			@Nullable final EmailReporter emailReporter) {

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

	}

	public void runPerfAlyzer() throws IOException {
		checkDirs();
		executeWorkflows();
		createReport();
	}

	private void checkDirs() throws IOException {
		if (doNormalization) {
			if (normalizedDir.isDirectory()) {
				LOG.info("Directory '{}' already exists. Deleting it...", normalizedDir);
				deleteDirectory(normalizedDir);
			}
		} else {
			checkState(normalizedDir.isDirectory(),
					"Normalization was turned off, but directory with normalized files does not exist or is not a directory: %s",
					normalizedDir);
		}

		if (doBinning) {
			if (binnedDir.isDirectory()) {
				LOG.info("Directory '{}' already exists. Deleting it...", binnedDir);
				deleteDirectory(binnedDir);
			}
		} else {
			checkState(binnedDir.isDirectory(),
					"Binning was turned off, but directory with binned files does not exist or is not a directory: %s", binnedDir);
		}

		if (doReportPreparation) {
			if (reportPreparationDir.isDirectory()) {
				LOG.info("Directory '{}' already exists. Deleting it...", reportPreparationDir);
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

	private void executeWorkflows() throws IOException {
		LOG.info("Executing workflows...");

		if (doNormalization) {
			workflowExecutor.executeNormalizationTasks(unzippedDir, DirectoryLister.listFiles(unzippedDir), normalizedDir);
		}

		if (doBinning) {
			workflowExecutor.executeBinningTasks(normalizedDir, DirectoryLister.listPerfAlyzerFiles(normalizedDir), binnedDir);
		}

		if (doReportPreparation) {
			workflowExecutor.executeReportPreparationTasks(binnedDir, DirectoryLister.listPerfAlyzerFiles(binnedDir),
					reportPreparationDir);
		}
	}

	private void createReport() throws IOException {
		LOG.info("Writing report...");
		reportCreator.createReport(DirectoryLister.listFiles(reportPreparationDir));

		// copy assets
		copyDirectoryToDirectory(new File("assets"), reportDir);

		if (emailReporter != null) {
			LOG.info("Creating e-mail report...");
			emailReporter.createAndSendReportMail();
		}
	}

	public static void main(final String[] args) {
		JCommander jCmd = null;
		try {
			Stopwatch stopwatch = Stopwatch.createStarted();
			LOG.info("Starting perfAlyzer...");

			PerfAlyzerArgs perfAlyzerArgs = new PerfAlyzerArgs();

			jCmd = new JCommander(perfAlyzerArgs);
			jCmd.parse(args);

			Injector injector = Guice.createInjector(new PerfAlyzerModule(perfAlyzerArgs));
			PerfAlyzer perfAlyzer = injector.getInstance(PerfAlyzer.class);
			perfAlyzer.runPerfAlyzer();

			ExecutorService executorService = injector.getInstance(ExecutorService.class);
			executorService.shutdownNow();

			stopwatch.stop();
			LOG.info("Done.");
			LOG.info("Total execution time: {}", stopwatch);
		} catch (ParameterException ex) {
			LOG.error(ex.getMessage());
			StringBuilder sb = new StringBuilder(200);
			jCmd.usage(sb);
			LOG.info(sb.toString());
			System.exit(1);
		} catch (Exception ex) {
			LOG.error(ex.getMessage(), ex);
			System.exit(1);
		}
	}

}
