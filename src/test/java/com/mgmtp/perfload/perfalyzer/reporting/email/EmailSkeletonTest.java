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
package com.mgmtp.perfload.perfalyzer.reporting.email;

import static com.google.common.io.Files.newWriter;
import static com.mgmtp.perfload.perfalyzer.util.PropertiesUtils.loadProperties;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.mgmtp.perfload.perfalyzer.util.ResourceBundleProvider;
import com.mgmtp.perfload.perfalyzer.util.ResourceBundleProvider.Utf8Control;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;

/**
 * @author rnaegele
 */
public class EmailSkeletonTest {

	private final List<? extends List<String>> data = ImmutableList.of(
			ImmutableList.of("operation", "time", "numRequests", "numErrors", "minReqPerSec", "medianReqPerSec", "maxReqPerSec",
					"minReqPerMin", "medianReqPerMin", "maxReqPerMin", "minExecutionTime", "medianExecutionTime",
					"maxExecutionTime"),
			ImmutableList.of("myoperation", "2012-10-26T17:03:58.077+02:00", "79,320", "0", "0", "76", "222", "0", "4,378",
					"8,585", "242", "3,174", "198,476"),
			ImmutableList.of("myoperation", "2012-10-26T17:03:58.077+02:00", "79,320", "0", "0", "76", "222", "0", "4,378",
					"8,585", "242", "3,174", "198,476"),
			ImmutableList.of("myoperation", "2012-10-26T17:03:58.077+02:00", "79,320", "0", "0", "76", "222", "0", "4,378",
					"8,585", "242", "3,174", "198,476"),
			ImmutableList.of("myoperation", "2012-10-26T17:03:58.077+02:00", "79,320", "0", "0", "76", "222", "0", "4,378",
					"8,585", "242", "3,174", "198,476")
			);

	private final Map<String, List<? extends List<String>>> comparisonData = ImmutableMap.of("myoperation1", data,
			"myoperation2", data);

	@Test
	public void testEmailSkeleton() throws IOException {
		Properties perfLoadMetaProps = loadProperties(new File("src/test/resources/email/perfload.meta.utf8.props"));
		TestMetadata testMetadata = TestMetadata.create("rawResultsDir", perfLoadMetaProps);
		Locale locale = Locale.US;
		ResourceBundle bundle = new ResourceBundleProvider(locale, new Utf8Control(new File("distribution/strings"))).get();

		EmailSkeleton email = new EmailSkeleton(testMetadata, bundle, locale, data, comparisonData, "http://link.to/report.html");

		File file = new File("target/tmp", "email.html");
		Files.createParentDirs(file);

		try (Writer wr = newWriter(file, Charsets.UTF_8)) {
			email.write(wr);
		}
	}
}
