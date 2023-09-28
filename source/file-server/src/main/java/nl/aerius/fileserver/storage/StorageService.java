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
package nl.aerius.fileserver.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service interface for managing storage of files.
 */
public interface StorageService {

  /**
   * Stores the file.
   *
   * @param uuid unique identifier to store the file under
   * @param filename original name of the file
   * @param size size of the data
   * @param expires tag to set expiration of the file
   * @param in inputstream to the file data
   * @throws IOException
   */
  void putFile(String uuid, String filename, long size, String expires, InputStream in) throws IOException;

  /**
   * Returns the file path of the file with the given job.
   *
   * @param uuid unique identifier the file is stored by
   * @param filename original name of the file
   * @return absolute path to the file
   * @throws FileNotFoundException thrown when the file is unknown
   */
  String getFile(String uuid, String filename) throws FileNotFoundException;

  /**
   * @param sourceUuid The unique identifier the file to copy is stored by
   * @param destinationUuid The unique identifier to copy the file to
   * @param filename The original name of the file
   * @param expires The tag to set expiration of the file in the destination
   * @throws IOException
   */
  void copyFile(final String sourceUuid, final String destinationUuid, final String filename, final String expires) throws IOException;

  /**
   * Deletes the file.
   *
   * @param uuid unique identifier the file is stored by
   * @param filename original name of the file
   * @throws IOException in case delete failed. Not thrown when file not found
   */
  void deleteFile(String uuid, String filename) throws IOException;

  /**
   * Deletes all files under uuid
   *
   * @param uuid unique identifier the files are stored by
   * @throws IOException in case delete failed. Not thrown when file not found
   */
  void deleteFiles(String uuid) throws IOException;
}
