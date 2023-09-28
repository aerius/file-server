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

import java.util.Locale;

/**
 * Tags to send to the file server to indicate how long files should be stored.
 */
public enum FileServerExpireTag {

  /**
   * Keep this file for a short period.
   */
  SHORT,
  /**
   * Keep this file for the amount of time data can be preserved according to the legal data retention period.
   */
  LEGAL,
  /**
   * These files should never be deleted.
   */
  NEVER;

  private static final String TAG_EXPIRES_KEY = "expires";

  /**
   * @return Name to the key to use this tag
   */
  public static String tagKey() {
    return TAG_EXPIRES_KEY;
  }

  /**
   * Returns a valid tag. If the tag is null it will return NEVER.
   *
   * @param tag tag to return as string
   * @return string value of the tag.
   */
  public static String safeTagValue(final FileServerExpireTag tag) {
    return (tag == null ? NEVER : tag).toTagName();
  }

  private String toTagName() {
    return name().toLowerCase(Locale.ROOT);
  }
}
