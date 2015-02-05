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
package com.mgmtp.perfload.perfalyzer.reporting.email;

import com.google.common.base.Charsets;
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
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newTreeMap;
import static com.google.common.io.Files.newReader;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
	private final Authenticator authenticator;
	private final int maxHistoryItems;
	private final Map<String, List<Pattern>> reportContentsConfigMap;

	@Inject
	public EmailReporter(final TestMetadata testMetadata, @ReportPreparationDir final File soureDir, final ResourceBundle resourceBundle,
			final Locale locale, @Nullable @ReportsBaseUrl final String reportsBaseUrl, @RelativeDestDir final File destDir,
			@EmailFrom final String fromAddress, @EmailTo final List<String> toAddresses, @SmtpProps final Properties smtpProps,
			@SubjectProps final Properties subjectProps, @Nullable final Authenticator authenticator,
			@MaxEmailHistoryItems final int maxHistoryItems, final Map<String, List<Pattern>> reportContentsConfigMap) {
		this.testMetadata = testMetadata;
		this.soureDir = soureDir;
		this.resourceBundle = resourceBundle;
		this.locale = locale;
		this.reportsBaseUrl = reportsBaseUrl;
		this.destDir = destDir;
		this.fromAddress = fromAddress;
		this.toAddresses = toAddresses;
		this.smtpProps = smtpProps;
		this.subjectProps = subjectProps;
		this.authenticator = authenticator;
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

		String content = sw.toString();

		String subject;
		String testName = removeExtension(testMetadata.getTestPlanFile());
		String subjectTemplate = subjectProps.getProperty(testName);
		if (isNotBlank(subjectTemplate)) {
			Map<String, String> replacements = PerfAlyzerUtils.readAggregatedMap(new File(soureDir,
					"global/[measuring][executions].csv"), Charsets.UTF_8);
			replacements.put("test.name", testName);

			subject = PlaceholderUtils.resolvePlaceholders(subjectTemplate, replacements);
		} else {
			subject = "perfLoad E-mail Report";
		}

		sendMessage(subject, content);
	}

	private void sendMessage(final String subject, final String content) {
		try {
			Session session = (authenticator != null) ? Session.getInstance(smtpProps, authenticator) : Session.getInstance(smtpProps);

			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(fromAddress));
			msg.setSubject(subject);
			msg.addRecipients(Message.RecipientType.TO, on(',').join(toAddresses));
			msg.setText(content, UTF_8.name(), "html");

			Transport.send(msg);
		} catch (MessagingException e) {
			log.error("Error while creating report e-mail", e);
		}
	}

	private List<? extends List<String>> loadData(final File file) throws IOException {
		try (BufferedReader br = newReader(file, Charsets.UTF_8)) {
			List<List<String>> rows = newArrayList();
			StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
			tokenizer.setDelimiterChar(DELIMITER);

			for (String line; (line = br.readLine()) != null; ) {
				tokenizer.reset(line);
				List<String> tokenList = tokenizer.getTokenList();
				rows.add(tokenList);
			}

			return rows;
		}
	}
}
