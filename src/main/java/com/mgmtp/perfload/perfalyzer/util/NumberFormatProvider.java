package com.mgmtp.perfload.perfalyzer.util;

import java.text.NumberFormat;
import java.util.Locale;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.text.DecimalFormat;

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