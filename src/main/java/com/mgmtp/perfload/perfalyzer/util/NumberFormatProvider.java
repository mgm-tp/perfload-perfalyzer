/*
 * Copyright (c) 2022 mgm technology partners GmbH
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
package com.mgmtp.perfload.perfalyzer.util;

import java.text.NumberFormat;
import java.util.Locale;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.text.DecimalFormat;

/**
 * @author aneugebauer
 */

public class NumberFormatProvider {

	final private NumberFormat numberFormat;

	public NumberFormatProvider(final Locale locale, boolean intProvider) {
		if (intProvider) {
			this.numberFormat = getInt(locale);
		} else {
			this.numberFormat = getFloat(locale);
		}
	}

	public NumberFormat get() {
		return numberFormat;
	}

	private NumberFormat getInt(final Locale locale) {
		NumberFormat nf = NumberFormat.getIntegerInstance(locale);
		nf.setGroupingUsed(false);
		nf.setRoundingMode(RoundingMode.HALF_UP);
		return nf;
	}

	private NumberFormat getFloat(final Locale locale) {
		DecimalFormatSymbols dfs = new DecimalFormatSymbols(locale);
		NumberFormat nf = new DecimalFormat("0.00", dfs);
		nf.setGroupingUsed(false);
		nf.setRoundingMode(RoundingMode.HALF_UP);
		return nf;
	}
}