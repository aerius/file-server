/*
 * Copyright the State of the Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package nl.overheid.aerius.fileserver.util;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Util to validate the uuid and filename passed as path parameters to the server.
 */
public final class FilenameUtil {

  // Length of standard UUID
  private static final int MAX_UUID_LENGTH = 36;
  // Length of job key: character prefixed UUID, and dashes removed from UUID.
  private static final int MAX_SPECIFIC_UUID_LENGTH = 32 + 1;
  private static final Pattern UUID_PATTERN = Pattern.compile("[a-fA-F\\d\\-]*");
  /**
   * Arbitrary maximum length of a filename. To avoid abuse.
   */
  private static final int MAX_FILENAME_LENGTH = 256;

  private FilenameUtil() {
    // Util class
  }

  public static void validateParameters(final String uuid, final String filename) throws IOException {
    if (!FilenameUtil.validateUuid(uuid) || !FilenameUtil.validateFilename(filename)) {
      throw new IOException("Invalid parameters");
    }
  }

  /**
   * Checks if the uuid has a valid pattern.
   *
   * @param uuid uuid to check
   * @return true if valid uuid
   */
  public static boolean validateUuid(final String uuid) {
    final int length = uuid.length();

    return (length == MAX_UUID_LENGTH && UUID_PATTERN.matcher(uuid).matches())
        || (length == MAX_SPECIFIC_UUID_LENGTH && UUID_PATTERN.matcher(uuid.substring(1)).matches());
  }

  /**
   * Checks if the filename is not null and not too long.
   *
   * @param filename filename to check
   * @return true if valid filename
   */
  public static boolean validateFilename(final String filename) {
    return filename != null && filename.length() < MAX_FILENAME_LENGTH;
  }
}
