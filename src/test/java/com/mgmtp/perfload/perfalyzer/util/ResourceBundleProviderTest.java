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

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import org.testng.annotations.Test;

import com.mgmtp.perfload.perfalyzer.util.ResourceBundleProvider.Utf8Control;

/**
 * @author rnaegele
 */
public class ResourceBundleProviderTest {

	//	@Test
	//	public void shouldLoadEnglishLocalizations() {
	//		Provider<ResourceBundle> provider = new ResourceBundleProvider(Locale.US, new Utf8Control(new File("src/test/resources/strings")));
	//		ResourceBundle bundle = provider.get();
	//		String cpuUsage = bundle.getString("title.cpu");
	//		assertEquals(cpuUsage, "CPU Usage");
	//	}

	@Test
	public void shouldLoadGermanLocalizations() {
		ResourceBundleProvider provider = new ResourceBundleProvider(Locale.GERMANY, 
				new Utf8Control(new File("src/test/resources/strings")));
		ResourceBundle bundle = provider.get();
		String cpuUsage = bundle.getString("title.cpu");
		assertEquals(cpuUsage, "CPU-Auslastung");
	}
}
