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

import java.util.regex.Pattern;

/**
 * 
 * @author rnaegele
 */
public enum PerfMonTypeConfig {
	CPU("cpu_X"),
	JAVA("java_\\d+"),
	IO("io_\\d+(?:_(?:r|w))?", true, 1d / 1024d),
	MEM("mem", 1d / 1024d),
	SWAP("swap", 1d / 1024d);

	private final Pattern pattern;
	private final boolean normalizeValues;
	private double factor;

	private PerfMonTypeConfig(final String pattern, final boolean normalizeValues, final double factor) {
		this.normalizeValues = normalizeValues;
		this.factor = factor;
		this.pattern = Pattern.compile(pattern);
	}

	private PerfMonTypeConfig(final String pattern, final double factor) {
		this(pattern, false, factor);
	}

	private PerfMonTypeConfig(final String pattern) {
		this(pattern, false, 1d);
	}

	/**
	 * @return the pattern
	 */
	public Pattern getPattern() {
		return pattern;
	}

	/**
	 * @return the normalizeValues
	 */
	public boolean isNormalizeValues() {
		return normalizeValues;
	}

	public double factor(final double value) {
		return value * factor;
	}
}