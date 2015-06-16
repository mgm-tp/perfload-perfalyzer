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
package com.mgmtp.perfload.perfalyzer.util;

import static com.mgmtp.perfload.perfalyzer.util.AggregationType.MEAN;
import static com.mgmtp.perfload.perfalyzer.util.AggregationType.MEDIAN;
import static com.mgmtp.perfload.perfalyzer.util.PerfMonTypeConfig.Constants.AGGR_HEADERS_1;
import static com.mgmtp.perfload.perfalyzer.util.PerfMonTypeConfig.Constants.AGGR_HEADERS_2;
import static com.mgmtp.perfload.perfalyzer.util.PerfMonTypeConfig.Constants.HEADER_MEAN;
import static com.mgmtp.perfload.perfalyzer.util.PerfMonTypeConfig.Constants.HEADER_MEDIAN;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author rnaegele
 */
public enum PerfMonTypeConfig {
	CPU("cpu_X", HEADER_MEAN, AGGR_HEADERS_1, MEAN),
	JAVA("java_\\d+", HEADER_MEAN, AGGR_HEADERS_1, MEAN),
	IO("io_\\d+(?:_(?:r|w))?", true, 1d / 1024d, HEADER_MEAN, AGGR_HEADERS_1, MEAN),
	MEM("mem", 1d / 1024d, HEADER_MEDIAN, AGGR_HEADERS_2, MEDIAN),
	SWAP("swap", 1d / 1024d, HEADER_MEDIAN, AGGR_HEADERS_2, MEDIAN),
	DOCKER_CPU("docker_cpu_X|\\S+", HEADER_MEAN, AGGR_HEADERS_1, MEAN),
	DOCKER_MEM("docker_mem|\\S+", 1d / 1024d, HEADER_MEDIAN, AGGR_HEADERS_2, MEDIAN);

	static class Constants {
		static final String HEADER_MEAN = "mean";
		static final String HEADER_MEDIAN = "median";
		static final List<String> AGGR_HEADERS_1 = asList("min", "mean", "max");
		static final List<String> AGGR_HEADERS_2 = asList("min", "q0.1", "q0.5", "q0.9", "max");
	}

	private final Pattern pattern;
	private final boolean normalizeValues;
	private final double factor;
	private final String header;
	private final List<String> aggregatedHeaders;
	private final AggregationType aggregationType;

	private PerfMonTypeConfig(final String pattern, final boolean normalizeValues, final double factor, final String header,
			final List<String> aggregatedHeaders, final AggregationType aggregationType) {
		this.normalizeValues = normalizeValues;
		this.factor = factor;
		this.header = header;
		this.aggregatedHeaders = aggregatedHeaders;
		this.aggregationType = aggregationType;
		this.pattern = Pattern.compile(pattern);
	}

	private PerfMonTypeConfig(final String pattern, final double factor, final String header, final List<String> aggregatedHeaders,
			final AggregationType aggregationType) {
		this(pattern, false, factor, header, aggregatedHeaders, aggregationType);
	}

	private PerfMonTypeConfig(final String pattern, final String header, final List<String> aggregatedHeaders, final AggregationType aggregationType) {
		this(pattern, false, 1d, header, aggregatedHeaders, aggregationType);
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

	public String getHeader() {
		return header;
	}

	public List<String> getAggregatedHeaders() {
		return aggregatedHeaders;
	}

	public AggregationType getAggregationType() {
		return aggregationType;
	}

	public static PerfMonTypeConfig fromString(final String perfmonType) {
		for (PerfMonTypeConfig tc : PerfMonTypeConfig.values()) {
			Matcher matcher = tc.getPattern().matcher(perfmonType);
			if (matcher.matches()) {
				return tc;
			}
		}
		throw new IllegalStateException("No binning content found for type: " + perfmonType);
	}
}
