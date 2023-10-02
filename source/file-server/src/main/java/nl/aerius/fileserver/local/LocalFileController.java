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
package nl.aerius.fileserver.local;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import nl.aerius.fileserver.storage.FileController;
import nl.aerius.fileserver.storage.StorageService;
import nl.aerius.fileserver.util.FilenameUtil;

/**
 * Controller to handle the HTTP requests to the file server.
 */
@Controller
@Profile("local")
class LocalFileController extends FileController {

  private static final Logger LOG = LoggerFactory.getLogger(LocalFileController.class);

  @Autowired
  public LocalFileController(final StorageService storageService) {
    super(storageService);
    LOG.info("Starting file controller with Local File Storage");
  }

  /**
   * Retrieve the file belonging to the given uuid and filename
   *
   * @param uuid uuid of file to get
   * @param filename filename of the file
   * @return returns the file or not found status if not present
   */
  @GetMapping(FILE_PATH)
  @ResponseBody
  public ResponseEntity<Resource> getFile(final @PathVariable String uuid, final @PathVariable String filename) {
    try {
      FilenameUtil.validateParameters(uuid, filename);
      LOG.debug("Get file {}/{}", uuid, filename);
      final String file = storageService.getFile(uuid, filename);

      LOG.debug("Returning file: {}", file);
      final FileUrlResource resource = new FileUrlResource(file);
      final HttpHeaders headers = new HttpHeaders();
      final ContentDisposition contentDisposition = ContentDisposition.attachment().filename(filename).build();

      headers.setContentDisposition(contentDisposition);
      return ResponseEntity.ok().headers(headers).body(resource);
    } catch (final IOException e) {
      LOG.trace("IOException when trying to get a file", e);
    } catch (final RuntimeException e) {
      LOG.warn("RuntimeException when trying to get a file", e);
    }
    return ResponseEntity.notFound().build();
  }

}
