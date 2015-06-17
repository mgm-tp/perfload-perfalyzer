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

import java.io.File;
import java.util.List;

import com.mgmtp.perfload.perfalyzer.util.Marker;

/**
 * @author rnaegele
 */
public interface Workflow {

	/**
	 * Returns a list of a lists of {@link Runnable} instances representing normalization tasks.
	 * Parallel execution is possible within each list of {@link Runnable}s contained in the result
	 * list, but the overall execution of these lists must be performed sequentially.
	 *
	 * @param inputDir  the input base directory where file to be normalized are expected
	 * @param outputDir the output base directory where normalized files are to be written to
	 * @param marker    A marker to be considered when selecting file for processing; may be null
	 * @return a list of tasks
	 */
	List<Runnable> getNormalizationTasks(final File inputDir, final File outputDir);

	/**
	 * Returns a list of a lists of {@link Runnable} instances representing binning tasks. Parallel
	 * execution is possible within each list of {@link Runnable}s contained in the result list, but
	 * the overall execution of these lists must be performed sequentially.
	 *
	 * @param inputDir  the input base directory where file to be normalized are expected
	 * @param outputDir the output base directory where binned files are to be written to
	 * @param marker    A marker to be considered when selecting file for processing; may be null
	 * @return a list of tasks
	 */
	List<Runnable> getBinningTasks(final File inputDir, final File outputDir, Marker marker);

	/**
	 * Returns a list of a lists of {@link Runnable} instances representing report preparation
	 * tasks. Parallel execution is possible within each list of {@link Runnable}s contained in the
	 * result list, but the overall execution of these lists must be performed sequentially.
	 *
	 * @param inputDir  the input base directory where file to be normalized are expected
	 * @param outputDir the output base directory where files for report preparation are to be written to
	 * @param marker    A marker to be considered when selecting file for processing; may be null
	 * @return a list of tasks
	 */
	List<Runnable> getReportPreparationTasks(final File inputDir, final File outputDir, Marker marker);
}
