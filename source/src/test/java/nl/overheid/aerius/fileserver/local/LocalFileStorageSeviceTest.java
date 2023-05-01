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
package nl.overheid.aerius.fileserver.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test class for {@link LocalFileStorageSevice}.
 */
class LocalFileStorageSeviceTest {

  private static final String CONTENT = "AERIUS";
  private static final String UUID_CODE = "123";
  private static final String FILENAME = "test.gml";
  @TempDir
  File tempDir;

  private LocalFileStorageSevice service;
  private File expectedFile;

  @BeforeEach
  void beforeEach() throws IOException {
    final LocalStorageProperties properties = new LocalStorageProperties();

    properties.setLocation(tempDir.getAbsolutePath());
    service = new LocalFileStorageSevice(properties);
    expectedFile = new File(new File(tempDir, UUID_CODE), FILENAME);
  }

  @Test
  void testLocalStorageDirectoryCreation() throws IOException {
    final LocalStorageProperties properties = new LocalStorageProperties();
    final File newStorageDirectory = new File(tempDir.getAbsolutePath(), UUID.randomUUID().toString());

    properties.setLocation(newStorageDirectory.getAbsolutePath());
    // override service with new properties to check if it creates the new directory.
    service = new LocalFileStorageSevice(properties);
    assertTrue(Files.exists(newStorageDirectory.toPath()), "Createing a new service should create the storage directory if it doesn't exists");
  }

  @Test
  void testPutFile() throws IOException {
    service.putFile(UUID_CODE, FILENAME, 10, null, new ByteArrayInputStream(CONTENT.getBytes()));
    assertTrue(expectedFile.exists(), "File should exist when stored");
    assertEquals(CONTENT, Files.readString(expectedFile.toPath()), "Content of file should be as expected.");
  }

  @Test
  void testGetFile() throws IOException {
    writeTempFile();
    assertEquals(expectedFile.getAbsolutePath(), service.getFile(UUID_CODE, FILENAME), "Expects the complete path to file.");
  }

  @Test
  void testGetFileNotFound() throws FileNotFoundException {
    assertThrows(FileNotFoundException.class, () -> service.getFile(UUID_CODE, FILENAME), "Expects the file to not be found.");
  }

  @Test
  void testDeleteFile() throws IOException {
    writeTempFile();
    service.deleteFile(UUID_CODE, FILENAME);
    assertFalse(expectedFile.exists(), "File should not exist after delete");
  }

  @Test
  void testDeleteNotFound() throws IOException {
    assertFalse(expectedFile.exists(), "Check if file does not exist before trying to delete non existing file.");
    assertThrows(NoSuchFileException.class, () -> service.deleteFile(UUID_CODE, FILENAME), "Should throw exception when file not found.");
  }

  @Test
  void testDeleteFiles() throws IOException {
    writeTempFile();
    service.deleteFile(UUID_CODE, FILENAME);
    assertFalse(expectedFile.exists(), "File should not exist after delete");
  }

  private void writeTempFile() throws IOException {
    Files.createDirectory(expectedFile.getParentFile().toPath());
    Files.writeString(expectedFile.toPath(), CONTENT);
  }
}
