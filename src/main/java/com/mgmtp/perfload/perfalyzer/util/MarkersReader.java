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

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.lang3.text.StrTokenizer;

import com.google.common.collect.ImmutableList;

/**
 * @author ctchinda
 */
public class MarkersReader {

	private static final String MARKER = "[[marker]]";
	private static final String MARKER_LEFT = "left";
	private static final String MARKER_RIGHT = "right";

	private static final int COL_TIMESTAMP = 0;
	private static final int COL_MARKER = 1;
	private static final int COL_MARKER_NAME = 2;
	private static final int COL_MARKER_TYPE = 3;

	private final File inputFile;

	public MarkersReader(final File inputFile) {
		this.inputFile = inputFile;
	}

	public List<Marker> readMarkers() throws IOException {
		Map<String, Marker> markers = newHashMapWithExpectedSize(3);

		StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
		tokenizer.setDelimiterChar(DELIMITER);

		try (FileInputStream fis = new FileInputStream(inputFile)) {

			for (Scanner scanner = new Scanner(fis.getChannel()); scanner.hasNext();) {
				String line = scanner.nextLine();
				if (line.startsWith("#")) {
					continue;
				}

				tokenizer.reset(line);

				List<String> tokenList = tokenizer.getTokenList();

				if (MARKER.equals(tokenList.get(COL_MARKER))) {
					String markerName = tokenList.get(COL_MARKER_NAME);
					String markerType = tokenList.get(COL_MARKER_TYPE);
					long timeMillis = Long.parseLong(tokenList.get(COL_TIMESTAMP));

					if (MARKER_LEFT.equals(markerType)) {
						Marker marker = new Marker(markerName);
						markers.put(markerName, marker);
						marker.setLeftMillis(timeMillis);
					} else if (MARKER_RIGHT.equals(markerType)) {
						Marker marker = markers.get(markerName);
						marker.setRightMillis(timeMillis);
					} else {
						throw new IllegalStateException("Invalid marker type: " + markerType);
					}
				}
			}

			return ImmutableList.copyOf(markers.values());
		}
	}
}
