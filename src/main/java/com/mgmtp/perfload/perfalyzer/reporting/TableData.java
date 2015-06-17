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

import java.util.List;

/**
 * @author rnaegele
 */
public class TableData {

	final List<String> headers;
	final List<? extends List<String>> rowData;
	private final int valueColumnsCount;
	private final boolean imageInNewRow;

	public TableData(final List<String> headers, final List<? extends List<String>> rowData, final int valueColumnsCount,
			final boolean imageInNewRow) {
		this.headers = headers;
		this.rowData = rowData;
		this.valueColumnsCount = valueColumnsCount;
		this.imageInNewRow = imageInNewRow;
	}

	/**
	 * @return the headers
	 */
	public List<String> getHeaders() {
		return headers;
	}

	/**
	 * @return the valueColumnsCount
	 */
	public int getValueColumnsCount() {
		return valueColumnsCount;
	}

	/**
	 * @return the imageInNewRow
	 */
	public boolean isImageInNewRow() {
		return imageInNewRow;
	}

	/**
	 * @return the rowData
	 */
	public List<? extends List<String>> getRowData() {
		return rowData;
	}
}
