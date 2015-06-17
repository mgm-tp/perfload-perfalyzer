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
package com.mgmtp.perfload.perfalyzer.reportpreparation;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.io.Files;
import com.google.common.io.LineReader;
import com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants;
import com.mgmtp.perfload.perfalyzer.reportpreparation.NumberDataSet.SeriesPoint;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator.AxisType;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator.ChartDimensions;
import com.mgmtp.perfload.perfalyzer.reportpreparation.PlotCreator.RendererType;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.lang3.text.StrTokenizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.io.Files.createParentDirs;
import static com.google.common.io.Files.newReader;
import static com.google.common.io.Files.readLines;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.writeLineToChannel;
import static com.mgmtp.perfload.perfalyzer.util.PerfAlyzerUtils.readDataFile;
import static com.mgmtp.perfload.perfalyzer.util.StrBuilderUtils.appendEscapedAndQuoted;
import static java.lang.Math.min;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.writeLines;
import static org.apache.commons.lang3.StringUtils.substringAfter;

/**
 * @author rnaegele
 */
public class MeasuringReportPreparationStrategy extends AbstractReportPreparationStrategy {

	private final int maxHistoryItems;

	public MeasuringReportPreparationStrategy(final NumberFormat intNumberFormat,
			final NumberFormat floatNumberFormat, final List<DisplayData> displayDataList,
			final ResourceBundle resourceBundle, final PlotCreator plotCreator, final TestMetadata testMetadata,
			final DataRange dataRange, final int maxHistoryItems) {
		super(intNumberFormat, floatNumberFormat, displayDataList, resourceBundle, plotCreator, testMetadata, dataRange);
		this.maxHistoryItems = maxHistoryItems;
	}

	@Override
	public void processFiles(final File sourceDir, final File destDir, final List<PerfAlyzerFile> files) throws IOException {
		List<MeasuringHandler> handlers = ImmutableList.of(
				new ByOperationHandler(sourceDir, destDir),
				new ByOperationAggregatedHandler(sourceDir, destDir),
				new ByOperationExecutionsPerTimeHandler(sourceDir, destDir, "execMin"),
				new ByOperationExecutionsPerTimeHandler(sourceDir, destDir, "exec10Min"),
				new DistributionHandler(sourceDir, destDir),
				new QuantilesHandler(sourceDir, destDir),
				new RequestsPerPeriodByOperationHandler(sourceDir, destDir, PerfAlyzerConstants.BIN_SIZE_MILLIS_1_MINUTE),
				new RequestsPerPeriodByOperationHandler(sourceDir, destDir, PerfAlyzerConstants.BIN_SIZE_MILLIS_1_SECOND),
				new ErrorsHandler(sourceDir, destDir)
		);

		for (PerfAlyzerFile f : files) {
			log.info("Processing file '{}'...", f);

			for (MeasuringHandler handler : handlers) {
				handler.processFile(f);
			}
		}

		for (MeasuringHandler handler : handlers) {
			handler.finishProcessing();
		}
	}

	/*
	 * Abstract base class for measuring file handlers.
	 */
	abstract class MeasuringHandler {
		protected final File sourceDir;
		protected final File destDir;

		public MeasuringHandler(final File sourceDir, final File destDir) {
			this.sourceDir = sourceDir;
			this.destDir = destDir;
		}

		abstract void processFile(final PerfAlyzerFile paFile) throws IOException;

		abstract void finishProcessing() throws IOException;
	}

	/**
	 * Copies the quantiles files.
	 * <p>
	 * <pre>
	 * Input:  [measuring][&lt;operation&gt;][quantiles].csv
	 * Output: [measuring][&lt;operation&gt;][distribution].csv
	 * </pre>
	 */
	class QuantilesHandler extends MeasuringHandler {
		ListMultimap<String, PerfAlyzerFile> byTypeMultimap = ArrayListMultimap.create();
		StrTokenizer tokenizer = StrTokenizer.getCSVInstance();

		public QuantilesHandler(final File sourceDir, final File destDir) {
			super(sourceDir, destDir);
			tokenizer.setDelimiterChar(DELIMITER);
		}

		@Override
		void processFile(final PerfAlyzerFile f) throws IOException {
			List<String> fileNameParts = f.getFileNameParts();
			if (fileNameParts.size() == 3 && "quantiles".equals(fileNameParts.get(2))) {
				// Simply copy the file renaming it in order to align it to the plot file
				File destFile = new File(destDir, f.copy().removeFileNamePart("quantiles").addFileNamePart("distribution").getFile().getPath());
				copyFile(new File(sourceDir, f.getFile().getPath()), destFile);
			}
		}

		@Override
		void finishProcessing() throws IOException {
			// no-op
		}
	}

	/**
	 * Creates response time distribution plots.
	 * <p>
	 * <pre>
	 * Input:  [measuring][&lt;operation&gt;][distribution_&lt;index&gt;].csv
	 * Output: [measuring][&lt;operation&gt;][distribution].png
	 * </pre>
	 */
	class DistributionHandler extends MeasuringHandler {

		ListMultimap<String, PerfAlyzerFile> byTypeMultimap = ArrayListMultimap.create();

		public DistributionHandler(final File sourceDir, final File destDir) {
			super(sourceDir, destDir);
		}

		@Override
		void processFile(final PerfAlyzerFile f) throws IOException {
			List<String> fileNameParts = f.getFileNameParts();
			if (fileNameParts.size() == 3 && fileNameParts.get(2).startsWith("distribution_")) {
				// key is the plot file name, i. e. we group effectively by operation
				String key = f.copy().removeFileNamePart("distribution_*").addFileNamePart("distribution").setExtension("png").getFile().getPath();
				byTypeMultimap.put(key, f);
			}
		}

		@Override
		void finishProcessing() throws IOException {
			for (String key : byTypeMultimap.keySet()) {
				NumberDataSet dataSet = new NumberDataSet();
				File destFile = new File(destDir, key);

				// one file for each request type
				for (PerfAlyzerFile f : byTypeMultimap.get(key)) {
					File file = new File(sourceDir, f.getFile().getPath());
					List<SeriesPoint> dataList = readDataFile(file, Charsets.UTF_8, intNumberFormat);

					// extract three-digit mapping key as series name which is then shown on the chart's legend
					String mappingKey = substringAfter(f.getFileNameParts().get(2), "_");
					dataSet.addSeries(mappingKey, dataList);
				}

				plotCreator.writePlotFile(destFile, AxisType.LOGARITHMIC, AxisType.LOGARITHMIC, RendererType.SHAPES,
						ChartDimensions.LARGE, null, false, dataSet);
			}
		}
	}

	/**
	 * Binned response times plots.
	 * <p>
	 * <pre>
	 * Input:  [measuring][&lt;operation&gt;][executions].csv
	 * Output: [measuring][executions].png
	 * </pre>
	 */
	class ByOperationHandler extends MeasuringHandler {

		ListMultimap<String, PerfAlyzerFile> byOperationMap = ArrayListMultimap.create();

		public ByOperationHandler(final File sourceDir, final File destDir) {
			super(sourceDir, destDir);
		}

		@Override
		void processFile(final PerfAlyzerFile f) {
			List<String> fileNameParts = f.getFileNameParts();
			if (fileNameParts.size() == 3 && "executions".equals(fileNameParts.get(2))) {
				String operation = fileNameParts.get(1);
				byOperationMap.put(operation, f);
			}
		}

		@Override
		void finishProcessing() throws IOException {
			NumberDataSet dataSet = new NumberDataSet();

			PerfAlyzerFile destFile = null;
			for (String key : byOperationMap.keySet()) {
				for (PerfAlyzerFile f : byOperationMap.get(key)) {
					PerfAlyzerFile tmp = f.copy().removeFileNamePart(1).setExtension("png");
					if (destFile == null) {
						destFile = tmp;
					} else {
						// safety check
						checkState(destFile.getFile().equals(tmp.getFile()));
					}
					File file = new File(sourceDir, f.getFile().getPath());
					List<SeriesPoint> dataList = readDataFile(file, Charsets.UTF_8, intNumberFormat);
					dataSet.addSeries(key, dataList);
				}
			}

			plotCreator.writePlotFile(new File(destDir, destFile.getFile().getPath()), AxisType.LINEAR, AxisType.LINEAR, RendererType.LINES,
					ChartDimensions.WIDE, dataRange, false, dataSet);
		}
	}

	/**
	 * Binned executions by time plots.
	 * <p>
	 * <pre>
	 * Input:  [measuring][&lt;operation&gt;][&lt;fileNamePart&gt;].csv
	 * Output: [measuring][&lt;fileNamePart&gt;].png
	 * </pre>
	 */
	class ByOperationExecutionsPerTimeHandler extends MeasuringHandler {

		ListMultimap<String, PerfAlyzerFile> byOperationMap = ArrayListMultimap.create();
		private final String fileNamePart;

		public ByOperationExecutionsPerTimeHandler(final File sourceDir, final File destDir, final String fileNamePart) {
			super(sourceDir, destDir);
			this.fileNamePart = fileNamePart;
		}

		@Override
		void processFile(final PerfAlyzerFile f) {
			List<String> fileNameParts = f.getFileNameParts();
			if (fileNameParts.size() == 3 && fileNameParts.get(2).equals(fileNamePart)) {
				String operation = fileNameParts.get(1);
				byOperationMap.put(operation, f);
			}
		}

		@Override
		void finishProcessing() throws IOException {
			NumberDataSet dataSet = new NumberDataSet();

			PerfAlyzerFile destFile = null;
			for (String key : byOperationMap.keySet()) {
				for (PerfAlyzerFile f : byOperationMap.get(key)) {
					PerfAlyzerFile tmp = f.copy().removeFileNamePart(1).setExtension("png");
					if (destFile == null) {
						destFile = tmp;
					} else {
						// safety check
						checkState(destFile.getFile().equals(tmp.getFile()));
					}
					File file = new File(sourceDir, f.getFile().getPath());
					List<SeriesPoint> dataList = readDataFile(file, Charsets.UTF_8, intNumberFormat);
					dataSet.addSeries(key, dataList);
				}
			}

			plotCreator.writePlotFile(new File(destDir, destFile.getFile().getPath()), AxisType.LINEAR, AxisType.LINEAR, RendererType.LINES,
					ChartDimensions.WIDE, dataRange, false, dataSet);
		}
	}

	/**
	 * Creates response time csv files. The contents are considered for comparison, thus comparison
	 * files are updated as well.
	 * <p>
	 * <pre>
	 * Input:  [measuring][&lt;operation&gt;][aggregated].csv
	 * Output: [measuring][executions].csv
	 * </pre>
	 */
	class ByOperationAggregatedHandler extends MeasuringHandler {

		ListMultimap<String, PerfAlyzerFile> byOperationAggregatedMap = ArrayListMultimap.create();

		public ByOperationAggregatedHandler(final File sourceDir, final File destDir) {
			super(sourceDir, destDir);
		}

		@Override
		void processFile(final PerfAlyzerFile f) {
			List<String> fileNameParts = f.getFileNameParts();
			if (fileNameParts.size() == 3 && "aggregated".equals(fileNameParts.get(2))) {
				String operation = fileNameParts.get(1);
				byOperationAggregatedMap.put(operation, f);
			}
		}

		private File createDestFile(final File parentDir, final PerfAlyzerFile sourceFile, final String baseDir, final boolean dropOperationPart) {
			PerfAlyzerFile result = sourceFile.copy().removeFileNamePart(1);
			if (dropOperationPart) {
				result.removeFileNamePart(1);
			}
			result.addFileNamePart("executions");
			result.setExtension("csv");
			return new File(parentDir, new File(baseDir, result.getFile().getName()).getPath());
		}

		@Override
		void finishProcessing() throws IOException {
			// files in this set already have a header
			Set<File> overallFiles = newHashSet();

			for (String key : byOperationAggregatedMap.keySet()) {
				for (PerfAlyzerFile f : byOperationAggregatedMap.get(key)) {

					File destFile = createDestFile(destDir, f, "global", true);

					try (FileOutputStream fosOverall = new FileOutputStream(destFile, true)) {
						FileChannel overallChannel = fosOverall.getChannel();

						StrTokenizer tokenizer = StrTokenizer.getCSVInstance();
						tokenizer.setDelimiterChar(DELIMITER);

						File globalComparisonFile;
						try (Reader r = newReader(new File(sourceDir, f.getFile().getPath()), Charsets.UTF_8)) {
							createParentDirs(destFile);

							String operation = f.getFileNameParts().get(1);
							globalComparisonFile = createDestFile(destDir.getParentFile().getParentFile(), f, ".comparison", false);

							List<String> comparisonLines;

							// if file does not exist yet, simply write the header to it first
							StrBuilder sb = new StrBuilder(50);
							appendEscapedAndQuoted(sb, DELIMITER, "time", "numRequests", "numErrors", "minReqPerSec",
									"medianReqPerSec", "maxReqPerSec", "minReqPerMin", "medianReqPerMin", "maxReqPerMin",
									"minExecutionTime", "medianExecutionTime", "maxExecutionTime");
							String comparisonHeader = sb.toString();

							if (!globalComparisonFile.exists()) {
								createParentDirs(globalComparisonFile);
								comparisonLines = newArrayListWithCapacity(2);
								comparisonLines.add(comparisonHeader);
							} else {
								comparisonLines = readLines(globalComparisonFile, Charsets.UTF_8);
								// Update header in case it has changed
								comparisonLines.set(0, comparisonHeader);

								String line = comparisonLines.get(1);
								tokenizer.reset(line);

								String timestamp = tokenizer.nextToken();
								if (testMetadata.getTestStart().toString().equals(timestamp)) {
									// report already existed for this test, i. e. we remove the last entry to create it anew
									comparisonLines.remove(1);
								}
							}

							boolean isHeaderLine = true;

							// files contain only two lines
							LineReader lineReader = new LineReader(r);
							for (String line; (line = lineReader.readLine()) != null; ) {
								if (isHeaderLine) {
									isHeaderLine = false;

									if (!overallFiles.contains(destFile)) {
										overallFiles.add(destFile);
										writeLineToChannel(overallChannel, "\"operation\"" + DELIMITER + line, Charsets.UTF_8);
									}

								} else {

									tokenizer.reset(line);
									String[] tokens = tokenizer.getTokenArray();

									StrBuilder sbAggregated = new StrBuilder(line.length() + 10);
									appendEscapedAndQuoted(sbAggregated, DELIMITER, operation, tokens);
									writeLineToChannel(overallChannel, sbAggregated.toString(), Charsets.UTF_8);

									StrBuilder sbComparison = new StrBuilder(line.length() + 10);
									appendEscapedAndQuoted(sbComparison, DELIMITER, testMetadata.getTestStart().toString(), tokens);

									comparisonLines.add(1, sbComparison.toString());

									// apply max restriction, add 1 for header
									comparisonLines = comparisonLines.subList(0,
											min(maxHistoryItems + 1, comparisonLines.size()));

									writeLines(globalComparisonFile, Charsets.UTF_8.name(), comparisonLines);
								}
							}
						}

						File comparisonFile = new File(destDir, "comparison" + SystemUtils.FILE_SEPARATOR + globalComparisonFile.getName());
						// copy global file to this test's result files
						copyFile(globalComparisonFile, comparisonFile);
					}
				}
			}
		}
	}

	/**
	 * Creates requests per minute plots.
	 * <p>
	 * <pre>
	 * Input:  [measuring][&lt;operation&gt;][requests].csv
	 * Output: [measuring][requests].png
	 * </pre>
	 */
	class RequestsPerPeriodByOperationHandler extends MeasuringHandler {
		ListMultimap<String, PerfAlyzerFile> requestsByOperationMap = ArrayListMultimap.create();

		private final int binSize;

		public RequestsPerPeriodByOperationHandler(final File sourceDir, final File destDir, final int binSize) {
			super(sourceDir, destDir);
			this.binSize = binSize;
		}

		@Override
		void processFile(final PerfAlyzerFile f) {
			List<String> fileNameParts = f.getFileNameParts();
			if (fileNameParts.size() == 4 && "requests".equals(fileNameParts.get(2))
					&& fileNameParts.get(3).equals(String.valueOf(binSize))) {
				String operation = fileNameParts.get(1);
				requestsByOperationMap.put(operation, f);
			}
		}

		@Override
		void finishProcessing() throws IOException {
			NumberDataSet dataSet = new NumberDataSet();

			PerfAlyzerFile destFile = null;
			for (String key : requestsByOperationMap.keySet()) {
				for (PerfAlyzerFile f : requestsByOperationMap.get(key)) {
					PerfAlyzerFile tmp = f.copy().removeFileNamePart(1).setExtension("png");
					if (destFile == null) {
						destFile = tmp;
					} else {
						// safety check
						checkState(destFile.getFile().equals(tmp.getFile()));
					}
					File file = new File(sourceDir, f.getFile().getPath());
					List<SeriesPoint> dataList = readDataFile(file, Charsets.UTF_8, intNumberFormat);
					dataSet.addSeries(key, dataList);
				}
			}

			plotCreator.writePlotFile(new File(destDir, destFile.getFile().getPath()), AxisType.LINEAR, AxisType.LINEAR,
					RendererType.LINES, ChartDimensions.WIDE, dataRange, false, dataSet);
		}
	}

	/**
	 * Creates error files.
	 * <p>
	 * <pre>
	 * Input:  [measuring][&lt;operation&gt;][errorCount].csv, [measuring][&lt;operation&gt;][errorsByType].csv
	 * Output: [measuring][errors].png, [measuring][errors].csv
	 * </pre>
	 */
	class ErrorsHandler extends MeasuringHandler {
		ListMultimap<String, PerfAlyzerFile> errorCountsByOperationMultimap = ArrayListMultimap.create();
		List<PerfAlyzerFile> errorsByType = new ArrayList<>();

		public ErrorsHandler(final File sourceDir, final File destDir) {
			super(sourceDir, destDir);
		}

		@Override
		void processFile(final PerfAlyzerFile f) throws IOException {
			List<String> fileNameParts = f.getFileNameParts();
			if (fileNameParts.size() == 3 && "errorCount".equals(fileNameParts.get(2))) {
				errorCountsByOperationMultimap.put(fileNameParts.get(1), f);
			} else if (fileNameParts.size() == 3 && "errorsByType".equals(fileNameParts.get(2))) {
				errorsByType.add(f);
			}
		}

		@Override
		void finishProcessing() throws IOException {
			NumberDataSet dataSet = new NumberDataSet();

			PerfAlyzerFile destFile = null;
			for (String key : errorCountsByOperationMultimap.keySet()) {
				for (PerfAlyzerFile f : errorCountsByOperationMultimap.get(key)) {
					PerfAlyzerFile tmp = f.copy().removeFileNamePart(1).removeFileNamePart(1).addFileNamePart("errors").setExtension("png");
					if (destFile == null) {
						destFile = tmp;
					} else {
						// safety check
						checkState(destFile.getFile().equals(tmp.getFile()));
					}
					File file = new File(sourceDir, f.getFile().getPath());
					List<SeriesPoint> dataList = readDataFile(file, Charsets.UTF_8, intNumberFormat);
					dataSet.addSeries(key, dataList);
				}
			}

			File targetFile = new File(destDir, destFile.getFile().getPath());
			plotCreator.writePlotFile(targetFile, AxisType.LINEAR, AxisType.LINEAR, RendererType.LINES, ChartDimensions.DEFAULT,
					dataRange, false, dataSet);

			targetFile = new File(destDir, destFile.copy().setExtension("csv").getFile().getPath());
			try (FileOutputStream fos = new FileOutputStream(targetFile)) {
				FileChannel channel = fos.getChannel();

				boolean needsHeader = true;
				for (PerfAlyzerFile paf : errorsByType) {
					try (BufferedReader br = Files.newReader(new File(sourceDir, paf.getFile().getPath()), Charsets.UTF_8)) {

						String header = br.readLine();
						if (needsHeader) {
							writeLineToChannel(channel, "\"operation\"" + DELIMITER + header, Charsets.UTF_8);
							needsHeader = false;
						}

						String operation = paf.getFileNameParts().get(1);
						for (String line; (line = br.readLine()) != null; ) {
							writeLineToChannel(channel, "\"" + operation + "\"" + DELIMITER + line, Charsets.UTF_8);
						}
					}
				}
			}
		}
	}
}
