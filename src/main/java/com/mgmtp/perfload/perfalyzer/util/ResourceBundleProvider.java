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

import static com.google.common.io.Files.newReader;
import static org.apache.commons.io.FilenameUtils.wildcardMatch;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

/**
 * Provider for a {@link ResourceBundle} with localizations. Resource bundles are search in the
 * directory {@code ./res}.
 * 
 * @author rnaegele
 */
public class ResourceBundleProvider implements Provider<ResourceBundle> {

	private final Locale locale;
	private final Control control;

	@Inject
	public ResourceBundleProvider(final Locale locale, final Control control) {
		this.control = control;
		this.locale = locale;
	}

	@Override
	public ResourceBundle get() {
		return ResourceBundle.getBundle("strings", locale, control);
	}

	public static class Utf8Control extends Control {

		private final Logger log = LoggerFactory.getLogger(getClass());

		private static final String FORMAT_UTF8 = "utf8.props";
		private static final Locale FALLBACK_LOCALE = new Locale("");

		private final File resourceDir;

		public Utf8Control(final File resourceDir) {
			this.resourceDir = resourceDir;
		}

		@Override
		public ResourceBundle newBundle(final String baseName, final Locale locale, final String format,
				final ClassLoader loader, final boolean reload) throws IOException {

			String bundleName = toBundleName(baseName, locale);
			String resourceName = toResourceName(bundleName, FORMAT_UTF8);

			// must use a reader, so UTF-8 can be used in properties files
			try (Reader r = newReader(new File(resourceDir, resourceName), Charsets.UTF_8)) {
				return new PropertyResourceBundle(r) {
					@Override
					public Object handleGetObject(final String key) {
						for (Enumeration<String> en = getKeys(); en.hasMoreElements();) {
							String resolvedKey = en.nextElement();
							if (wildcardMatch(key, resolvedKey)) {
								return super.handleGetObject(resolvedKey);
							}
						}

						Object object = super.handleGetObject(key);
						if (object == null) {
							object = key;
							log.debug("Can't find resource for key '{}'", key);
						}
						return object;
					}
				};
			}
		}

		@Override
		public Locale getFallbackLocale(final String baseName, final Locale locale) {
			// necessary in order to provide the unlocalized default
			return FALLBACK_LOCALE;
		}

		@Override
		public boolean needsReload(final String baseName, final Locale locale, final String format, final ClassLoader loader,
				final ResourceBundle bundle, final long loadTime) {
			return false;
		}

		@Override
		public List<String> getFormats(final String baseName) {
			return ImmutableList.of(FORMAT_UTF8);
		}
	}
}
