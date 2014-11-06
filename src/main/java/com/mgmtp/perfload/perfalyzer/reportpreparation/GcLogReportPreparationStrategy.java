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
import com.mgmtp.perfload.perfalyzer.util.MemoryFormat;
import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;
import com.mgmtp.perfload.perfalyzer.util.TestMetadata;
import com.mgmtp.perfload.perfalyzer.util.TimestampNormalizer;
import com.tagtraum.perf.gcviewer.imp.DataReader;
import com.tagtraum.perf.gcviewer.imp.DataReaderFactory;
import com.tagtraum.perf.gcviewer.math.IntData;
import com.tagtraum.perf.gcviewer.model.GCEvent;
import com.tagtraum.perf.gcviewer.model.GCModel;
import org.apache.commons.lang3.text.StrBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.mgmtp.perfload.perfalyzer.constants.PerfAlyzerConstants.DELIMITER;
import static com.mgmtp.perfload.perfalyzer.util.StrBuilderUtils.appendEscapedAndQuoted;
import static org.apache.commons.io.FileUtils.writeLines;

/**
 * @author ctchinda
 */
public class GcLogReportPreparationStrategy extends AbstractReportPreparationStrategy {

	private final TimestampNormalizer timestampNormalizer;
	private final MemoryFormat memoryFormat;

	public GcLogReportPreparationStrategy(final NumberFormat intNumberFormat,
			final NumberFormat floatNumberFormat, final List<DisplayData> displayDataList,
			final ResourceBundle resourceBundle, final PlotCreator plotCreator, final TestMetadata testMetadata,
			final TimestampNormalizer timestampNormalizer, final MemoryFormat memoryFormat) {
		super(intNumberFormat, floatNumberFormat, displayDataList, resourceBundle, plotCreator, testMetadata);
		this.timestampNormalizer = timestampNormalizer;
		this.memoryFormat = memoryFormat;
	}

	@Override
	public void processFiles(final File sourceDir, final File destDir, final List<PerfAlyzerFile> files) throws IOException,
			ParseException {
		for (PerfAlyzerFile f : files) {
			log.info("Processing file '{}'...", f);

			GCModel origModel = null;
			try (InputStream is = new FileInputStream(new File(sourceDir, f.getFile().getPath()))) {
				DataReader dataReader = new DataReaderFactory().getDataReader(is);
				origModel = dataReader.read();
			} catch (IOException ex) {
				log.error("Error reading GC log file: " + f.getFile(), ex);
				continue;
			}

			GCModel model = new GCModel();
			model.setFormat(origModel.getFormat());

			for (Iterator<GCEvent> it = origModel.getGCEvents(); it.hasNext(); ) {
				GCEvent event = it.next();
				ZonedDateTime datestamp = event.getDatestamp();
				if (datestamp == null) {
					// we assume there are generally no datestamps if the first event does not have one
					log.error("Unsupported GC log format. Please activate date stamp logging (-XX:+PrintGCDateStamps for Oracle JDK).");
					break;
				}

				if (timestampNormalizer.isInRange(datestamp)) {
					model.add(event);
				} else {
					log.debug(event.toString());
				}
			}

			if (model.size() > 0) {
				NumberDataSet dataSetHeap = new NumberDataSet();
				NumberDataSet dataSetGcTimes = new NumberDataSet();

				for (Iterator<GCEvent> it = model.getGCEvents(); it.hasNext(); ) {
					GCEvent event = it.next();
					ZonedDateTime timestamp = event.getDatestamp();
					long seconds = timestampNormalizer.normalizeTimestamp(timestamp, 0L) / 1000;

					dataSetHeap.addSeriesPoint("total", new SeriesPoint(seconds, event.getTotal() / 1024));
					dataSetHeap.addSeriesPoint("used", new SeriesPoint(seconds, event.getPreUsed() / 1024));
					dataSetHeap.addSeriesPoint("used", new SeriesPoint(seconds, event.getPostUsed() / 1024));

					dataSetGcTimes.addSeriesPoint("time", new SeriesPoint(seconds, event.getPause() * 1000));
				}

				if (dataSetHeap.isEmpty() || dataSetGcTimes.isEmpty()) {
					continue;
				}

				plotCreator.writePlotFile(new File(destDir, f.copy().setExtension("png").getFile().getPath()), AxisType.LINEAR,
						AxisType.LINEAR, RendererType.LINES, ChartDimensions.WIDE, dataSetHeap, dataSetGcTimes);

				List<CharSequence> gcLines = newArrayListWithCapacity(2);
				writeHeader(gcLines);
				writeData(model, gcLines);
				writeLines(new File(destDir, f.copy().setExtension("csv").getFile().getPath()), Charsets.UTF_8.name(), gcLines);
			}
		}
	}

	private void writeHeader(final List<CharSequence> gcLines) {
		StrBuilder sb = new StrBuilder(500);
		appendEscapedAndQuoted(sb, DELIMITER, "total");
		appendEscapedAndQuoted(sb, DELIMITER, "tenured");
		appendEscapedAndQuoted(sb, DELIMITER, "young");
		appendEscapedAndQuoted(sb, DELIMITER, "permgen");
		appendEscapedAndQuoted(sb, DELIMITER, "freed memory");
		appendEscapedAndQuoted(sb, DELIMITER, "freed memory/min");
		appendEscapedAndQuoted(sb, DELIMITER, "total gc time");
		appendEscapedAndQuoted(sb, DELIMITER, "full gc time (min/max)");
		appendEscapedAndQuoted(sb, DELIMITER, "throughput");
		appendEscapedAndQuoted(sb, DELIMITER, "num full gc");
		appendEscapedAndQuoted(sb, DELIMITER, "full gc performance");
		appendEscapedAndQuoted(sb, DELIMITER, "gc performance");
		gcLines.add(sb);
	}

	private void writeData(final GCModel model, final List<CharSequence> gcLines) {
		StrBuilder sb = new StrBuilder(500);
		appendEscapedAndQuoted(sb, DELIMITER, memoryFormat.format(model.getFootprint()));
		appendEscapedAndQuoted(sb, DELIMITER, formatGcValue(model.getTenuredAllocatedSizes()));
		appendEscapedAndQuoted(sb, DELIMITER, formatGcValue(model.getYoungAllocatedSizes()));
		appendEscapedAndQuoted(sb, DELIMITER, formatGcValue(model.getPermAllocatedSizes()));
		appendEscapedAndQuoted(sb, DELIMITER, memoryFormat.format(model.getFreedMemory()));
		appendEscapedAndQuoted(sb, DELIMITER, memoryFormat.format(model.getFreedMemory() / model.getRunningTime() * 60.0));
		appendEscapedAndQuoted(sb, DELIMITER, model.hasCorrectTimestamp()
											  ? floatNumberFormat.format(model.getPause().getSum()) + " s"
											  : "n/a");
		appendEscapedAndQuoted(sb, DELIMITER, model.getFullGCPause().getN() > 0
											  ? intNumberFormat.format(model.getFullGCPause().getMin()) + " s / "
													  + intNumberFormat.format(model.getFullGCPause().getMax()) + " s"
											  : "n/a");
		appendEscapedAndQuoted(sb, DELIMITER, model.hasCorrectTimestamp()
											  ? floatNumberFormat.format(model.getThroughput()) + " %"
											  : "n/a");
		appendEscapedAndQuoted(sb, DELIMITER, model.getFullGCPause().getN() > 0
											  ? intNumberFormat.format(model.getFullGCPause().getN())
											  : "n/a");
		appendEscapedAndQuoted(sb, DELIMITER, model.getFullGCPause().getN() > 0
											  ? memoryFormat.format(model.getFreedMemoryByFullGC().getSum() / model.getFullGCPause().getSum()) + "/s"
											  : "n/a");
		appendEscapedAndQuoted(sb, DELIMITER, model.getGCPause().getN() > 0
											  ? memoryFormat.format(model.getFreedMemoryByGC().getSum() / model.getGCPause().getSum()) + "/s"
											  : "n/a");
		gcLines.add(sb);
	}

	private String formatGcValue(final IntData data) {
		return data.getN() > 0 ? memoryFormat.format(data.getMax()) : "n/a";
	}
}
