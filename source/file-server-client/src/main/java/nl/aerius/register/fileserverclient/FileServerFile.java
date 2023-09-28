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
package nl.aerius.register.fileserverclient;

import java.util.UUID;

/**
 * Template enum for files to be stored on the file server.
 */
public enum FileServerFile {

  /**
   * Format to use when only UUID is needed. For example to delete all files related to the UUID in case of temporary files.
   */
  ALL(""),
  /**
   * Validation of imported data as json.
   */
  VALIDATION("validation.json"),
  /**
   * Summary of imported data obtained from Connect,  as json.
   */
  SUMMARY("summary.json"),
  /**
   * Free format where the actual filename is a parameter.
   */
  FREE_FORMAT("{filename}");

  private static final String UUID_PLACEHOLDER = "{uuid}";
  private static final String UUID_SLASH = UUID_PLACEHOLDER + "/";
  private final String filename;

  private FileServerFile(final String filename) {
    this.filename = filename;
  }

  /**
   * Creates a unique id with the given prefix prepended to the id.
   *
   * @param prefix string to prefix
   * @return unique id
   */
  public static String createId(final String prefix) {
    return prefix + UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * @return Gets the filename as is used on the fileserver.
   */
  public String getFilename() {
    return filename;
  }

  /**
   * @return Gets the template of the uuid and filename pattern.
   */
  public String uriTemplate() {
    return this == ALL ? UUID_PLACEHOLDER : (UUID_SLASH + filename);
  }
}
