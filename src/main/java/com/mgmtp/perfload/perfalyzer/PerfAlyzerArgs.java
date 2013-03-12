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
package com.mgmtp.perfload.perfalyzer;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

/**
 * @author ctchinda
 */
public class PerfAlyzerArgs {

	@Parameter(names = "-i", required = true, description = "The input directory", converter = FileConverter.class)
	File inputDir;

	@Parameter(names = "-o", description = "The output base directory", converter = FileConverter.class)
	File outputDir = new File("output");

	@Parameter(names = "-n", arity = 1, description = "Perform normalization")
	boolean normalization = true;

	@Parameter(names = "-b", arity = 1, description = "Perform binning")
	boolean binning = true;

	@Parameter(names = "-r", arity = 1, description = "Perform report preparation")
	boolean reportPreparation = true;

	@Parameter(names = "-u", arity = 1, description = "Unzip test archives")
	boolean unzip = true;
}
