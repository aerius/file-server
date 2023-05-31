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

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Local storage configuration.
 */
@ConfigurationProperties("aerius.file.storage")
@Validated
class LocalStorageProperties {
  /**
   * Folder location for storing files
   */
  private @NotNull String location = "aeriusupload";

  private boolean preventCleanup;

  public String getLocation() {
    return location;
  }

  public void setLocation(final String location) {
    this.location = location;
  }

  public boolean isPreventCleanup() {
    return preventCleanup;
  }

  public void setPreventCleanup(final boolean preventCleanup) {
    this.preventCleanup = preventCleanup;
  }
}
