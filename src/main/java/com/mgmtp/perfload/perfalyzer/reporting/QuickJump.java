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

import java.util.Map;

/**
 * @author rnaegele
 */
public final class QuickJump {

	private final String tab;
	private final Map<String, String> entryMap;

	/**
	 * @param entryMap
	 *            contians entries for the quick jump drop down, the keys being the target ids
	 *            (starting with a # symbol) and the vaues being the text to be displayed
	 */
	public QuickJump(final String tab, final Map<String, String> entryMap) {
		this.tab = tab;
		this.entryMap = entryMap;
	}

	public String getTab() {
		return tab;
	}

	public Map<String, String> getEntryMap() {
		return entryMap;
	}

	//	@Override
//	protected void build() {
//		//@formatter:off
//
//		div().classAttr("perf-quickjump");
//			div().classAttr("btn-group");
//				button().classAttr("btn btn-xs btn-info dropdown-toggle").attr("data-toggle", "dropdown", "aria-expanded", "false");
//					text("Quick Jump - " + tab);
//					span().classAttr("caret").end();
//				end();
//				ul().classAttr("dropdown-menu").attr("role", "menu");
//				for (Entry<String, String> entry : entryMap.entrySet()) {
//					li();
//						a();
//							href("#" + entry.getKey()).text(entry.getValue());
//						end();
//					end();
//				}
//				end();
//			end();
//		end();
//
//		//@formatter:on
//	}
}
