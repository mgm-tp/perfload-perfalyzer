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
package com.mgmtp.perfload.perfalyzer.reporting;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.googlecode.jatl.HtmlWriter;

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

		if (contentItems.size() > 1) {
			ul().classAttr("nav nav-tabs");
				for (String key : contentItems.keySet()) {
					li().a().href("#" + key).attr("data-toggle", "tab").text(key).end();
				}
			end();

			div().classAttr("tab-content");

			boolean active = true;
			for (Entry<String, List<ContentItem>> entry : contentItems.entrySet()) {
				if (active) {
					div().classAttr("tab-pane active").id(entry.getKey());
					active = false;
				} else {
					div().classAttr("tab-pane").id(entry.getKey());
				}
						div().classAttr("accordion").id("accordion");

							for (ContentItem contentItem : entry.getValue()) {
								write(contentItem);
							}

						end();
					end();
			}

			end();
		} else {

			div().classAttr("accordion").id("accordion");

			for (ContentItem contentItem : contentItems.get("overall")) {
				write(contentItem);
			}

			end();
		}
		//@formatter:on
	}
}
