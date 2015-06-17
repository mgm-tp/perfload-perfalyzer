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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author rnaegele
 */
public class Content extends HtmlWriter {

	private final Map<String, List<ContentItem>> contentItems;

	public Content(final Map<String, List<ContentItem>> contentItems) {
		this.contentItems = contentItems;
	}

	@Override
	protected void build() {
		//@formatter:off

		div().attr("role", "tabpanel");
			div().classAttr("perf-tab-content");

			boolean active = true;
			for (Entry<String, List<ContentItem>> entry : contentItems.entrySet()) {
				div();
					if (active) {
						classAttr("perf-tab-pane active");
						active = false;
					} else {
						classAttr("perf-tab-pane");
					}
					String tab = entry.getKey();
					attr("role", "tabpanel").id(tab);

					entry.getValue().forEach(contentItem-> write(contentItem));
				end();
			}

			end();
		end();

		//@formatter:on
	}
}
