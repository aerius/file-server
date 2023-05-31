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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import nl.aerius.fileserver.storage.StorageService;

/**
 * Class to manage files to be stored on a file system.
 */
@Service
@EnableConfigurationProperties(LocalStorageProperties.class)
@Profile("local")
class LocalFileStorageSevice implements StorageService {

  private final File localStorageDirectory;
  private final boolean preventCleanup;

  @Autowired
  public LocalFileStorageSevice(final LocalStorageProperties properties) throws IOException {
    preventCleanup = properties.isPreventCleanup();
    localStorageDirectory = properties.getLocation() == null ? null : new File(properties.getLocation());
    if (localStorageDirectory != null && !localStorageDirectory.exists()) {
      Files.createDirectory(localStorageDirectory.toPath());
    }
  }

  @Override
  public void putFile(final String uuid, final String filename, final long size, final String expires, final InputStream in) throws IOException {
    final Path uuidPath = uuidDirectory(uuid);

    if (!Files.exists(uuidPath)) {
      Files.createDirectory(uuidPath);
    }
    final Path file = filePath(uuidPath, filename);
    Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public String getFile(final String uuid, final String filename) throws FileNotFoundException {
    return existingFilePath(uuid, filename).toAbsolutePath().toString();
  }

  @Override
  public void copyFile(final String sourceUuid, final String destinationUuid, final String filename, final String expires) throws IOException {
    final Path sourceFilePath = existingFilePath(sourceUuid, filename);
    final Path uuidPath = uuidDirectory(destinationUuid);

    if (!Files.exists(uuidPath)) {
      Files.createDirectory(uuidPath);
    }
    final Path destinationFilePath = filePath(uuidPath, filename);
    Files.copy(sourceFilePath, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public void deleteFile(final String uuid, final String filename) throws IOException {
    if (preventCleanup) {
      return;
    }
    final Path uuidPath = uuidDirectory(uuid);

    Files.delete(filePath(uuidPath, filename));
    final String[] files = uuidPath.toFile().list();

    if (files == null || files.length == 0) {
      Files.delete(uuidPath);
    }
  }

  @Override
  public void deleteFiles(final String uuid) throws IOException {
    if (preventCleanup) {
      return;
    }
    final Path uuidPath = uuidDirectory(uuid);

    if (Files.exists(uuidPath)) {
      final File uuidFile = uuidPath.toFile();

      for (final String file : uuidFile.list()) {
        Files.delete(new File(uuidFile, file).toPath());
      }
      Files.delete(uuidPath);
    } else {
      throw new NoSuchFileException("Key not found");
    }
  }

  private Path existingFilePath(final String uuid, final String filename) throws FileNotFoundException {
    final Path uuidPath = uuidDirectory(uuid);

    if (Files.exists(uuidPath)) {
      final Path file = filePath(uuidPath, filename);

      if (Files.exists(file)) {
        return file;
      }
    }
    throw new FileNotFoundException("file '" + uuid + "/" + filename + "' not found");
  }

  private Path uuidDirectory(final String uuid) {
    return Paths.get(localStorageDirectory.getAbsolutePath(), uuid);
  }

  private static Path filePath(final Path uuidPath, final String filename) {
    return uuidPath.resolve(filename);
  }
}
