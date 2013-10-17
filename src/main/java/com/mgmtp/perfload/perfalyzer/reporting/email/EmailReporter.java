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
package com.mgmtp.perfload.perfalyzer.reporting.email;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newTreeMap;
import static com.google.common.io.Files.newReader;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static java.lang.Math.min;
import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.mail.DefaultMailerFactory;
import com.mgmtp.mail.Mail;
import com.mgmtp.mail.MailAddress;
import com.mgmtp.perfload.perfalyzer.annotations.EmailFrom;
import com.mgmtp.perfload.perfalyzer.annotations.EmailTo;
import com.mgmtp.perfload.perfalyzer.annotations.MaxEmailHistoryItems;
import com.mgmtp.perfload.perfalyzer.annotations.RelativeDestDir;
import com.mgmtp.perfload.perfalyzer.annotations.ReportPreparationDir;
import com.mgmtp.perfload.perfalyzer.annotations.ReportsBaseUrl;
import com.mgmtp.perfload.perfalyzer.annotations.SmtpProps;
import com.mgmtp.perfload.perfalyzer.annotations.SubjectProps;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerUtils;
import com.mgmtp.perfload.perfalyzer.util.PlaceholderUtils;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;

/**
 * Creates an e-mail report.
 * 
 * @author rnaegele
 */
@Singleton
public class EmailReporter {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final TestMetadata testMetadata;
	private final ResourceBundle resourceBundle;
	private final Locale locale;
	private final String reportsBaseUrl;
	private final File destDir;
	private final String fromAddress;
	private final List<String> toAddresses;
	private final Properties smtpProps;
	private final Properties subjectProps;
	private final File soureDir;
	private final Charset charset;
	private final int maxHistoryItems;
	private final Map<String, List<Pattern>> reportContentsConfigMap;

	@Inject
	public EmailReporter(final TestMetadata testMetadata, @ReportPreparationDir final File soureDir, final Charset charset,
			final ResourceBundle resourceBundle, final Locale locale, @Nullable @ReportsBaseUrl final String reportsBaseUrl,
			@RelativeDestDir final File destDir, @EmailFrom final String fromAddress, @EmailTo final List<String> toAddresses,
			@SmtpProps final Properties smtpProps, @SubjectProps final Properties subjectProps,
			@MaxEmailHistoryItems final int maxHistoryItems, final Map<String, List<Pattern>> reportContentsConfigMap) {
		this.testMetadata = testMetadata;
		this.soureDir = soureDir;
		this.charset = charset;
		this.resourceBundle = resourceBundle;
		this.locale = locale;
		this.reportsBaseUrl = reportsBaseUrl;
		this.destDir = destDir;
		this.fromAddress = fromAddress;
		this.toAddresses = toAddresses;
		this.smtpProps = smtpProps;
		this.subjectProps = subjectProps;
		this.maxHistoryItems = maxHistoryItems;
		this.reportContentsConfigMap = reportContentsConfigMap;
	}

	public void createAndSendReportMail() throws IOException {
		log.info("Creating e-mail report...");

		List<? extends List<String>> data = loadData(new File(soureDir, "global/[measuring][executions].csv"));
		Map<String, List<? extends List<String>>> comparisonData = newTreeMap();

		if (maxHistoryItems > 0) {
			File[] files = new File(soureDir, "comparison").listFiles();
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
					String operation = PerfAlyzerFile.create(file).getFileNameParts().get(1);
					List<? extends List<String>> comparisonDataList = loadData(file);

					// apply max restriction, add 1 for header
					comparisonDataList = comparisonDataList.subList(0, min(maxHistoryItems + 1, comparisonDataList.size()));
					comparisonData.put(operation, comparisonDataList);
				}
			}
		}

		String link;
		if (reportsBaseUrl != null) {
			link = reportsBaseUrl + normalize(new File(destDir, "05_report/report.html").getPath(), true);
		} else {
			link = null;
			log.warn("'reportsBaseUrl' not configured. E-mail will have no link to report.");
		}

		EmailSkeleton skeleton = new EmailSkeleton(testMetadata, resourceBundle, locale, data, comparisonData, link);
		StringWriter sw = new StringWriter(1000);
		skeleton.write(sw);

		Mail.Builder builder = new Mail.Builder(new MailAddress(fromAddress), new MailAddress(toAddresses.get(0)));
		for (int i = 1; i < toAddresses.size(); ++i) {
			builder.addTo(toAddresses.get(i));
		}

		builder.setHtmlBody(sw.toString());

		String testName = removeExtension(testMetadata.getTestPlanFile());
		String subjectTemplate = subjectProps.getProperty(testName);
		if (isNotBlank(subjectTemplate)) {
			Map<String, String> replacements = PerfAlyzerUtils.readAggregatedMap(new File(soureDir,
					"global/[measuring][executions].csv"), charset);
			replacements.put("test.name", testName);

			String subject = PlaceholderUtils.resolvePlaceholders(subjectTemplate, replacements);
			builder.setSubject(subject);
		} else {
			builder.setSubject("perfLoad E-mail Report");
		}

		Mail mail = builder.build();

		log.info("Sending mail...");
		DefaultMailerFactory.get().create(smtpProps).send(mail);
	}

	private List<? extends List<String>> loadData(final File file) throws IOException {
		try (BufferedReader br = newReader(file, charset)) {
			List<List<String>> rows = newArrayList();
			StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
			tokenizer.setDelimiterChar(DELIMITER);

			for (String line = null; (line = br.readLine()) != null;) {
				tokenizer.reset(line);
				List<String> tokenList = tokenizer.getTokenList();
				rows.add(tokenList);
			}

			return rows;
		}
	}
}
