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

import org.joda.time.DateTime;

import com.googlecode.jatl.HtmlWriter;

/**
 * @author rnaegele
 */
public class HtmlSkeleton extends HtmlWriter {
	private final String title;
	private final NavBar navBar;
	private final OverviewItem overviewItem;
	private final Content content;
	private final String createdString;
	private final String topLinkName;

	public HtmlSkeleton(final String title, final String createdString, final NavBar navBar, final OverviewItem overviewItem,
			final Content content, final String topLinkName) {
		this.title = title;
		this.createdString = createdString;
		this.navBar = navBar;
		this.overviewItem = overviewItem;
		this.content = content;
		this.topLinkName = topLinkName;
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
				link().href("assets/img/icon.png").rel("icon").type("image/png");
				link().href("assets/css/bootstrap.css").rel("stylesheet");
				script().src("assets/js/jquery-1.7.2.min.js").end();
				script().src("assets/js/bootstrap.js").end();
			end();
			body();

				write(navBar);

				div().classAttr("container");
					div().classAttr("row-fluid");
						write(overviewItem);
					end();
					div().classAttr("row-fluid");
						write(content);
					end();

					hr();

					start("footer").classAttr("footer");
						p().classAttr("pull-right");
							a().href("#").text(topLinkName).end();
						end();

						p().text(createdString).end();
						p().raw(String.format("&copy; %d mgm technology partners GmbH", new DateTime().getYear())).end();
					end();
				end();
			end();
		end();

		//@formatter:on
	}
}
