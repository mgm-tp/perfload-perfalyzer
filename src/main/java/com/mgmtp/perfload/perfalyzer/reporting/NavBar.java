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

import java.util.Map;
import java.util.Set;

/**
 * @author rnaegele
 */
public class NavBar extends HtmlWriter {

	private final Set<String> tabNames;
	private final Map<String, QuickJump> quickJumps;

	public NavBar(final Set<String> tabNames, final Map<String, QuickJump> quickJumps) {
		this.tabNames = tabNames;
		this.quickJumps = quickJumps;
	}

	@Override
	protected void build() {
		start("nav").id("navigation");
			div().classAttr("perf-navbar-header");
				a().href("").text("perfAlyzer Report").end();
			end();
			div().classAttr("perf-navbar-content");
				// only if markers are present:
				if (tabNames.size() > 1) {
					ul().attr("role", "tablist");
					boolean active = true;
					for (String tab : tabNames) {
						li().attr("role", "presentation");
							if (active) {
								classAttr("active");
								active = false;
							}
							a().href("#" + tab).id("tab_" + tab).attr("data-toggle", "tab", "role", "tab").text(tab).end();
						end();
					}
					end();
				}

				ul().classAttr("perf-quickjump");
					boolean active = true;
					for (String tab : tabNames) {
						li().id("quickjump_" + tab);
							if (active) {
								classAttr("dropdown show");
								active = false;
							} else {
								classAttr("dropdown hide");
							}
							a().classAttr("dropdown-toggle").href("#").attr("data-toggle", "dropdown", "aria-expanded", "false");
								text("Quick Jump");
								span().classAttr("caret").end();
							end();
							ul().classAttr("dropdown-menu").attr("role", "menu");
								QuickJump quickJump = quickJumps.get(tab);
								quickJump.getEntryMap().entrySet().forEach(entry -> {
									li();
										a();
											href("#" + entry.getKey()).text(entry.getValue());
										end();
									end();
								});
							end();
						end();
					}
				end();
			end();
		end();
	}
}
