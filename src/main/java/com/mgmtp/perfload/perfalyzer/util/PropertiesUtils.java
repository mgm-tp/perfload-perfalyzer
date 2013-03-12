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

import static com.google.common.io.Closeables.closeQuietly;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * @author rnaegele
 */
public class PropertiesUtils {

	private PropertiesUtils() {
		// no-op
	}

	public static Properties loadIntoProperties(final File propertiesFile, final Properties props) throws IOException {
		Reader br = null;
		try {
			br = Files.newReader(propertiesFile, Charsets.UTF_8);
			props.load(br);
			return props;
		} finally {
			closeQuietly(br);
		}
	}

	public static Properties loadProperties(final File propertiesFile) throws IOException {
		Reader br = null;
		try {
			br = Files.newReader(propertiesFile, Charsets.UTF_8);
			Properties props = new Properties();
			props.load(br);
			return props;
		} finally {
			closeQuietly(br);
		}
	}

	public static Properties saveProperties(final File propertiesFile, final Properties props) throws IOException {
		Writer w = null;
		try {
			w = Files.newWriter(propertiesFile, Charsets.UTF_8);
			props.store(w, "perfAlyzer properties");
			return props;
		} finally {
			closeQuietly(w);
		}
	}
}
