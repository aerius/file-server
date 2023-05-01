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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Test class for {@link FilenameUtil}
 */
class FilenameUtilTest {

  @Test
  void testValidateUuid() {
    assertFalse(FilenameUtil.validateUuid("11233"), "Random number not matching uuid like pattern should not be valid");
    assertFalse(FilenameUtil.validateUuid("x".repeat(36)), "Random number with valid length not matching uuid like pattern should not be valid");
    assertFalse(FilenameUtil.validateUuid("x".repeat(33)),
        "Random number with valid length not matching job key uuid like pattern should not be valid");
    final String uuid = UUID.randomUUID().toString();

    assertTrue(FilenameUtil.validateUuid(uuid), "UUID should be valid");
    assertTrue(FilenameUtil.validateUuid('x' + uuid.replace("-", "")), "job key UUID should be valid");
  }
}
