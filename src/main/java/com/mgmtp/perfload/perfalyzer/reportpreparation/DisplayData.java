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
package com.mgmtp.perfload.perfalyzer.reportpreparation;

import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;

/**
 * @author rnaegele
 */
public class DisplayData {

	private final Pattern pattern;
	private final String unitX;
	private final List<String> unitYList;

	public DisplayData(final Pattern pattern, final String unitX, final List<String> unitYList) {
		this.pattern = pattern;
		this.unitX = unitX;
		this.unitYList = ImmutableList.copyOf(unitYList);
	}

	/**
	 * @return the pattern
	 */
	public Pattern getPattern() {
		return pattern;
	}

	/**
	 * @return the unitX
	 */
	public String getUnitX() {
		return unitX;
	}

	/**
	 * @return the unitY
	 */
	public String getUnitY() {
		return unitYList.get(0);
	}

	/**
	 * @return the unitYList
	 */
	public List<String> getUnitYList() {
		return unitYList;
	}
}
