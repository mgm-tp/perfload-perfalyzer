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
package com.mgmtp.perfload.perfalyzer.reporting;

import com.googlecode.jatl.HtmlWriter;

import java.time.Year;

/**
 * @author rnaegele
 */
public class HtmlSkeleton extends HtmlWriter {
	private final String title;
	private final NavBar navBar;
	private final OverviewItem overviewItem;
	private final Content content;
	private final String createdString;

	public HtmlSkeleton(final String title, final String createdString, final NavBar navBar, final OverviewItem overviewItem, final Content content) {
		this.title = title;
		this.createdString = createdString;
		this.navBar = navBar;
		this.overviewItem = overviewItem;
		this.content = content;
	}

	@Override
	protected void build() {
		//@formatter:off

		raw("<!DOCTYPE html>"); // HTML5 doc type

		html().lang("en");
			head();
				title().text("perfAlyzer Report - " + title).end();
				meta().charset("UTF-8");
				meta().name("viewport").content("width=device-width, initial-scale=1.0");
				link().href("assets/images/icon.png").rel("icon").type("image/png");
				link().href("assets/stylesheets/perfalyzer.css").rel("stylesheet");
				script().src("assets/javascripts/jquery.js").end();
				script().src("assets/javascripts/bootstrap.js").end();
			end();
			body();
				div().classAttr("perf-container");
					write(navBar);
					write(overviewItem);
					write(content);
					hr();
					start("footer").classAttr("perf-footer");
						p().text(createdString).end();
						p().raw(String.format("&copy; %s mgm technology partners GmbH", Year.now())).end();
					end();
				end();
				script().src("assets/javascripts/perfalyzer.js").end();
			end();
		end();

		//@formatter:on
	}
}
