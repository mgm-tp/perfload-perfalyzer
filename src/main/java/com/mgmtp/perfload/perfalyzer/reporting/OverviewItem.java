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
package com.mgmtp.perfload.perfalyzer.reporting;

import com.googlecode.jatl.HtmlWriter;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

import static com.google.common.base.Joiner.on;
import static org.apache.commons.io.FilenameUtils.removeExtension;

/**
 * @author rnaegele
 */
public class OverviewItem extends HtmlWriter {
	private final TestMetadata testMetadata;
	private final ResourceBundle resourceBundle;
	private final DateTimeFormatter dateTimeFormatter;

	public OverviewItem(final TestMetadata testMetadata, final ResourceBundle resourceBundle, final Locale locale) {
		this.testMetadata = testMetadata;
		this.resourceBundle = resourceBundle;
		this.dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withLocale(locale);
	}

	@Override
	protected void build() {
		//@formatter:off

		div().id("testmeta");
			div().classAttr("perf-row");
				div().classAttr("perf-col-2");
					img().src("assets/images/perfAlyzer.png").alt("perfAlyzer Logo").end();
				end();
				div().classAttr("perf-col-7");
					h1().classAttr("perf-report-title").text(removeExtension(testMetadata.getTestPlanFile())).end();
				end();
				div().classAttr("perf-col-3");
					img().classAttr("perf-logo-img").src("assets/images/perfLoad_Logo_wort3.png").alt("perfLoad Logo").end();
				end();
			end();
			div().classAttr("perf-row");
				div().classAttr("perf-col-6");
					h3().classAttr("perf-overview-header").text(resourceBundle.getString("overview.header")).end();
					table().classAttr("perf-overview-table");
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
						end();
					end();
				end();
				div().classAttr("perf-col-6");
					String loadprofile = resourceBundle.getString("overview.loadprofile");
					h3().classAttr("perf-overview-header").text(loadprofile).end();
					img().src("console/[loadprofile].png").alt(loadprofile);
				end();
			end();
		end();
		//@formatter:on
	}

}
