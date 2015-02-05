package com.mgmtp.perfload.perfalyzer.util;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author rnaegele
 */
public class NioUtils {

	private static final int BUFFER_SIZE = 8192;

	private NioUtils() {
	}

	public static Stream<String> lines(final Path path, final Charset charset) {
		final LineReader lr = new LineReader(path, charset);
		try {
			Stream<String> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(lr, Spliterator.ORDERED | Spliterator.NONNULL), false);
			return stream.onClose(() -> closeQuietly(lr));
		} catch (Error | Exception e) {
			try {
				lr.close();
			} catch (IOException suppressed) {
				try {
					e.addSuppressed(suppressed);
				} catch (Throwable ignore) {
					// ignore
				}
			}
			throw e;
		}
	}

	static class LineReader implements Iterator<String>, Closeable, AutoCloseable {
		private final Queue<String> lines;
		private final LineBuffer lineBuffer;
		private final Path path;
		private ByteChannel channel;
		private byte[] bytes;
		private ByteBuffer buffer;

		String nextLine;

		LineReader(final Path path, final Charset charset) {
			this.path = path;
			lines = new LinkedList<>();
			lineBuffer = new LineBuffer(charset) {
				@Override
				protected void handleLine(final String line) {
					lines.add(line);
				}
			};
		}

		@Override
		public boolean hasNext() {
			if (nextLine != null) {
				return true;
			} else {
				try {
					nextLine = readLine();
					return (nextLine != null);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}

		@Override
		public String next() {
			if (nextLine != null || hasNext()) {
				String line = nextLine;
				nextLine = null;
				return line;
			} else {
				throw new NoSuchElementException();
			}
		}

		private String readLine() throws IOException {
			if (channel == null) {
				channel = Files.newByteChannel(path, StandardOpenOption.READ);
				bytes = new byte[BUFFER_SIZE];
				buffer = ByteBuffer.wrap(bytes);
			}
			while (lines.peek() == null) {
				int numRead;
				while ((numRead = channel.read(buffer)) != -1) {
					lineBuffer.add(bytes, 0, numRead);
					buffer.clear();
				}
				if (numRead == -1) {
					lineBuffer.finish();
					break;
				}
			}
			return lines.poll();
		}

		@Override
		public void close() throws IOException {
			if (channel != null) {
				channel.close();
			}
		}
	}

	abstract static class LineBuffer {

		private static final byte LF = 10;
		private static final byte CR = 13;

		private final Charset charset;

		// whether a line ending with a CR is pending processing.
		private boolean foundCR;

		private final ByteArrayOutputStream lineStream = new ByteArrayOutputStream(256);

		LineBuffer(final Charset charset) {
			this.charset = charset;
		}

		/**
		 * Process additional characters from the stream. When a line separator
		 * is found the contents of the line and the line separator itself
		 * are passed to the abstract {@link #handleLine} method.
		 *
		 * @param buf the character buffer to process
		 * @param off the offset into the buffer
		 * @param len the number of characters to process
		 * @throws IOException if an I/O error occurs
		 * @see #finish
		 */
		protected void add(final byte[] buf, final int off, final int len) throws IOException {
			int pos = off;
			if (foundCR && len > 0) {
				// Last call to add ended with a CR; we can handle the line now.
				if (finishLine(buf[pos] == 10)) {
					pos++;
				}
			}

			int start = pos;
			for (int end = off + len; pos < end; pos++) {
				switch (buf[pos]) {
					case CR:
						lineStream.write(buf, start, pos - start);
						foundCR = true;
						if (pos + 1 < end) {
							if (finishLine(buf[pos + 1] == LF)) {
								pos++;
							}
						}
						start = pos + 1;
						break;
					case LF:
						lineStream.write(buf, start, pos - start);
						finishLine(true);
						start = pos + 1;
						break;
					default:
						// do nothing
				}
			}
			lineStream.write(buf, start, off + len - start);
		}

		/**
		 * Called when a line is complete.
		 */
		private boolean finishLine(final boolean foundLF) {
			String lineEnding = foundCR ? (foundLF ? "\r\n" : "\r") : (foundLF ? "\n" : "");
			handleLine(new String(lineStream.toByteArray(), charset));
			lineStream.reset();
			foundCR = false;
			return foundLF;
		}

		/**
		 * Subclasses must call this method after finishing character processing,
		 * in order to ensure that any unterminated line in the buffer is
		 * passed to {@link #handleLine}.
		 *
		 * @throws IOException if an I/O error occurs
		 */
		protected void finish() throws IOException {
			if (foundCR || lineStream.size() > 0) {
				finishLine(false);
			}
		}

		/**
		 * Called for each line found in the character data passed to {@link #add}.
		 *
		 * @param line       a line of text (possibly empty), without any line separators
		 * @throws IOException if an I/O error occurs
		 */
		protected abstract void handleLine(String line);
	}
}
