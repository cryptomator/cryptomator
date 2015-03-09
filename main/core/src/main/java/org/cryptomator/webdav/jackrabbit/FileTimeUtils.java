/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

final class FileTimeUtils {

	private FileTimeUtils() {
		throw new IllegalStateException("not instantiable");
	}

	static String toRfc1123String(FileTime time) {
		final Temporal date = OffsetDateTime.ofInstant(time.toInstant(), ZoneOffset.UTC);
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(date);
	}

	static FileTime fromRfc1123String(String string) {
		final Instant instant = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(string));
		return FileTime.from(instant);
	}

}
