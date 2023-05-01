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
package nl.overheid.aerius.fileserver.s3;

import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import nl.overheid.aerius.fileserver.storage.FileController;
import nl.overheid.aerius.fileserver.storage.StorageService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Implements the controller for using Amazon S3.
 */
@Controller
@Profile("s3")
public class AmazonS3Controller extends FileController {

  private static final Logger LOG = LoggerFactory.getLogger(AmazonS3Controller.class);

  public AmazonS3Controller(final StorageService storageService) {
    super(storageService);
    LOG.info("Starting file controller with Amazon S3 File Storage");
  }

  /**
   * Instead of returning the content of the file this redirects to a Amazon S3 url.
   *
   * @param uuid uuid of the file
   * @param filename file name
   * @return temporal URL to the file on Amazon S3
   */
  @GetMapping(FILE_PATH)
  public String getFile(final @PathVariable String uuid, final @PathVariable String filename, final HttpServletRequest request,
      final HttpServletResponse response) {
    try {
      LOG.debug("Get file {}/{}", uuid, filename);
      return "redirect:" + storageService.getFile(uuid, filename);
    } catch (final FileNotFoundException | RuntimeException e) {
      response.setStatus(HttpStatus.NOT_FOUND.value());
      return null;
    }
  }
}
