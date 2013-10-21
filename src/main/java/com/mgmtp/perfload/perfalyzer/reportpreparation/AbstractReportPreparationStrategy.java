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
package com.mgmtp.perfload.perfalyzer.reportpreparation;

import java.text.NumberFormat;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;

/**
 * @author rnaegele
 */
abstract class AbstractReportPreparationStrategy implements ReportPreparationStrategy {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final NumberFormat intNumberFormat;
	protected final NumberFormat floatNumberFormat;
	protected final List<DisplayData> displayDataList;
	protected final ResourceBundle resourceBundle;
	protected final PlotCreator plotCreator;
	protected final TestMetadata testMetadata;

	protected AbstractReportPreparationStrategy(final NumberFormat intNumberFormat,
			final NumberFormat floatNumberFormat, final List<DisplayData> displayDataList,
			final ResourceBundle resourceBundle, final PlotCreator plotCreator, final TestMetadata testMetadata) {
		this.intNumberFormat = intNumberFormat;
		this.floatNumberFormat = floatNumberFormat;
		this.plotCreator = plotCreator;
		this.testMetadata = testMetadata;
		this.displayDataList = ImmutableList.copyOf(displayDataList);
		this.resourceBundle = resourceBundle;
	}
}
