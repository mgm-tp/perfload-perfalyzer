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
package com.mgmtp.perfload.perfalyzer;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.Message;
import com.google.inject.util.Providers;
import com.mgmtp.perfload.perfalyzer.annotations.BinnedDir;
import com.mgmtp.perfload.perfalyzer.annotations.DoBinning;
import com.mgmtp.perfload.perfalyzer.annotations.DoNormalization;
import com.mgmtp.perfload.perfalyzer.annotations.DoReportPreparation;
import com.mgmtp.perfload.perfalyzer.annotations.EmailFrom;
import com.mgmtp.perfload.perfalyzer.annotations.EmailTo;
import com.mgmtp.perfload.perfalyzer.annotations.FloatFormat;
import com.mgmtp.perfload.perfalyzer.annotations.IntFormat;
import com.mgmtp.perfload.perfalyzer.annotations.MaxEmailHistoryItems;
import com.mgmtp.perfload.perfalyzer.annotations.MaxHistoryItems;
import com.mgmtp.perfload.perfalyzer.annotations.NormalizedDir;
import com.mgmtp.perfload.perfalyzer.annotations.RelativeDestDir;
import com.mgmtp.perfload.perfalyzer.annotations.ReportDir;
import com.mgmtp.perfload.perfalyzer.annotations.ReportPreparationDir;
import com.mgmtp.perfload.perfalyzer.annotations.ReportTabNames;
import com.mgmtp.perfload.perfalyzer.annotations.ReportsBaseUrl;
import com.mgmtp.perfload.perfalyzer.annotations.SmtpProps;
import com.mgmtp.perfload.perfalyzer.annotations.SubjectProps;
import com.mgmtp.perfload.perfalyzer.annotations.UnzippedDir;
import com.mgmtp.perfload.perfalyzer.annotations.WarmUpSeconds;
import com.mgmtp.perfload.perfalyzer.reporting.ReportCreator;
import com.mgmtp.perfload.perfalyzer.reporting.email.EmailReporter;
import com.mgmtp.perfload.perfalyzer.reportpreparation.DisplayData;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator;
import com.mgmtp.perfload.perfalyzer.util.ArchiveExtracter;
import com.mgmtp.perfload.perfalyzer.util.Marker;
import com.mgmtp.perfload.perfalyzer.util.MarkersReader;
import com.mgmtp.perfload.perfalyzer.util.MemoryFormat;
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
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.beust.jcommander.internal.Lists.newArrayList;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.mgmtp.perfload.perfalyzer.util.PropertiesUtils.loadIntoProperties;
import static com.mgmtp.perfload.perfalyzer.util.PropertiesUtils.loadProperties;
import static com.mgmtp.perfload.perfalyzer.util.PropertiesUtils.saveProperties;
import static com.mgmtp.perfload.perfalyzer.util.PropertiesUtils.setIfNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;

/**
 * @author rnaegele
 */
public class PerfAlyzerModule extends AbstractModule {
	public static final Pattern INPUT_DIR_PATTERN = Pattern.compile("(\\d{8}-\\d{4})_(.*)");

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final PerfAlyzerArgs args;

	public PerfAlyzerModule(final PerfAlyzerArgs args) {
		checkState(!args.unzip || args.inputDir.isDirectory(), "'inputDir' does not exist or is not a directory: %s", args.inputDir);
		this.args = args;
	}

	@SuppressWarnings("unchecked")
	private static <T> T get(final ConfigObject configObject, final String key) {
		return (T) configObject.get(key);
	}

	@Override
	protected void configure() {
		binder().requireExplicitBindings();

		bindConstant().annotatedWith(DoNormalization.class).to(args.normalization);
		bindConstant().annotatedWith(DoBinning.class).to(args.binning);
		bindConstant().annotatedWith(DoReportPreparation.class).to(args.reportPreparation);

		String inputDirName = args.inputDir.getName();
		Matcher matcher = INPUT_DIR_PATTERN.matcher(inputDirName);
		checkState(matcher.matches(), "Input dir did not match pattern %s", INPUT_DIR_PATTERN);

		String timestamp = matcher.group(1);
		String testName = matcher.group(2);
		File destDir = new File(new File(args.outputDir, testName), timestamp);

		File unzippedDir = new File(destDir, "01_unzipped");

		bind(File.class).annotatedWith(RelativeDestDir.class).toInstance(new File(testName, timestamp));
		bind(File.class).annotatedWith(UnzippedDir.class).toInstance(unzippedDir);
		bind(File.class).annotatedWith(NormalizedDir.class).toInstance(new File(destDir, "02_normalized"));
		bind(File.class).annotatedWith(BinnedDir.class).toInstance(new File(destDir, "03_binned"));
		bind(File.class).annotatedWith(ReportPreparationDir.class).toInstance(new File(destDir, "04_reportpreparation"));
		bind(File.class).annotatedWith(ReportDir.class).toInstance(new File(destDir, "05_report"));

		if (args.unzip) {
			try {
				if (unzippedDir.isDirectory()) {
					log.info("Directory '{}' already exists. Deleting it...", unzippedDir);
					deleteDirectory(unzippedDir);
				}
				log.info("Extracting result archives...");
				ArchiveExtracter archiveExtracter = new ArchiveExtracter(args.inputDir, unzippedDir);
				archiveExtracter.extract();
			} catch (IOException ex) {
				Throwables.propagate(ex);
			}
		} else {
			checkState(unzippedDir.isDirectory(),
					"Unzipping was turned off, but directory with unzipped files does not exist or is not a directory: %s",
					unzippedDir);
		}

		createBindingsFromConfigFile(args.outputDir);
		bindTestMetadata(unzippedDir);

		Multibinder<Workflow> workflowBinder = Multibinder.newSetBinder(binder(), Workflow.class);
		workflowBinder.addBinding().to(PerfMonWorkflow.class);
		workflowBinder.addBinding().to(MeasuringWorkflow.class);
		workflowBinder.addBinding().to(GcLogWorkflow.class);
		workflowBinder.addBinding().to(LoadProfileWorkflow.class);

		bind(WorkflowExecutor.class);
		bind(PerfAlyzer.class);
		bind(ReportCreator.class);
		bind(ResourceBundle.class).toProvider(ResourceBundleProvider.class);
		bind(PlotCreator.class);
		bind(MemoryFormat.class);
	}

	private void bindTestMetadata(final File unzippedDir) {
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
			// args override
			setIfNonNull(perfLoadMetaProps, "test.start", args.testStartDate);
			setIfNonNull(perfLoadMetaProps, "test.finish", args.testFinishDate);
			setIfNonNull(perfLoadMetaProps, "test.comment", args.testComment);
			setIfNonNull(perfLoadMetaProps, "operations", args.operations);

			TestMetadata testMetadata = TestMetadata.create(args.inputDir.getName(), perfLoadMetaProps);
			bind(TestMetadata.class).toInstance(testMetadata);
		} catch (IOException ex) {
			log.error("Error reading test meta properties: " + metaPropsFile, ex);
			Throwables.propagate(ex);
		}
	}

	private void createBindingsFromConfigFile(final File destDir) {
		File configFile = new File("config", "PerfAlyzerConfig.groovy");
		if (!configFile.exists()) {
			log.info("Config file '{}' does not exist. Using default config file.", configFile);
			configFile = new File("config", "PerfAlyzerConfig_Default.groovy");
		}
		try {
			log.info("Loading perfAlyzer config file...");
			ConfigSlurper slurper = new ConfigSlurper();
			ConfigObject configObject = slurper.parse(configFile.toURI().toURL());

			// wrapping into a provider caters for null
			String url = get(configObject, "reportsBaseUrl");
			bind(String.class).annotatedWith(ReportsBaseUrl.class).toProvider(Providers.of(url));

			Integer warmUpSeconds = get(configObject, "warmUpSeconds");
			bindConstant().annotatedWith(WarmUpSeconds.class).to(warmUpSeconds);

			Integer maxHistoryItems = get(configObject, "maxHistoryItems");
			bindConstant().annotatedWith(MaxHistoryItems.class).to(maxHistoryItems);

			/***** email *****/
			ConfigObject emailConfig = get(configObject, "email");
			Boolean flag = get(emailConfig, "enabled");
			if (flag) {
				bindConstant().annotatedWith(EmailFrom.class).to((String) emailConfig.get("from"));

				List<String> toList = get(emailConfig, "to");
				bind(new TypeLiteral<List<String>>() {
					//
				}).annotatedWith(EmailTo.class).toInstance(toList);

				ConfigObject smtpConfig = get(emailConfig, "smtp");

				Boolean auth = (Boolean) smtpConfig.get("auth");
				if (auth != null && auth) {
					final String username = (String) smtpConfig.get("username");
					final String password = (String) smtpConfig.get("password");
					bind(Authenticator.class).toInstance(new Authenticator() {
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(username, password);
						}
					});
				}
				Boolean ssl = (Boolean) smtpConfig.remove("ssl");
				String prefix;
				String protocol;
				if (ssl != null && ssl) {
					protocol = "smtps";
					prefix = "mail.smtps";
				} else {
					protocol = "smtp";
					prefix = "mail.smtp";
				}

				Properties smtpProps = smtpConfig.toProperties(prefix);
				smtpProps.setProperty("mail.transport.protocol", protocol);
				bind(Properties.class).annotatedWith(SmtpProps.class).toInstance(smtpProps);

				ConfigObject subjectConfig = get(emailConfig, "subjects");
				Properties subjectProps = subjectConfig != null ? subjectConfig.toProperties() : new Properties();
				bind(Properties.class).annotatedWith(SubjectProps.class).toInstance(subjectProps);

				Integer maxEmailHistoryItems = get(emailConfig, "maxHistoryItems");
				if (maxEmailHistoryItems == null) {
					maxEmailHistoryItems = maxHistoryItems;
				} else {
					checkState(maxEmailHistoryItems <= maxHistoryItems,
							"Max. history items in e-mail cannot be greater than global max history items");
				}
				bindConstant().annotatedWith(MaxEmailHistoryItems.class).to(maxEmailHistoryItems);

				bind(EmailReporter.class);
			} else {
				bind(EmailReporter.class).toProvider(Providers.<EmailReporter>of(null));
			}

			/***** thread count *****/
			Integer threadCount = get(configObject, "threadCount");
			bind(ExecutorService.class).toInstance(Executors.newFixedThreadPool(threadCount));

			/***** locale *****/
			String localeString = get(configObject, "locale");
			File localPropsFile = new File(destDir, ".config");
			Properties localProps = new Properties();
			if (localPropsFile.exists()) {
				loadIntoProperties(localPropsFile, localProps);
				String originalLocaleString = localProps.getProperty("locale");
				if (!originalLocaleString.equals(localeString)) {
					log.warn(
							"Configured locale ({}) has changed but is ignored for compatibility reasons with comparison data. Locale used: {}",
							localeString, originalLocaleString);
				}
				localeString = originalLocaleString;
			} else {
				localProps.setProperty("locale", localeString);
				saveProperties(localPropsFile, localProps);
			}
			final Locale locale = new Locale(localeString);
			bind(Locale.class).toInstance(locale);

			/***** reports contents configuration *****/
			Map<String, List<Pattern>> reportContentsConfigMap = get(configObject, "reportContents");
			bind(new TypeLiteral<Map<String, List<Pattern>>>() {
				//
			}).toInstance(reportContentsConfigMap);

			/***** display data *****/
			Map<String, Map<String, Object>> displayDataMap = get(configObject, "formats");

			List<DisplayData> displayDataList = newArrayListWithCapacity(displayDataMap.size());
			for (Map<String, Object> map : displayDataMap.values()) {
				Pattern pattern = (Pattern) map.get("pattern");
				String unitX = (String) map.get("unitX");
				@SuppressWarnings("unchecked")
				List<String> unitYList = (List<String>) map.get("unitY");
				DisplayData dd = new DisplayData(pattern, unitX, unitYList);
				displayDataList.add(dd);
			}

			bind(new TypeLiteral<List<DisplayData>>() {
				//
			}).toInstance(displayDataList);
		} catch (IOException ex) {
			addError(new Message(ImmutableList.<Object>of(configFile), "Error reading config file: " + configFile, ex));
		}
	}

	@Provides
	@IntFormat
	NumberFormat provideIntFormat(final Locale locale) {
		NumberFormat nf = NumberFormat.getIntegerInstance(locale);
		nf.setGroupingUsed(false);
		nf.setRoundingMode(RoundingMode.HALF_UP);
		return nf;
	}

	@Provides
	@FloatFormat
	NumberFormat provideFloatFormat(final Locale locale) {
		DecimalFormatSymbols dfs = new DecimalFormatSymbols(locale);
		NumberFormat nf = new DecimalFormat("0.00", dfs);
		nf.setGroupingUsed(false);
		nf.setRoundingMode(RoundingMode.HALF_UP);
		return nf;
	}

	@Provides
	@Singleton
	Control provideResourceBundleControl() {
		return new Utf8Control(new File("strings"));
	}

	@Provides
	@Singleton
	TimestampNormalizer provideTimestampNormalizer(final TestMetadata testMetadata, @WarmUpSeconds final int warmUpSeconds) {
		ZonedDateTime testStartDate = testMetadata.getTestStart();
		ZonedDateTime testEndDate = testMetadata.getTestEnd();
		return new TimestampNormalizer(testStartDate, testEndDate, warmUpSeconds);
	}

	@Provides
	@Singleton
	List<Marker> provideMarkers(@UnzippedDir final File unzippedDir, final TestMetadata testMetadata) throws IOException {
		log.info("Loading markers from load profile...");
		File loadProfileFile = getOnlyElement(listFiles(new File(unzippedDir, "console/console-logs"), suffixFileFilter(".perfload"), null));
		MarkersReader markerReader = new MarkersReader(loadProfileFile, testMetadata.getTestStart());
		return markerReader.readMarkers();
	}

	@Provides
	@Singleton
	@ReportTabNames
	List<String> provideReportTabNames(final List<Marker> markers) {
		List<String> result = newArrayList("Overall");
		result.addAll(markers.stream().map(Marker::getName).collect(toList()));
		return result;
	}
}
