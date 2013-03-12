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
package com.mgmtp.perfload.perfalyzer.util;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

/**
 * 
 * @author rnaegele
 */
public class MemoryFormat extends DecimalFormat {

	private static final long ONE_KB = 1024L;
	private static final long TEN_KB = ONE_KB * 10L;
	private static final long ONE_MB = 1024L * ONE_KB;
	private static final long TEN_MB = ONE_MB * 10L;

	private final NumberFormat format;

	public MemoryFormat(final Locale locale) {
		format = NumberFormat.getNumberInstance(locale);
		format.setMaximumFractionDigits(1);
	}

	@Override
	public StringBuffer format(final double memInK, final StringBuffer toAppendTo, final FieldPosition pos) {
		final double bytes = memInK * ONE_KB;
		if (bytes >= TEN_MB) {
			format.format(bytes / ONE_MB, toAppendTo, pos);
			toAppendTo.append(" MiB");
		} else if (bytes >= TEN_KB) {
			format.format(bytes / ONE_KB, toAppendTo, pos);
			toAppendTo.append(" KiB");
		} else {
			format.format(bytes, toAppendTo, pos);
			toAppendTo.append(" B");
		}
		return toAppendTo;
	}

	@Override
	public StringBuffer format(final long memInK, final StringBuffer toAppendTo, final FieldPosition pos) {
		final double bytes = memInK * ONE_KB;
		if (bytes >= TEN_MB) {
			format.format(bytes / ONE_MB, toAppendTo, pos);
			toAppendTo.append(" MiB");
		} else if (bytes >= TEN_KB) {
			format.format(bytes / ONE_KB, toAppendTo, pos);
			toAppendTo.append(" KiB");
		} else {
			format.format(bytes, toAppendTo, pos);
			toAppendTo.append(" B");
		}
		return toAppendTo;
	}

	@Override
	public Number parse(final String source, final ParsePosition parsePosition) {
		throw new UnsupportedOperationException("class does not support parsing");
	}
}