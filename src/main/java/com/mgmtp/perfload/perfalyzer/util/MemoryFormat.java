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
package com.mgmtp.perfload.perfalyzer.util;

import java.text.NumberFormat;
import java.util.Locale;
import javax.inject.Inject;

/**
 *
 * @author rnaegele
 */
public class MemoryFormat {

	private static final long ONE_KB = 1024L;
	private static final long TEN_KB = ONE_KB * 10L;
	private static final long ONE_MB = 1024L * ONE_KB;
	private static final long TEN_MB = ONE_MB * 10L;
	private static final long ONE_GB = 1024L * ONE_MB;
	private static final long TEN_GB = ONE_GB * 10L;

	private final NumberFormat format;

	@Inject
	public MemoryFormat(final Locale locale) {
		format = NumberFormat.getNumberInstance(locale);
		format.setMaximumFractionDigits(2);
	}

	public String format(final double memInK) {
		final double bytes = memInK * ONE_KB;
		if (bytes >= TEN_GB) {
			return format.format(bytes / ONE_GB) + " GiB";
		} else if (bytes >= TEN_MB) {
			return format.format(bytes / ONE_MB) + " MiB";
		} else if (bytes >= TEN_KB) {
			return format.format(bytes / ONE_KB) + " KiB";
		} else {
			return format.format(bytes) + " B";
		}
	}
}
