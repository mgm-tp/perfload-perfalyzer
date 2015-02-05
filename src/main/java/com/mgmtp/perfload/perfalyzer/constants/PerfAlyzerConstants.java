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
package com.mgmtp.perfload.perfalyzer.constants;

/**
 * @author rnaegele
 */
public class PerfAlyzerConstants {

	public static final char DELIMITER = ';';

	public static final int BIN_SIZE_MILLIS_1_SECOND = 1000;
	public static final int BIN_SIZE_MILLIS_30_SECONDS = 30 * BIN_SIZE_MILLIS_1_SECOND;
	public static final int BIN_SIZE_MILLIS_1_MINUTE = 60 * BIN_SIZE_MILLIS_1_SECOND;
	public static final int BIN_SIZE_MILLIS_10_MINUTES = 10 * BIN_SIZE_MILLIS_1_MINUTE;

	public static final int MEASURING_RAW_COL_RESPONSE_TIME_FIRST_BYTE = 4;
	public static final int MEASURING_RAW_COL_RESPONSE_TIME = 5;
	public static final int MEASURING_RAW_COL_OPERATION = 6;
	public static final int MEASURING_RAW_COL_RESULT = 8;
	public static final int MEASURING_RAW_COL_ERROR_MSG = 9;
	public static final int MEASURING_RAW_COL_REQUEST_TYPE = 10;
	public static final int MEASURING_RAW_COL_URI = 11;
	public static final int MEASURING_RAW_COL_URI_ALIAS = 12;
	public static final int MEASURING_RAW_COL_EXECUTION_ID = 15;
	public static final int MEASURING_RAW_COL_REQUEST_ID = 16;

	public static final int MEASURING_NORMALIZED_COL_REQUEST_TYPE = 4;
	public static final int MEASURING_NORMALIZED_COL_URI_ALIAS = 6;
	public static final int MEASURING_NORMALIZED_COL_RESULT = 7;
	public static final int MEASURING_NORMALIZED_COL_ERROR_MSG = 8;
	public static final int MEASURING_NORMALIZED_COL_EXECUTION_ID = 9;

	private PerfAlyzerConstants() {
	}
}
