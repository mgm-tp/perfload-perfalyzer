/*
 * Copyright (c) 2022 mgm technology partners GmbH
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

import java.util.Locale;

/**
 * @author aneugebauer
 */

public class MemoryFormatProvider {

	final private MemoryFormat memoryFormat;

	public MemoryFormatProvider(final Locale locale) {
		this.memoryFormat = new MemoryFormat(locale);
	}

	public MemoryFormat get() {
		return memoryFormat;
	}
}