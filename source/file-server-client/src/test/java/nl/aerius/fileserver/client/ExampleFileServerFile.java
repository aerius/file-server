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
package nl.aerius.fileserver.client;

/**
 * Example definition of a file folder structure
 */
public enum ExampleFileServerFile implements FileServerFile {

  /**
   * Validation of imported data as json.
   */
  VALIDATION("validation.json"),
  /**
   * Free format where the actual filename is a parameter.
   */
  FREE_FORMAT("{filename}");

  private static final String UUID_PLACEHOLDER = "{uuid}";
  private static final String UUID_SLASH = UUID_PLACEHOLDER + "/";
  private final String filename;

  ExampleFileServerFile(final String filename) {
    this.filename = filename;
  }

  @Override
  public String getFilename() {
    return filename;
  }

  @Override
  public String uriTemplate() {
    return UUID_SLASH + filename;
  }
}
