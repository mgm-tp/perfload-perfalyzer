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

import java.util.Map;
import java.util.Map.Entry;

import com.googlecode.jatl.HtmlWriter;

/**
 * @author rnaegele
 */
public final class NavBar extends HtmlWriter {

	private final String testName;
	private final Map<String, String> tocMap;

	/**
	 * @param tocMap
	 *            contians entries for the quick jump drop down, the keys being the target ids
	 *            (starting with a # symbol) and the vaues being the text to be displayed
	 */
	public NavBar(final String testName, final Map<String, String> tocMap) {
		this.testName = testName;
		this.tocMap = tocMap;
	}

	@Override
	protected void build() {
		//@formatter:off

		div().classAttr("navbar navbar-fixed-top");
			div().classAttr("navbar-inner");
				div().classAttr("container");
					a().classAttr("brand").href("#top").text("perfAlyzer Report - " + testName).end();
					ul().classAttr("nav");
						li().classAttr("dropdown");
							a().href("#").classAttr("dropdown-toggle").attr("data-toggle", "dropdown");
								text("Quick Jump").span().classAttr("caret").end();
							end();
							ul().classAttr("dropdown-menu");
								for (Entry<String, String> entry : tocMap.entrySet()) {
									li().a().href("#accordion" + entry.getKey()).text(entry.getValue()).end().end();
								}
							end();
						end();
					end();
				end();
			end();
		end();

		//@formatter:on
	}
}
