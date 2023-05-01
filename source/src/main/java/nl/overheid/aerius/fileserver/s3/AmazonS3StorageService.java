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
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import nl.overheid.aerius.fileserver.storage.StorageService;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAttributesRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectAttributes;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest.Builder;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Service that wraps around the Amazon S3 client api.
 */
@Service
@EnableConfigurationProperties(AmazonS3StorageProperties.class)
@Profile("s3")
public class AmazonS3StorageService implements StorageService {

  private static final String TAG_EXPIRES_KEY = "expires";
  private static final String TAG_EXPIRES_NEVER = "never";
  private static final Duration SIGNATURE_DURATION = Duration.ofHours(1);
  private static final String OTHER_PREFIX = "z";

  private final S3Client s3Client;
  private final S3Presigner presigner;
  private final String bucketName;

  @Autowired
  public AmazonS3StorageService(final S3Client s3Client, final S3Presigner presigner, final AmazonS3StorageProperties properties) {
    bucketName = properties.getBucketName();
    this.s3Client = s3Client;
    this.presigner = presigner;
  }

  @Override
  public void putFile(final String uuid, final String filename, final long size, final String expires, final InputStream in) throws IOException {
    final String expiresValue = expires == null ? TAG_EXPIRES_NEVER : expires;
    final Builder builder = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(key(uuid, filename))
        .tagging(Tagging.builder().tagSet(Tag.builder().key(TAG_EXPIRES_KEY).value(expiresValue).build()).build());

    s3Client.putObject(builder.build(), RequestBody.fromBytes(in.readAllBytes()));
  }

  @Override
  public String getFile(final String uuid, final String filename) throws FileNotFoundException {
    try {
      final String key = key(uuid, filename);
      checkFileExists(key);

      final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucketName)
          .responseContentDisposition("attachment; filename=\"" + filename + "\"")
          .key(key)
          .build();
      final GetObjectPresignRequest objectPresignRequest = GetObjectPresignRequest.builder()
          .signatureDuration(SIGNATURE_DURATION)
          .getObjectRequest(getObjectRequest)
          .build();
      // Generate the presigned request
      return presigner.presignGetObject(objectPresignRequest).url().toExternalForm();
    } catch (final S3Exception e) {
      throw new FileNotFoundException(e.getMessage());
    }
  }

  /**
   * Check if file exists by querying for attribute checksum. If not exists. It will throw a NoSuchKeyException if the key doesn't exist.
   * @param key key to check
   */
  private void checkFileExists(final String key) {
    s3Client.getObjectAttributes(GetObjectAttributesRequest.builder().bucket(bucketName).key(key).objectAttributes(ObjectAttributes.CHECKSUM).build());
  }

  @Override
  public void deleteFile(final String uuid, final String filename) throws IOException {
    deleteObjects(List.of(toDeleteObject(key(uuid, filename))));
  }

  @Override
  public void deleteFiles(final String uuid) throws IOException {
    final ListObjectsRequest listObjects = ListObjectsRequest
        .builder()
        .bucket(bucketName)
        .prefix(uuidWithPrefix(uuid))
        .build();

    deleteObjects(s3Client.listObjects(listObjects).contents().stream().map(S3Object::key).map(AmazonS3StorageService::toDeleteObject)
        .collect(Collectors.toList()));
  }

  private void deleteObjects(final List<ObjectIdentifier> toDelete) throws IOException {
    try {
      final DeleteObjectsRequest dor = DeleteObjectsRequest.builder()
          .bucket(bucketName)
          .delete(Delete.builder()
              .objects(toDelete).build())
          .build();

      s3Client.deleteObjects(dor);
    } catch (final S3Exception e) {
      throw new IOException(e);
    }
  }

  private static String key(final String uuid, final String filename) {
    return uuidWithPrefix(uuid) + filename;
  }

  private static String uuidWithPrefix(final String uuid) {
    final String prefix = Character.digit(uuid.charAt(0), 16) == -1 ? String.valueOf(uuid.charAt(0)) : OTHER_PREFIX;

    return prefix + "/" + uuid + "/";
  }

  private static ObjectIdentifier toDeleteObject(final String objectName) {
    return ObjectIdentifier.builder().key(objectName).build();
  }

}
