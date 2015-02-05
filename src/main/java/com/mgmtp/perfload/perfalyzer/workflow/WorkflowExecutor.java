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
package com.mgmtp.perfload.perfalyzer.workflow;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgmtp.perfload.perfalyzer.PerfAlyzerException;
import com.mgmtp.perfload.perfalyzer.util.Marker;

/**
 * @author rnaegele
 */
@Singleton
public class WorkflowExecutor {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Set<Workflow> workflows;
	private final ExecutorService execService;
	private final List<Marker> markers;

	@Inject
	public WorkflowExecutor(final Set<Workflow> workflows, final ExecutorService execService, final List<Marker> markers) {
		this.workflows = workflows;
		this.execService = execService;
		this.markers = markers;
	}

	public void executeNormalizationTasks(final File inputDir, final File outputDir) {
		List<Future<?>> futures = newArrayList();

		for (Workflow workflow : workflows) {
			log.info("Executing normalization tasks: {}", workflow);
			futures.addAll(executeTasks(workflow.getNormalizationTasks(inputDir, outputDir)));
		}

		waitForTasks(futures);
	}

	public void executeBinningTasks(final File inputDir, final File outputDir) {
		List<Future<?>> futures = newArrayList();

		for (Workflow workflow : workflows) {
			log.info("Executing binning tasks: {}", workflow);
			futures.addAll(executeTasks(workflow.getBinningTasks(inputDir, outputDir, null)));
			markers.forEach(marker -> futures.addAll(executeTasks(workflow.getBinningTasks(inputDir, outputDir, marker))));
		}

		waitForTasks(futures);
	}

	public void executeReportPreparationTasks(final File inputDir, final File outputDir) {
		List<Future<?>> futures = newArrayList();

		for (Workflow workflow : workflows) {
			log.info("Executing report preparation tasks: {}", workflow);
			futures.addAll(executeTasks(workflow.getReportPreparationTasks(inputDir, outputDir, null)));
			markers.forEach(marker -> futures.addAll(executeTasks(workflow.getReportPreparationTasks(inputDir, outputDir, marker))));
		}

		waitForTasks(futures);
	}

	private List<Future<?>> executeTasks(final List<Runnable> tasks) {
		return tasks.stream().map(execService::submit).collect(Collectors.toList());
	}

	private void waitForTasks(final List<Future<?>> futures) {
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new PerfAlyzerException("Workflow execution interrupted.", ex);
			} catch (ExecutionException ex) {
				throw new PerfAlyzerException("Error excuting workflow.", ex.getCause());
			}
		}
	}
}
