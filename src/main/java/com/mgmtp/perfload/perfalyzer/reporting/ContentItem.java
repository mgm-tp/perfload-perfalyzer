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

import com.googlecode.jatl.HtmlWriter;

/**
 * @author rnaegele
 */
public class ContentItem extends HtmlWriter {

	private final int itemIndex;
	private final String itemTitle;
	private final TableData tableData;
	private final String plotSrc;

	public ContentItem(final int itemIndex, final String itemTitle, final TableData tableData, final String plotSrc) {
		this.itemIndex = itemIndex;
		this.itemTitle = itemTitle;
		this.tableData = tableData;
		this.plotSrc = plotSrc != null ? plotSrc.replace('\\', '/') : null;
	}

	@Override
	protected final void build() {
		//@formatter:off

		div().classAttr("accordion-group").id("accordion" + itemIndex);
			div().classAttr("accordion-heading");
				a().classAttr("accordion-toggle").attr("data-toggle", "collapse", "data-parent", "#accordion").href("#collapse" + itemIndex);
					text(itemTitle);
				end();
			end();
			div().id("collapse" + itemIndex).classAttr("accordion-body collapse in");
				div().classAttr("accordion-inner");
					if (tableData != null) {
						div().classAttr("row-fluid");
							boolean imageInNewRow = tableData.isImageInNewRow();
							div().classAttr(imageInNewRow || plotSrc == null ? "span12" : "span5");
								table().classAttr("table table-bordered table-condensed table-striped");
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
									div().classAttr("span7");
										img().src(plotSrc).alt(plotSrc);
									end();
								}
							}
						end();
					}
					if (tableData == null || tableData.isImageInNewRow()) {
						div().classAttr("row-fluid");
							div().classAttr("span12");
								img().src(plotSrc).alt(plotSrc);
							end();
						end();
					}
				end();
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
