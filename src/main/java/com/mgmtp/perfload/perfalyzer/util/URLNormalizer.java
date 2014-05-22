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

import java.net.URI;
import java.util.List;

/**
 * @author ctchinda
 */
public class URLNormalizer {

	private final int columnToTransform;

	private final char characterToReplace;

	private final char characterToReplaceWith;

	/**
 *
 */
	public URLNormalizer() {
		this('/', '.', 11);
	}

	public URLNormalizer(final char characterToReplace, final char characterToReplaceWith, final int columnToTransform) {
		this.columnToTransform = columnToTransform;
		this.characterToReplace = characterToReplace;
		this.characterToReplaceWith = characterToReplaceWith;
	}

	public List<String> normalizeUrl(final List<String> columns) {
		StringBuilder transformerBuffer = new StringBuilder();
		transformerBuffer.append(URI.create(columns.get(columnToTransform)).getPath());
		if (transformerBuffer.charAt(0) == '/') {
			transformerBuffer.deleteCharAt(0);
		}
		String outputString = transformerBuffer.toString().replace(characterToReplace, characterToReplaceWith);
		columns.set(columnToTransform, outputString);
		return columns;

	}
}
