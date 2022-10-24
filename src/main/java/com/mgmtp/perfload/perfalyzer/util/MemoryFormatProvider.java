package com.mgmtp.perfload.perfalyzer.util;

import java.util.Locale;

public class MemoryFormatProvider {

	final private MemoryFormat memoryFormat;

	public MemoryFormatProvider(final Locale locale) {
		this.memoryFormat = new MemoryFormat(locale);
	}

	public MemoryFormat get() {
		return memoryFormat;
	}
}