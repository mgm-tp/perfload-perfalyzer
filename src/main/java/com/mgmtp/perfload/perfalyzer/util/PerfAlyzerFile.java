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
package com.mgmtp.perfload.perfalyzer.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithExpectedSize;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.wildcardMatch;

/**
 * @author rnaegele
 */
public class PerfAlyzerFile implements Comparable<PerfAlyzerFile> {

	/**
	 * Regular expression that matches a part of a perfAlyzer file. File name parts are enclosed by
	 * square brackets, e. g. {@code [perfmon][cpu_X].out}.
	 */
	private static final Pattern PATTERN_FILE_NAME_PARTS = Pattern.compile("\\[([^]]+)\\]");
	private static final Pattern PATTERN_MARKER = Pattern.compile("\\{([^}]+)\\}");

	private final String path;
	private String extension;
	private final List<String> fileNameParts;
	private String marker;

	private PerfAlyzerFile(final String path, final String extension, final List<String> fileNameParts, final String marker) {
		this.path = path;
		this.extension = extension;
		this.fileNameParts = newArrayList(fileNameParts);
		this.marker = marker;
	}

	public static PerfAlyzerFile create(final File file) {
		checkState(!file.isAbsolute(), "File must not be absolute: %s", file);

		String fileName = file.getName();

		List<String> parts = newArrayListWithExpectedSize(3);
		List<String> partsWithBrackets = newArrayListWithExpectedSize(3);

		Matcher m = PATTERN_FILE_NAME_PARTS.matcher(fileName);
		while (m.find()) {
			partsWithBrackets.add(m.group());
			parts.add(m.group(1));
		}
		m = PATTERN_MARKER.matcher(fileName);
		String marker = m.find() ? m.group(1) : null;

		return new PerfAlyzerFile(FilenameUtils.getPath(file.getPath()), getExtension(fileName), parts, marker);
	}

	public PerfAlyzerFile copy() {
		return new PerfAlyzerFile(path, extension, newArrayList(fileNameParts), marker);
	}

	/**
	 * @return the file
	 */
	public File getFile() {
		String baseName = '[' + on("][").join(fileNameParts) + ']';
		if (marker != null) {
			baseName += "{" + marker + '}';
		}
		return new File(path, baseName + (extension.equals(StringUtils.EMPTY) ? StringUtils.EMPTY : '.' + extension));
	}

	public Path getPath() {
		return getFile().toPath();
	}

	public PerfAlyzerFile addFileNamePart(final String part) {
		if (part != null) {
			fileNameParts.add(part);
		}
		return this;
	}

	public PerfAlyzerFile addFileNamePart(final Iterable<String> parts) {
		for (String part : parts) {
			fileNameParts.add(part);
		}
		return this;
	}

	public PerfAlyzerFile addFileNamePart(final int index, final String part) {
		fileNameParts.add(index, part);
		return this;
	}

	public PerfAlyzerFile removeFileNamePart(final int index) {
		fileNameParts.remove(index);
		return this;
	}

	public PerfAlyzerFile removeFileNamePart(final String wildcard) {
		for (Iterator<String> it = fileNameParts.iterator(); it.hasNext();) {
			if (wildcardMatch(it.next(), wildcard)) {
				it.remove();
				break;
			}
		}
		return this;
	}

	public PerfAlyzerFile setExtension(final String extension) {
		this.extension = extension;
		return this;
	}

	/**
	 * @return the fileNameParts
	 */
	public List<String> getFileNameParts() {
		return fileNameParts;
	}

	/**
	 * @return the marker
	 */
	public String getMarker() {
		return marker;
	}

	public void setMarker(final String marker) {
		this.marker = marker;
	}

	@Override
	public int compareTo(final PerfAlyzerFile o) {
		return getFile().compareTo(o.getFile());
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
