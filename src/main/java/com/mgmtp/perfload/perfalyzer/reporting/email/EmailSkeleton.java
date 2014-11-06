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
import com.google.common.io.Resources;
import com.googlecode.jatl.HtmlWriter;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import static com.google.common.base.Joiner.on;

/**
 * Encapsulates HTML creation for the e-mail report.
 *
 * @author rnaegele
 */
public class EmailSkeleton extends HtmlWriter {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final TestMetadata testMetadata;
	private final ResourceBundle resourceBundle;
	private final List<? extends List<String>> data;
	private final String linkToReport;
	private final DateTimeFormatter dateTimeFormatter;
	private final Map<String, List<? extends List<String>>> comparisonData;

	public EmailSkeleton(final TestMetadata testMetadata, final ResourceBundle resourceBundle, final Locale locale,
			final List<? extends List<String>> data, final Map<String, List<? extends List<String>>> comparisonData,
			final String linkToReport) {
		this.testMetadata = testMetadata;
		this.resourceBundle = resourceBundle;
		this.data = data;
		this.comparisonData = comparisonData;
		this.linkToReport = linkToReport;
		this.dateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME.withLocale(locale);
	}

	@Override
	protected void build() {
		String css = null;
		try {
			css = Resources.toString(Resources.getResource("email/email.css"), Charsets.UTF_8);
		} catch (IOException ex) {
			log.error("Could not read CSS for e-mail", ex);
		}

		raw("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");

		//@formatter:off

		html();
			head();
				title().text("perfAlyzer Report").end();
				meta().httpEquiv("Content-Type").content("text/html; charset=UTF-8").end();
				if (css != null) {
					style().type("text/css").text(SystemUtils.LINE_SEPARATOR +  css).end();
				}
			end();
			body();
				div().id("content");
					h1().text("perfAlyzer E-mail Report").end();
					div();
						table();
							tbody();
								tr();
									th().text(resourceBundle.getString("overview.testplan")).end();
									td().text(testMetadata.getTestPlanFile()).end();
								end();
								tr();
									th().text(resourceBundle.getString("overview.start")).end();
									td().text(dateTimeFormatter.format(testMetadata.getTestStart())).end();
								end();
								tr();
									th().text(resourceBundle.getString("overview.end")).end();
									td().text(dateTimeFormatter.format(testMetadata.getTestEnd())).end();
								end();
								tr();
									th().text(resourceBundle.getString("overview.duration")).end();
									td().text(testMetadata.getTestDuration()).end();
								end();
								tr();
									th().text(resourceBundle.getString("overview.operations")).end();
									td().text(on(", ").join(testMetadata.getOperations())).end();
								end();
								tr();
									th().text(resourceBundle.getString("overview.comment")).end();
									td().text(testMetadata.getTestComment()).end();
								end();
								tr();
									th().text(resourceBundle.getString("overview.rawResultsDir")).end();
									td().text(testMetadata.getRawResultsDir()).end();
								end();
								tr();
									th().text(resourceBundle.getString("overview.perfloadVersion")).end();
									td().text(testMetadata.getPerfLoadVersion()).end();
								end();
								if (linkToReport != null) {
									tr();
										th().text(resourceBundle.getString("report.link")).end();
										td().a().href(linkToReport).text(linkToReport).alt("Link to perfAlyzer Report").end().end();
									end();
								}
							end();
						end();
					end();
					br();
					addDataTable(data, true);
					for (Entry<String, List<? extends List<String>>> entry : comparisonData.entrySet()) {
						br();
						h2().text("Comparison - " + entry.getKey()).end();
						addDataTable(entry.getValue(), false);
					}
				end();
			end();
		end();

		//@formatter:on
	}

	private void addDataTable(final List<? extends List<String>> tableData, final boolean firstColumLeftAligned) {
		//@formatter:off
		
		div();
			table();
				boolean header = true;
				for (List<String> rowData : tableData) {
					if (header) {
						header = false;
	
						tr();
							for (String item : rowData) {
								th().nowrap("nowrap").text(resourceBundle.getString(item)).end();
							}
						end();
					} else {
						tr();
							int i = 0;
							for (String item : rowData) {
								td();
								if (i++ > 0 || !firstColumLeftAligned) {
									style("text-align: right;");
								}
								nowrap("nowrap").text(item).end();
							}
						end();
					}
				}
			end();
		end();
		
		//@formatter:on
	}

}
