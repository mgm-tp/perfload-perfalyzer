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
package com.mgmtp.perfload.perfalyzer.binning;

import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;

import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;

/**
 * @author rnaegele
 */
public abstract class AbstractBinningStrategy implements BinningStrategy {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final long startOfFirstBin;
	protected final NumberFormat intNumberFormat;
	protected final NumberFormat floatNumberFormat;
	protected final StrTokenizer tokenizer = StrTokenizer.getCSVInstance();

	protected AbstractBinningStrategy(final long startOfFirstBin, final NumberFormat intNumberFormat, final NumberFormat floatNumberFormat) {
		this.startOfFirstBin = startOfFirstBin;
		this.intNumberFormat = intNumberFormat;
		this.floatNumberFormat = floatNumberFormat;
		tokenizer.setDelimiterChar(DELIMITER);
	}
}
