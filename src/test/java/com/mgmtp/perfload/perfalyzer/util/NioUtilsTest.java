package com.mgmtp.perfload.perfalyzer.util;

import static java.lang.System.out;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.testng.annotations.Test;

/**
 * @author rnaegele
 */
public class NioUtilsTest {

	@Test
	public void testLines() throws IOException {
		NioUtils.lines(Paths.get("pom.xml"), StandardCharsets.UTF_8).forEach(out::println);
	}
}
