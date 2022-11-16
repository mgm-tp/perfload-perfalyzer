/*
 * Copyright (c) 2022 mgm technology partners GmbH
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
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.mgmtp.perfload.perfalyzer.util.PropertiesUtils.loadIntoProperties;
import static com.mgmtp.perfload.perfalyzer.util.PropertiesUtils.loadProperties;
import static com.mgmtp.perfload.perfalyzer.util.PropertiesUtils.saveProperties;
import static com.mgmtp.perfload.perfalyzer.util.PropertiesUtils.setIfNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.mgmtp.perfload.perfalyzer.reporting.ReportCreator;
import com.mgmtp.perfload.perfalyzer.reporting.email.EmailReporter;
import com.mgmtp.perfload.perfalyzer.reportpreparation.DisplayData;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator;
import com.mgmtp.perfload.perfalyzer.util.ArchiveExtracter;
import com.mgmtp.perfload.perfalyzer.util.Marker;
import com.mgmtp.perfload.perfalyzer.util.MarkersReader;
import com.mgmtp.perfload.perfalyzer.util.ResourceBundleProvider;
import com.mgmtp.perfload.perfalyzer.util.ResourceBundleProvider.Utf8Control;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;
import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;
import com.mgmtp.perfload.perfalyzer.workflow.GcLogWorkflow;
import com.mgmtp.perfload.perfalyzer.workflow.LoadProfileWorkflow;
import com.mgmtp.perfload.perfalyzer.workflow.MeasuringWorkflow;
import com.mgmtp.perfload.perfalyzer.workflow.PerfMonWorkflow;
import com.mgmtp.perfload.perfalyzer.workflow.Workflow;
import com.mgmtp.perfload.perfalyzer.workflow.WorkflowExecutor;
import com.mgmtp.perfload.perfalyzer.util.NumberFormatProvider;
import com.mgmtp.perfload.perfalyzer.util.MemoryFormatProvider;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

/**
 * Adaptation of PerfAlyzerModule but without the use of Guice.
 * 
 * @author aneugebauer
 */

class PerfAlyzerFactory {
	private class LocalConfigObject {
		Integer warmUpSeconds;
		Integer maxEmailHistoryItems;
		EmailReporter emailReporter;
		ExecutorService executorService;
		Map<String, List<Pattern>> reportContentsConfigMap;
		List<DisplayData> displayDataList;
		Locale locale;
	}

	public static final Pattern INPUT_DIR_PATTERN = Pattern.compile("(\\d{8}-\\d{4})_(.*)");

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final PerfAlyzerArgs args;

	private ExecutorService executorService = null;

	public PerfAlyzerFactory(final PerfAlyzerArgs args) {
		checkState(!args.unzip || args.inputDir.isDirectory(),
				"'inputDir' does not exist or is not a directory: %s",
				args.inputDir);
		this.args = args;
	}

	@SuppressWarnings("unchecked")
	private static <T> T get(final ConfigObject configObject, final String key) {
		return (T) configObject.get(key);
	}

	public PerfAlyzer getPerfAlyzer() {

		boolean doNormalization = args.normalization;
		boolean doBinning = args.binning;
		boolean doReportPreparation = args.reportPreparation;

		String inputDirName = args.inputDir.getName();
		Matcher matcher = INPUT_DIR_PATTERN.matcher(inputDirName);
		checkState(matcher.matches(), "Input dir did not match pattern %s",
				INPUT_DIR_PATTERN);

		String timestamp = matcher.group(1);
		String testName = matcher.group(2);
		File destDir = new File(new File(args.outputDir, testName), timestamp);

		File unzippedDir = new File(destDir, "01_unzipped");
		File relativeDestDir = new File(testName, timestamp);
		File normalizedDir = new File(destDir, "02_normalized");
		File binnedDir = new File(destDir, "03_binned");
		File reportPreparationDir = new File(destDir, "04_reportpreparation");
		File reportDir = new File(destDir, "05_report");

		if (args.unzip) {
			try {
				if (unzippedDir.isDirectory()) {
					log.info("Directory '{}' already exists. Deleting it...",
							unzippedDir);
					deleteDirectory(unzippedDir);
				}
				log.info("Extracting result archives...");
				ArchiveExtracter archiveExtracter = new ArchiveExtracter(args.inputDir, unzippedDir);
				archiveExtracter.extract();
			} catch (IOException ex) {
				Throwables.propagate(ex);
			}
		} else {
			checkState(
					unzippedDir.isDirectory(),
					"Unzipping was turned off, but directory with unzipped files does not exist or is not a directory: %s",
					unzippedDir);
		}

		final TestMetadata testMetadata = createTestMetadata(unzippedDir);
		LocalConfigObject configObject = createObjectsFromConfigFile(args.outputDir, testMetadata, reportPreparationDir,
				relativeDestDir);

		final TimestampNormalizer timestampNormalizer = new TimestampNormalizer(testMetadata.getTestStart(),
				testMetadata.getTestEnd(), configObject.warmUpSeconds);
		final NumberFormatProvider intProvider = new NumberFormatProvider(configObject.locale, true);
		final ResourceBundleProvider resourceBundleProvider = new ResourceBundleProvider(configObject.locale,
				new Utf8Control(new File("strings")));
		List<Marker> markers = provideMarkers(unzippedDir, testMetadata);
		PlotCreator plotCreator = new PlotCreator(
				intProvider.get(), resourceBundleProvider.get(), configObject.displayDataList, markers);

		PerfMonWorkflow perfMonWorkflow = new PerfMonWorkflow(
				timestampNormalizer, intProvider, intProvider, configObject.displayDataList,
				resourceBundleProvider.get(), plotCreator, testMetadata);

		List<String> reportTabNames = provideReportTabNames(markers);
		ReportCreator reporter = new ReportCreator(
				testMetadata, reportPreparationDir, reportDir, configObject.reportContentsConfigMap,
				resourceBundleProvider.get(), configObject.locale, reportTabNames);

		final NumberFormatProvider floatProvider = new NumberFormatProvider(configObject.locale, false);
		final MemoryFormatProvider memoryFormatProvider = new MemoryFormatProvider(configObject.locale);

		MeasuringWorkflow measuringWorkflow = new MeasuringWorkflow(
				timestampNormalizer, intProvider, floatProvider, configObject.displayDataList,
				resourceBundleProvider.get(), plotCreator, testMetadata, configObject.maxEmailHistoryItems);
		GcLogWorkflow gcLogWorkflow = new GcLogWorkflow(timestampNormalizer, intProvider, floatProvider,
				configObject.displayDataList, resourceBundleProvider.get(), plotCreator,
				testMetadata, memoryFormatProvider);
		LoadProfileWorkflow loadProfileWorkflow = new LoadProfileWorkflow(intProvider, intProvider,
				configObject.displayDataList, resourceBundleProvider.get(), plotCreator, testMetadata);

		// Create sets out of workflows
		Set<Workflow> workflows = new HashSet<Workflow>();
		workflows.add(measuringWorkflow);
		workflows.add(gcLogWorkflow);
		workflows.add(perfMonWorkflow);
		workflows.add(loadProfileWorkflow);

		final WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflows, configObject.executorService,
				markers);

		PerfAlyzer perfAlyzer = new PerfAlyzer(unzippedDir, binnedDir, normalizedDir,
				reportPreparationDir, reportDir, doNormalization,
				doBinning, doReportPreparation, workflowExecutor, reporter, configObject.emailReporter,
				markers);
		return perfAlyzer;
	}

	private TestMetadata createTestMetadata(final File unzippedDir) {
		File metaPropsFile = new File(unzippedDir, "console/console-logs/perfload.meta.utf8.props");
		try {
			Properties perfLoadMetaProps;
			if (metaPropsFile.exists()) {
				log.info("Loading test meta properties...");
				perfLoadMetaProps = loadProperties(metaPropsFile);
			} else {
				log.warn("Testplan properties file does not exist: {}", metaPropsFile);
				perfLoadMetaProps = new Properties();
			}
			setIfNonNull(perfLoadMetaProps, "test.start", args.testStartDate);
			setIfNonNull(perfLoadMetaProps, "test.finish", args.testFinishDate);
			setIfNonNull(perfLoadMetaProps, "test.comment", args.testComment);
			setIfNonNull(perfLoadMetaProps, "operations", args.operations);

			TestMetadata testMetadata = TestMetadata.create(args.inputDir.getName(), perfLoadMetaProps);
			return testMetadata;
		} catch (IOException ex) {
			log.error("Error reading test meta properties: " + metaPropsFile, ex);
			Throwables.propagate(ex);
		}
		return null;
	}

	private LocalConfigObject createObjectsFromConfigFile(final File destDir, final TestMetadata testMetadata,
			File reportPreparationDir, File relativeDestDir) {
		File configFile = new File("config", "PerfAlyzerConfig.groovy");
		if (!configFile.exists()) {
			log.info("Config file '{}' does not exist. Using default config file.",
					configFile);
			configFile = new File("config", "PerfAlyzerConfig_Default.groovy");
		}
		LocalConfigObject localConfigObject = new LocalConfigObject();
		try {
			log.info("Loading parfAlyzer config file...");
			ConfigSlurper slurper = new ConfigSlurper();
			ConfigObject slurpConfigObject = slurper.parse(configFile.toURI().toURL());

			String url = get(slurpConfigObject, "reportsBaseUrl");

			Integer warmUpSeconds = get(slurpConfigObject, "warmUpSeconds");

			Integer maxHistoryItems = get(slurpConfigObject, "maxHistoryItems");
			// Store default for maxEmailHistoryItems
			// Will be overritten later when needed, otherwiese it was not defined.
			localConfigObject.maxEmailHistoryItems = maxHistoryItems;

			/***** locale *****/
			String localeString = get(slurpConfigObject, "locale");
			File localPropsFile = new File(destDir, ".config");
			Properties localProps = new Properties();
			if (localPropsFile.exists()) {
				loadIntoProperties(localPropsFile, localProps);
				String originalLocalString = localProps.getProperty("locale");
				if (!originalLocalString.equals(localeString)) {
					log.warn(
							"Configured locale ({}) ahs changed but is ignored for compatibility reasons with comparison data. Locale used: {}",
							localeString, originalLocalString);
				}
				localeString = originalLocalString;
			} else {
				localProps.setProperty("locale", localeString);
				saveProperties(localPropsFile, localProps);
			}
			final Locale locale = new Locale(localeString);
			localConfigObject.locale = locale;

			/***** reports contents configuration *****/
			Map<String, List<Pattern>> reportContentsConfigMap = get(slurpConfigObject, "reportContents");

			/***** email *****/
			final ConfigObject emailConfig = get(slurpConfigObject, "email");
			Boolean flag = get(emailConfig, "enabled");
			if (flag) {
				String emailFrom = (String) emailConfig.get("from");

				List<String> toList = get(emailConfig, "to");

				ConfigObject smtpConfig = get(emailConfig, "smtp");

				Boolean auth = (Boolean) smtpConfig.get("auth");
				Authenticator passwordAuthentication = null;
				if (auth != null && auth) {
					final String username = (String) smtpConfig.get("username");
					final String password = (String) smtpConfig.get("password");
					passwordAuthentication = new Authenticator() {
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(username, password);
						}
					};
				}
				Boolean ssl = (Boolean) smtpConfig.remove("ssl");
				String prefix;
				String protocol;
				if (ssl != null && ssl) {
					protocol = "smtp";
					prefix = "mail.smtp";
				} else {
					protocol = "smtp";
					prefix = "mail.smtp";
				}

				Properties smtpProps = smtpConfig.toProperties(prefix);
				smtpProps.setProperty("mail.transport.protocol", protocol);

				ConfigObject subjectConfig = get(emailConfig, "subjects");
				Properties subjectProps = subjectConfig != null
						? subjectConfig.toProperties()
						: new Properties();

				Integer maxEmailHistoryItems = get(emailConfig, "maxHistoryItems");
				if (maxEmailHistoryItems == null) {
					maxEmailHistoryItems = maxHistoryItems;
				} else {
					checkState(
							maxEmailHistoryItems <= maxHistoryItems,
							"Max. history items in e-mail cannot be greater than global max history items");
				}
				localConfigObject.maxEmailHistoryItems = maxEmailHistoryItems;
				ResourceBundleProvider localResourceBundleProvider = new ResourceBundleProvider(locale,
						new Utf8Control(new File("string")));
				EmailReporter emailReporter = new EmailReporter(
						testMetadata, reportPreparationDir,
						localResourceBundleProvider.get(), locale, url, relativeDestDir,
						emailFrom, toList, smtpProps, subjectProps, passwordAuthentication,
						maxEmailHistoryItems, reportContentsConfigMap);
				localConfigObject.emailReporter = emailReporter;
			}

			/***** thread count *****/
			Integer threadCount = get(slurpConfigObject, "threadCount");
			executorService = Executors.newFixedThreadPool(threadCount);
			localConfigObject.executorService = executorService;

			/***** display data *****/
			Map<String, Map<String, Object>> displayDataMap = get(slurpConfigObject, "formats");

			final List<DisplayData> displayDataList = newArrayListWithCapacity(displayDataMap.size());
			for (Map<String, Object> map : displayDataMap.values()) {
				Pattern pattern = (Pattern) map.get("pattern");
				String unitX = (String) map.get("unitX");
				@SuppressWarnings("unchecked")
				List<String> unitYList = (List<String>) map.get("unitY");
				DisplayData dd = new DisplayData(pattern, unitX, unitYList);
				displayDataList.add(dd);
			}
			localConfigObject.warmUpSeconds = warmUpSeconds;
			localConfigObject.reportContentsConfigMap = reportContentsConfigMap;
			localConfigObject.displayDataList = displayDataList;
		} catch (IOException io) {
			log.error("Error creating objects for perfAlyzer object creation.", io);
			Throwables.propagate(io);
		}
		return localConfigObject;
	}

	private List<Marker> provideMarkers(final File unzippedDir,
			final TestMetadata testMetadata) {
		log.info("Loading markers from load profile...");
		File loadProfileFile = getOnlyElement(listFiles(new File(unzippedDir, "console/console-logs"),
				suffixFileFilter(".perfload"), null));
		MarkersReader markerReader = new MarkersReader(loadProfileFile, testMetadata.getTestStart());
		try {
			return markerReader.readMarkers();
		} catch (IOException io) {
			log.error("Error reading markers.", io);
			Throwables.propagate(io);
		}
		return null;
	}

	private List<String> provideReportTabNames(final List<Marker> markers) {
		List<String> result = newArrayList("Overall");
		result.addAll(markers.stream().map(Marker::getName).collect(toList()));
		return result;
	}

	public ExecutorService getExecutorService() {
		if (executorService == null) {
			throw new NullPointerException("The executorService is not yet initialized.");
		}
		return executorService;
	}
}