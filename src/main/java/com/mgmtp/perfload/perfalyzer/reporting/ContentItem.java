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

/**
 * @author rnaegele
 */
public class ContentItem extends HtmlWriter {

	private final String tab;
	private final int itemIndex;
	private final String itemTitle;
	private final TableData tableData;
	private final String topLinkName;
	private final String plotSrc;

	public ContentItem(final String tab, final int itemIndex, final String itemTitle, final TableData tableData,
			final String plotSrc, final String topLinkName) {
		this.tab = tab;
		this.itemIndex = itemIndex;
		this.itemTitle = itemTitle;
		this.tableData = tableData;
		this.plotSrc = plotSrc != null ? plotSrc.replace('\\', '/') : null;
		this.topLinkName = topLinkName;
	}

	@Override
	protected final void build() {
		//@formatter:off

		div().classAttr("perf-panel").id(tab + "_" + itemIndex);
			div().classAttr("perf-panel-heading");
				div().classAttr("perf-row");
					div().classAttr("perf-col-10 perf-panel-title").text(itemTitle).end();
//					div().classAttr("perf-col-6");
//						if (tabNames.size() > 1) {
//							div().classAttr("perf-tabs");
//								ul().attr("role", "tablist");
//								boolean active = true;
//								for (String tab : tabNames) {
//									li().attr("role", "presentation");
//										if (active) {
//											classAttr("active");
//											active = false;
//										}
//										a().href("#" + tab).attr("data-toggle", "tab", "role", "tab").text(tab).end();
//									end();
//								}
//								end();
//							end();
//						}
//					end();
					div().classAttr("perf-col-2");
						div().classAttr("perf-top-link");
							a().href("#").text(topLinkName).end();
						end();
					end();
				end();
			end();
			div().classAttr("perf-panel-body");
				if (tableData != null) {
					div().classAttr("perf-row");
						boolean imageInNewRow = tableData.isImageInNewRow();
						div().classAttr(imageInNewRow || plotSrc == null ? "perf-span-all" : "perf-col-left");
							table().classAttr("perf-data-table");
								int valueColumnsCount = tableData.getValueColumnsCount();
								int colCount = tableData.getHeaders().size();
								int firstValueColumnIndex = colCount - valueColumnsCount;

								thead();
									tr();

									for (int i = 0; i < colCount; ++i) {
										th();
										if (i >= firstValueColumnIndex) {
											style("text-align: right;");
										}
										text(tableData.getHeaders().get(i));
										end();
									}

									end();
								end();

								tbody();

								for (List<String> rowData : tableData.getRowData()) {
									tr();

									for (int i = 0; i < colCount; ++i) {
										td();
										if (i >= firstValueColumnIndex) {
											style("text-align: right;");
										}
										text(rowData.get(i));
										end();
									}

									end();
								}

								end();
							end();
						end();
						if (plotSrc != null) {
							if (!imageInNewRow) {
								div().classAttr("perf-col-right");
									img().src(plotSrc).alt(plotSrc);
								end();
							}
						}
					end();
				}
				if (tableData == null || tableData.isImageInNewRow()) {
					div().classAttr("perf-row");
						div().classAttr("perf-span-all");
							img().src(plotSrc).alt(plotSrc);
						end();
					end();
				}
			end();
		end();

		//@formatter:on
	}

	/**
	 * @return the itemIndex
	 */
	public int getItemIndex() {
		return itemIndex;
	}

	/**
	 * @return the itemTitle
	 */
	public String getItemTitle() {
		return itemTitle;
	}
}
