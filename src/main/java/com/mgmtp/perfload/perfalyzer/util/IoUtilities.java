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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.get;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;

import com.google.common.io.CharStreams;
import com.google.common.io.Files;

/**
 * Utility class for IO operations.
 * 
 * @author rnaegele, ctchinda
 */
public class IoUtilities {

	private static final int BUFFER_SIZE = 64 * 1024;

	/**
	 * Copies the content from one channel to another.
	 * 
	 * @param srcChannel
	 *            the source channel to copy from
	 * @param destChannel
	 *            the destination channel to copy to
	 */
	public static void copy(final ReadableByteChannel srcChannel, final WritableByteChannel destChannel) throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
		while (srcChannel.read(buffer) != -1) {
			// flip the buffer so it can be written to the destination channel
			buffer.flip();

			// write to the destination channel
			destChannel.write(buffer);

			// If partial transfer, shift remainder down so it does not get lost
			// If buffer is empty, this is the same as calling clear()
			buffer.compact();
		}

		// EOF will leave buffer in fill state
		buffer.flip();

		// make sure the buffer is fully drained
		while (buffer.hasRemaining()) {
			destChannel.write(buffer);
		}
	}

	/**
	 * Merges a list of files into a single one by appending the contents of each file. If
	 * {@code headerLines} is greater than zero, the header from the first file is written to the
	 * destination file. The same number of lines is skipped in all other file, i. e. all files are
	 * expected to have the same header.
	 * 
	 * @param sourceFiles
	 *            the list of source files
	 * @param destFile
	 *            the destination file
	 * @param headerLines
	 *            the number of header lines
	 * @param charset
	 *            the character set to use
	 */
	public static void merge(final File sourceDir, final List<PerfAlyzerFile> sourceFiles, final File destFile,
			final int headerLines, final Charset charset) throws IOException {
		Files.createParentDirs(destFile);

		// simply copy the first file
		copyFile(new File(sourceDir, get(sourceFiles, 0).getFile().getPath()), destFile);

		if (sourceFiles.size() > 1) {
			// append all other files skipping headers
			try (Writer w = Files.newWriter(destFile, charset)) {
				for (PerfAlyzerFile paf : sourceFiles.subList(1, sourceFiles.size())) {
					try (BufferedReader br = Files.newReader(new File(sourceDir, paf.getFile().getPath()), charset)) {

						// skip headers
						for (int i = 0; i < headerLines; ++i) {
							br.readLine();
						}

						// copy the rest
						CharStreams.copy(br, w);
					}
				}
			}
		}
	}

	/**
	 * Unzips a zip file.
	 * 
	 * @param zip
	 *            the zip file
	 * @param destDir
	 *            the destination directory (will be created if non-existent)
	 */
	public static void unzip(final ZipFile zip, final File destDir) throws IOException {
		if (!destDir.exists()) {
			destDir.mkdir();
		}

		for (Enumeration<? extends ZipEntry> zipEntryEnum = zip.entries(); zipEntryEnum.hasMoreElements();) {
			ZipEntry zipEntry = zipEntryEnum.nextElement();
			extractEntry(zip, zipEntry, destDir);
		}
	}

	private static void extractEntry(final ZipFile zf, final ZipEntry entry, final File destDir) throws IOException {
		File file = new File(destDir, entry.getName());

		if (entry.isDirectory()) {
			file.mkdirs();
		} else {
			new File(file.getParent()).mkdirs();

			try (InputStream is = zf.getInputStream(entry); FileOutputStream os = new FileOutputStream(file)) {
				copy(Channels.newChannel(is), os.getChannel());
			}
			// preserve modification time; must be set after the stream is closed
			file.setLastModified(entry.getTime());
		}
	}

	public static void writeToChannel(final WritableByteChannel destChannel, final ByteBuffer buffer) throws IOException {
		// write to the destination channel
		destChannel.write(buffer);

		// If partial transfer, shift remainder down so it does not get lost
		// If buffer is empty, this is the same as calling clear()
		buffer.compact();

		// EOF will leave buffer in fill state
		buffer.flip();

		// make sure the buffer is fully drained
		while (buffer.hasRemaining()) {
			destChannel.write(buffer);
		}
	}

	public static void writeLineToChannel(final WritableByteChannel destChannel, final String line, final Charset charset)
			throws IOException {
		String tmpLine = line.endsWith(SystemUtils.LINE_SEPARATOR) ? line : line + SystemUtils.LINE_SEPARATOR;
		CharBuffer buffer = CharBuffer.wrap(tmpLine);
		CharsetEncoder encoder = charset.newEncoder();
		ByteBuffer bb = encoder.encode(buffer);
		writeToChannel(destChannel, bb);
	}

	/**
	 * Reads the last line of the specified file.
	 * 
	 * @param file
	 *            the file
	 * @param charset
	 *            the charset
	 */
	public static String readLastLine(final File file, final Charset charset) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			long length = raf.length() - 1;

			ByteArrayOutputStream baos = new ByteArrayOutputStream(80);

			for (long pos = length; pos != -1; --pos) { // we start from the end of the file
				raf.seek(pos);

				int readByte = raf.readByte();

				if (readByte == 10) { // this is the case if the file ends with a line-break
					if (pos == length) {
						continue;
					}
					break;
				} else if (readByte == 13) { // this is the case if the file ends with a line-break (Windows only)
					if (pos == length - 1) {
						continue;
					}
					break;
				}

				baos.write(readByte);
			}

			byte[] bytes = baos.toByteArray();

			// reverse array because it was filled backwards
			ArrayUtils.reverse(bytes);

			// turn into string respecting the charset
			return new String(bytes, charset);
		}
	}

	/**
	 * Normalizes the specified file using {@link FilenameUtils#normalize(String)}. If {@code file}
	 * is a directory, the normalized result always ends with the file separator.
	 * 
	 * @param file
	 *            the file to normalize
	 * @return the normalized file path
	 */
	public static String computeNormalizedPath(final File file) throws IOException {
		String normalizedPath = normalize(file.getCanonicalPath());
		if (file.isDirectory()) {
			normalizedPath = ensureEndsWithFileSeparator(normalizedPath);
		}
		return normalizedPath;
	}

	/**
	 * Returns the specified path appending a file separator is necessary.
	 * 
	 * @param path
	 *            the path
	 * @return the path ending with a file separator
	 */
	public static String ensureEndsWithFileSeparator(final String path) {
		if (!path.endsWith(SystemUtils.FILE_SEPARATOR)) {
			return path + SystemUtils.FILE_SEPARATOR;
		}
		return path;
	}

	/**
	 * Turns the specified file into one relative to the specified parent directory.
	 * 
	 * @param parentDir
	 *            the parent directory
	 * @param file
	 *            the file to make relative
	 * @return the file relative to {@code parentDir}
	 * @throws IllegalStateException
	 *             if {@code parentDir} is neither a directory nor a parent directory of
	 *             {@code file}
	 */
	public static File makeRelative(final File parentDir, final File file) throws IOException {
		checkState(parentDir.isDirectory(), "'%s' is not a directory", parentDir);

		String normalizedBaseDirPath = computeNormalizedPath(parentDir);
		String normalizedFilePath = computeNormalizedPath(file);

		String relativeFilePath = substringAfter(normalizedFilePath, normalizedBaseDirPath);
		checkState(isNotEmpty(relativeFilePath), "'%s' is not the parent directory of '%s'", parentDir, file);

		return new File(relativeFilePath);
	}

	/**
	 * Creates a temporary directory named with a random UUID in the directory specified by the
	 * system property {@coe java.io.tmpdir}.
	 * 
	 * @return the directory
	 */
	public static File createTempDir() {
		File file = new File(SystemUtils.JAVA_IO_TMPDIR, UUID.randomUUID().toString());
		checkState(file.mkdir(), "Could not create temporary directory %s", file);
		return file;
	}
}
