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
package com.mgmtp.perfload.perfalyzer.workflow;

import com.mgmtp.perfload.perfalyzer.util.NumberFormatProvider;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.perfload.perfalyzer.reportpreparation.DataRange;
import com.mgmtp.perfload.perfalyzer.reportpreparation.DisplayData;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator;
import com.mgmtp.perfload.perfalyzer.util.Marker;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;
import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;

/**
 * @author rnaegele
 */
public abstract class AbstractWorkflow implements Workflow {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final TimestampNormalizer timestampNormalizer;
	protected final NumberFormatProvider intNumberFormatProvider;
	protected final NumberFormatProvider floatNumberFormatProvider;
	protected final List<DisplayData> displayDataList;
	protected final ResourceBundle resourceBundle;
	protected final TestMetadata testMetadata;
	protected final PlotCreator plotCreator;

	protected AbstractWorkflow(final TimestampNormalizer timestampNormalizer, final NumberFormatProvider intNumberFormatProvider2,
			final NumberFormatProvider numberFormatProvider1, final List<DisplayData> displayDataList, final ResourceBundle resourceBundle,
			final TestMetadata testMetadata, final PlotCreator plotCreator) {
		this.timestampNormalizer = timestampNormalizer;
		this.intNumberFormatProvider = intNumberFormatProvider2;
		this.floatNumberFormatProvider = numberFormatProvider1;
		this.displayDataList = displayDataList;
		this.resourceBundle = resourceBundle;
		this.testMetadata = testMetadata;
		this.plotCreator = plotCreator;
	}

	protected DataRange rangeFromMarker(final Marker marker) {
		return marker != null ? new DataRange(marker.getLeftMillis(), marker.getRightMillis()) : null;
	}
}
