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

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Set;

import static com.google.common.base.Splitter.on;
import static com.google.common.collect.Sets.newTreeSet;

/**
 * @author rnaegele
 */
public class TestMetadata {

	private final ZonedDateTime testStart;
	private final ZonedDateTime testEnd;
	private final String testDuration;
	private final String testPlanFile;
	private final String rawResultsDir;
	private final String perfLoadVersion;
	private final String testComment;
	private final Set<String> operations;

	public TestMetadata(final ZonedDateTime testStart, final ZonedDateTime testEnd, final String testDuration, final String testPlanFile,
			final String rawResultsDir, final String perfLoadVersion, final String testComment, final Set<String> operations) {
		this.testStart = testStart;
		this.testEnd = testEnd;
		this.testDuration = testDuration;
		this.testPlanFile = testPlanFile;
		this.rawResultsDir = rawResultsDir;
		this.perfLoadVersion = perfLoadVersion;
		this.testComment = testComment;
		this.operations = ImmutableSet.copyOf(operations);
	}

	public static TestMetadata create(final String rawResultsDir, final Properties properties) {
		ZonedDateTime start = ZonedDateTime.parse(properties.getProperty("test.start"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		ZonedDateTime end = ZonedDateTime.parse(properties.getProperty("test.finish"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		String duration = DurationFormatUtils.formatDurationHMS(Duration.between(start, end).toMillis());

		String operationsString = properties.getProperty("operations");
		Set<String> operations = newTreeSet(on(',').trimResults().split(operationsString));
		return new TestMetadata(start, end, duration, properties.getProperty("test.file"), rawResultsDir,
				properties.getProperty("perfload.implementation.version"), properties.getProperty("test.comment"), operations);
	}

	/**
	 * @return the testStart
	 */
	public ZonedDateTime getTestStart() {
		return testStart;
	}

	/**
	 * @return the testEnd
	 */
	public ZonedDateTime getTestEnd() {
		return testEnd;
	}

	/**
	 * @return the testDuration
	 */
	public String getTestDuration() {
		return testDuration;
	}

	/**
	 * @return the testPlanFile
	 */
	public String getTestPlanFile() {
		return testPlanFile;
	}

	/**
	 * @return the rawResultsDir
	 */
	public String getRawResultsDir() {
		return rawResultsDir;
	}

	/**
	 * @return the perfLoadVersion
	 */
	public String getPerfLoadVersion() {
		return perfLoadVersion;
	}

	/**
	 * @return the testComment
	 */
	public String getTestComment() {
		return testComment;
	}

	/**
	 * @return the operations
	 */
	public Set<String> getOperations() {
		return operations;
	}
}
