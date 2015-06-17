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

import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.readLastLine;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.Test;

import com.google.common.base.Charsets;

/**
 * @author ctchinda
 */
public class IoUtilitiesTest {

	@Test
	public void testReadLastLine() throws IOException {
		String line = readLastLine(new File("src/test/resources/test.txt"), Charsets.UTF_8);
		assertThat(line, is(equalTo("Das ist die letze Zeile (öäü~}ßé^°")));
	}

}
