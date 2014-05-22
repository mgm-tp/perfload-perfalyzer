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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import com.mgmtp.perfload.perfalyzer.util.PerfAlyzerFile;

/**
 * @author ctchinda
 */
public class ReporterPreparator {
	private final File sourceDir;
	private final File destDir;
	private final ReportPreparationStrategy reportingStrategy;

	public ReporterPreparator(final File sourceDir, final File destDir, final ReportPreparationStrategy reportingStrategy) {
		this.sourceDir = sourceDir;
		this.destDir = destDir;
		this.reportingStrategy = reportingStrategy;
	}

	public void processFiles(final List<PerfAlyzerFile> files) throws IOException, ParseException {
		reportingStrategy.processFiles(sourceDir, destDir, files);
	}
}
