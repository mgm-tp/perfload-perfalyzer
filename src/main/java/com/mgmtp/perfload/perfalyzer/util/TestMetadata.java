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

import static com.google.common.base.Splitter.on;
import static com.google.common.collect.Sets.newTreeSet;
import static org.apache.commons.io.FilenameUtils.wildcardMatch;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author rnaegele
 */
public class TestMetadata {

	private final DateTime testStart;
	private final DateTime testEnd;
	private final String testDuration;
	private final String testPlanFile;
	private final String rawResultsDir;
	private final String perfLoadVersion;
	private final String testComment;
	private final Set<String> operations;

	public TestMetadata(final DateTime testStart, final DateTime testEnd, final String testDuration, final String testPlanFile,
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
		DateTime start = new DateTime(properties.getProperty("test.start"));
		DateTime end = new DateTime(properties.getProperty("test.finish"));
		String duration = DurationFormatUtils.formatDurationHMS(new Duration(new DateTime(start), new DateTime(end)).getMillis());

		Set<String> operations = newTreeSet();
		for (Enumeration<?> en = properties.propertyNames(); en.hasMoreElements();) {
			String name = (String) en.nextElement();
			if (wildcardMatch(name, "testplan.*.operations")) {
				operations.addAll(ImmutableList.copyOf(on(',').split(properties.getProperty(name))));
			}
		}
		return new TestMetadata(start, end, duration, properties.getProperty("test.file"), rawResultsDir,
				properties.getProperty("perfload.implementation.version"), properties.getProperty("test.comment"), operations);
	}

	/**
	 * @return the testStart
	 */
	public DateTime getTestStart() {
		return testStart;
	}

	/**
	 * @return the testEnd
	 */
	public DateTime getTestEnd() {
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
