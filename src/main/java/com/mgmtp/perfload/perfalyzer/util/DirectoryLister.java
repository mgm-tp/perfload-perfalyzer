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
package com.mgmtp.perfload.perfalyzer.util;

import static com.mgmtp.perfload.perfalyzer.util.IoUtilities.makeRelative;
import static java.nio.file.Files.walk;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Recursively walks a directory adding files in the tree to a list. The files in the list will
 * be relative to the specified base directory.
 *
 * @author rnaegele
 */
public class DirectoryLister {

	private DirectoryLister() {
	}

	public static List<File> listFiles(final File baseDir) {
		try (Stream<Path> stream = walk(baseDir.toPath())) {
			return stream.filter(Files::isRegularFile)
					.filter(path -> path.toFile().length() > 0L)
					.map(path -> makeRelative(baseDir, path.toFile())).collect(toList());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static List<PerfAlyzerFile> listPerfAlyzerFiles(final File baseDir) {
		return listPerfAlyzerFiles(baseDir, null);
	}

	public static List<PerfAlyzerFile> listAllPerfAlyzerFiles(final File baseDir) {
		try (Stream<Path> stream = walk(baseDir.toPath())) {
			return stream.filter(Files::isRegularFile)
					.filter(path -> path.toFile().length() > 0L)
					.map(path -> PerfAlyzerFile.create(makeRelative(baseDir, path.toFile())))
					.collect(toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<PerfAlyzerFile> listPerfAlyzerFiles(final File baseDir, final Marker marker) {
		try (Stream<Path> stream = walk(baseDir.toPath())) {
			Stream<PerfAlyzerFile> fileStream = stream.filter(Files::isRegularFile)
					.filter(path -> path.toFile().length() > 0L)
					.map(path -> PerfAlyzerFile.create(makeRelative(baseDir, path.toFile())));
			fileStream = marker == null
					? fileStream.filter(perfAlyzerFile -> perfAlyzerFile.getMarker() == null)
					: fileStream.filter(perfAlyzerFile -> marker.getName().equals(perfAlyzerFile.getMarker()));
			return fileStream.collect(toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
