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
package nl.overheid.aerius.fileserver.storage;

import java.io.IOException;

import javax.servlet.ServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import nl.overheid.aerius.fileserver.util.FilenameUtil;

/**
 * Generic base Controller implementing all HTTP methods except for the GET request.
 */
public class FileController {

  private static final Logger LOG = LoggerFactory.getLogger(FileController.class);

  private static final String SLASH = "/";

  private static final String UUID = "{uuid}";
  private static final String FILENAME = "{filename}";
  protected static final String FILE_PATH = UUID + SLASH + FILENAME;

  protected final StorageService storageService;

  protected FileController(final StorageService storageService) {
    this.storageService = storageService;
  }

  /**
   * Stores a file.
   *
   * @param uuid uuid of file to put
   * @param filename filename of the file to put
   * @param expires optional expires header value should conform to RFC 1123
   * @param request HttpServletRequest to get the file from, which should be in the body
   */
  @PutMapping(value = FILE_PATH, consumes = {MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Void> putFile(final @PathVariable String uuid, final @PathVariable String filename,
      @RequestParam(name = "expires", required = false) final String expires, final ServletRequest request) {
    try {
      FilenameUtil.validateParameters(uuid, filename);
      LOG.debug("Put file {}/{}", uuid, filename);
      final String originalFilename = filename;

      storageService.putFile(uuid, originalFilename, request.getContentLength(), expires, request.getInputStream());
      return ResponseEntity.ok().build();
    } catch (final IOException e) {
      LOG.trace("IOException when trying to store a file", e);
    } catch (final RuntimeException e) {
      LOG.warn("RuntimeException when trying to store a file", e);
    }
    return ResponseEntity.badRequest().build();
  }

  /**
   * Deletes the file for the given uuid and filename. If the file could not be deleted the method still returns ok.
   *
   * @param uuid uuid of file to delete
   * @param filename filename of the file
   */
  @DeleteMapping(FILE_PATH)
  public ResponseEntity<Void> deleteFile(final @PathVariable String uuid, final @PathVariable String filename) {
    try {
      FilenameUtil.validateParameters(uuid, filename);
      LOG.debug("Delete file {}/{}", uuid, filename);
      storageService.deleteFile(uuid, filename);
    } catch (final IOException e) {
      LOG.trace("IOException when trying to delete a file", e);
    } catch (final RuntimeException e) {
      LOG.warn("RuntimeException when trying to delete a file", e);
    }
    return ResponseEntity.ok().build();
  }

  /**
   * Deletes the file for the given uuid. If the file could not be deleted the method still returns ok.
   *
   * @param uuid uuid of files to delete
   */
  @DeleteMapping(UUID)
  public ResponseEntity<Void> deleteFiles(final @PathVariable String uuid) {
    try {
      if (!FilenameUtil.validateUuid(uuid)) {
        throw new IOException("Invalid parameters");
      }
      LOG.debug("Delete files {}", uuid);
      storageService.deleteFiles(uuid);
    } catch (final IOException e) {
      LOG.trace("IOException when trying to delete a file", e);
    } catch (final RuntimeException e) {
      LOG.warn("RuntimeException when trying to delete a file", e);
    }
    return ResponseEntity.ok().build();
  }
}
