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

import com.google.common.io.Files;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Utility class for managing channels. A channel manager is always based on a destination directory
 * and uses a {@link FileNamingStrategy} in order to create files it opens channels for.
 *
 * @author rnaegele
 */
public class ChannelManager implements AutoCloseable, Closeable {

	private final File destDir;
	private final FileNamingStrategy fileNamingStrategy;
	private final Map<String, ChannelStreamContainer> cscMap = newHashMapWithExpectedSize(2);

	/**
	 * @param destDir
	 *            the destination directory
	 * @param fileNamingStrategy
	 *            the file naming strategy
	 */
	public ChannelManager(final File destDir, final FileNamingStrategy fileNamingStrategy) {
		this.destDir = destDir;
		this.fileNamingStrategy = fileNamingStrategy;
	}

	/**
	 * Returns a channel with the specified key. Channels are cached internally under the specified
	 * key. If this method is called the first time for the specified key, a new channel is opened
	 * and cached. Directories are created as necessary.
	 *
	 * @param channelKey
	 *            the channel key
	 * @return the channel
	 */
	public WritableByteChannel getChannel(final String channelKey) throws IOException {
		ChannelStreamContainer csc = cscMap.get(channelKey);
		if (csc == null) {
			csc = new ChannelStreamContainer();
			cscMap.put(channelKey, csc);
			File globalDestFile = new File(destDir, fileNamingStrategy.createFileName(channelKey).getFile().getPath());
			Files.createParentDirs(globalDestFile);
			FileOutputStream fos = new FileOutputStream(globalDestFile);
			csc.os = fos;
			csc.ch = fos.getChannel();
		}
		return csc.ch;
	}

	/**
	 * Closes all open streams associated with cached channels.
	 */
	@Override
	public void close() {
		for (ChannelStreamContainer csc : cscMap.values()) {
			closeQuietly(csc.os);
		}
	}

	static class ChannelStreamContainer {
		OutputStream os;
		WritableByteChannel ch;
	}
}
