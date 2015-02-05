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
package com.mgmtp.perfload.perfalyzer.reportpreparation;

import com.google.common.base.Charsets;
import com.mgmtp.perfload.perfalyzer.reportpreparation.NumberDataSet.SeriesPoint;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator.AxisType;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator.ChartDimensions;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator.RendererType;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import static com.mgmtp.perfload.perfalyzer.util.PerfAlyzerUtils.readDataFile;

/**
 * @author ctchinda
 */
public class LoadProfileReportPreparationStrategy extends AbstractReportPreparationStrategy {

	public LoadProfileReportPreparationStrategy(final NumberFormat intNumberFormat,
			final NumberFormat floatNumberFormat, final List<DisplayData> displayDataList,
			final ResourceBundle resourceBundle, final PlotCreator plotCreator, final TestMetadata testMetadata,
			final DataRange dataRange) {
		super(intNumberFormat, floatNumberFormat, displayDataList, resourceBundle, plotCreator, testMetadata, dataRange);
	}

	@Override
	public void processFiles(final File sourceDir, final File destDir, final List<PerfAlyzerFile> files) throws IOException {

		Map<String, PerfAlyzerFile> byOperationMap = new HashMap<>();
		PerfAlyzerFile destFile = null;

		for (PerfAlyzerFile f : files) {
			log.info("Processing file '{}'...", f);

			if (destFile == null) {
				destFile = f.copy().removeFileNamePart(1).setExtension("png");
			}
			String operation = f.getFileNameParts().get(1);
			byOperationMap.put(operation, f);
		}

		createPlot(sourceDir, destDir, destFile, byOperationMap);
	}

	private void createPlot(final File sourceDir, final File destDir, final PerfAlyzerFile destFile,
			final Map<String, PerfAlyzerFile> byOperationMap) throws IOException {

		NumberDataSet dataSet = new NumberDataSet();

		for (Entry<String, PerfAlyzerFile> entry : byOperationMap.entrySet()) {
			String operation = entry.getKey();
			PerfAlyzerFile dataFile = entry.getValue();

			File file = new File(sourceDir, dataFile.getFile().getPath());
			List<SeriesPoint> dataList = readDataFile(file, Charsets.UTF_8, intNumberFormat);
			dataSet.addSeries(operation, dataList);
		}

		plotCreator.writePlotFile(new File(destDir, destFile.getFile().getPath()), AxisType.LINEAR, AxisType.LINEAR,
				RendererType.LINES, ChartDimensions.DEFAULT, dataRange, true, dataSet);
	}
}
