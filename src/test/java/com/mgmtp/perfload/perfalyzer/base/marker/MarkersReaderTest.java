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
package com.mgmtp.perfload.perfalyzer.base.marker;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.testng.annotations.Test;

import com.mgmtp.perfload.perfalyzer.util.Marker;
import com.mgmtp.perfload.perfalyzer.util.MarkersReader;

/**
 * @author ctchinda
 */
public class MarkersReaderTest {

	@Test
	public void testMarkersCreated() throws IOException {

		MarkersReader markerReader = new MarkersReader(new File("src/test/resources/markers/test.perfload"));

		List<Marker> result = newArrayList(markerReader.readMarkers());
		Collections.sort(result, new Comparator<Marker>() {
			@Override
			public int compare(final Marker o1, final Marker o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		Marker marker1 = result.get(0);
		assertThat(marker1.getName(), is(equalTo("Marker 1")));
		assertThat(marker1.getLeftMillis(), is(equalTo(600000L)));
		assertThat(marker1.getRightMillis(), is(equalTo(900000L)));

		Marker marker2 = result.get(1);
		assertThat(marker2.getName(), is(equalTo("Marker 2")));
		assertThat(marker2.getLeftMillis(), is(equalTo(700000L)));
		assertThat(marker2.getRightMillis(), is(equalTo(1000000L)));

		Marker marker3 = result.get(2);
		assertThat(marker3.getName(), is(equalTo("Marker 3")));
		assertThat(marker3.getLeftMillis(), is(equalTo(1300000L)));
		assertThat(marker3.getRightMillis(), is(equalTo(1400000L)));

		Marker marker4 = result.get(3);
		assertThat(marker4.getName(), is(equalTo("Marker 4")));
		assertThat(marker4.getLeftMillis(), is(equalTo(370000L)));
		assertThat(marker4.getRightMillis(), is(equalTo(1700000L)));
	}
}
