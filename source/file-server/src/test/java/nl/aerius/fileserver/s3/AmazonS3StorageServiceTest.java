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
package nl.aerius.fileserver.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.S3Utilities.Builder;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAttributesRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * Test class for {@link AmazonS3StorageService}.
 */
@ExtendWith(MockitoExtension.class)
class AmazonS3StorageServiceTest {

  private static final Region REGION = Region.EU_WEST_1;
  private static final String BUCKET_NAME = "AERIUS_UNIT_TEST";
  private static final String AMAZON_URL = "https://s3.eu-west-1.amazonaws.com/" + BUCKET_NAME;

  private static final String CONTENT = "AERIUS";
  private static final String UUID_CODE = "123";
  private static final String FILENAME = "test.gml";
  private static final String EXPECTED_KEY_PREFIX = "z/" + UUID_CODE + "/";
  private static final String EXPECTED_KEY = EXPECTED_KEY_PREFIX + FILENAME;
  private static final String EXPECTED_FULLPATH = AMAZON_URL + "/" + EXPECTED_KEY;
  private static final String UUID_CODE_PREFIXED = "t123";
  private static final String EXPECTED_KEY_PEFIXED = "t/" + UUID_CODE_PREFIXED + "/" + FILENAME;
  private static final String EXPECTED_FULLPATH_PREFIXED = AMAZON_URL + "/" + EXPECTED_KEY_PEFIXED;

  private @Mock S3Client s3Client;
  private @Mock S3Presigner presigner;
  private @Mock PresignedGetObjectRequest presignedGetObjectRequest;

  private @Captor ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor;
  private @Captor ArgumentCaptor<RequestBody> requestBodyCaptor;
  private @Captor ArgumentCaptor<CopyObjectRequest> copyObjectRequestCaptor;
  private @Captor ArgumentCaptor<DeleteObjectsRequest> deleteObjectsRequestCaptor;
  private @Captor ArgumentCaptor<ListObjectsRequest> listObjectsRequestCaptor;

  private AmazonS3StorageService service;

  @BeforeEach
  void beforeEach() {
    final AmazonS3StorageProperties properties = new AmazonS3StorageProperties();

    properties.setBucketName(BUCKET_NAME);
    service = new AmazonS3StorageService(s3Client, presigner, properties);
  }

  @Test
  void testPutFile() throws IOException {
    service.putFile(UUID_CODE, FILENAME, 10, null, new ByteArrayInputStream(CONTENT.getBytes()));
    verify(s3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());

    assertEquals(EXPECTED_KEY, putObjectRequestCaptor.getValue().key(), "File should exist when stored");
    assertEquals(CONTENT.length(), requestBodyCaptor.getValue().optionalContentLength().get(), "Content of file should be as expected.");
  }

  @Test
  void testPutFileOverwrite() throws IOException {
    final String overwriteContent = "Overwritten content";
    service.putFile(UUID_CODE, FILENAME, 10, null, new ByteArrayInputStream(CONTENT.getBytes()));
    service.putFile(UUID_CODE, FILENAME, 0, null, new ByteArrayInputStream(overwriteContent.getBytes()));
    verify(s3Client, times(2)).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());

    assertEquals(2, putObjectRequestCaptor.getAllValues().size());
    assertEquals(2, requestBodyCaptor.getAllValues().size());
    for (final PutObjectRequest putObjectRequest : putObjectRequestCaptor.getAllValues()) {
      assertEquals(EXPECTED_KEY, putObjectRequest.key(), "File should be stored with same key when overwritten");
    }
    assertEquals(CONTENT.length(), requestBodyCaptor.getAllValues().get(0).optionalContentLength().get(),
        "Content of first file upload should be as expected.");
    assertEquals(overwriteContent.length(), requestBodyCaptor.getAllValues().get(1).optionalContentLength().get(),
        "Content of second file upload should be as expected.");
  }

  @ParameterizedTest
  @CsvSource({UUID_CODE + "," + EXPECTED_FULLPATH, UUID_CODE_PREFIXED + "," + EXPECTED_FULLPATH_PREFIXED})
  void testGetFile(final String uuid, final String expectedFullPath) throws IOException {
    final Builder builder = S3Utilities.builder();
    builder.region(REGION);
    doReturn(presignedGetObjectRequest).when(presigner).presignGetObject(any(GetObjectPresignRequest.class));
    doAnswer(a -> new URI(expectedFullPath).toURL()).when(presignedGetObjectRequest).url();
    assertEquals(expectedFullPath, service.getFile(uuid, FILENAME), "Expects the complete path to file.");
  }

  @Test
  void testGetFileNotFound() {
    doThrow(S3Exception.builder().build()).when(s3Client).getObjectAttributes(any(GetObjectAttributesRequest.class));
    assertThrows(FileNotFoundException.class, () -> service.getFile(UUID_CODE, FILENAME), "Expects the file to not be found.");
  }

  @Test
  void testCopyFile() throws IOException {
    final String destinationUuid = UUID.randomUUID().toString();
    service.copyFile(UUID_CODE, destinationUuid, FILENAME, "copiedTag");
    verify(s3Client).copyObject(copyObjectRequestCaptor.capture());

    final CopyObjectRequest copyObjectRequest = copyObjectRequestCaptor.getValue();
    assertEquals(BUCKET_NAME, copyObjectRequest.sourceBucket(), "Bucket should be the same");
    assertEquals(BUCKET_NAME, copyObjectRequest.destinationBucket(), "Bucket should be the same");
    assertEquals(EXPECTED_KEY, copyObjectRequest.sourceKey(), "Source key should be correct");
    assertEquals("z/" + destinationUuid + "/" + FILENAME, copyObjectRequest.destinationKey(), "Destination key should be correct");
    assertEquals("expires=copiedTag", copyObjectRequest.tagging(), "Tag used");
  }

  @Test
  void testCopyFileNotFound() {
    final String destinationUuid = UUID.randomUUID().toString();
    doThrow(S3Exception.builder().build()).when(s3Client).copyObject(any(CopyObjectRequest.class));
    assertThrows(IOException.class, () -> service.copyFile(UUID_CODE, destinationUuid, FILENAME, null),
        "Expect any exception to be mapped as IOException.");
  }

  @Test
  void testDeleteFile() throws IOException {
    service.deleteFile(UUID_CODE, FILENAME);
    verify(s3Client).deleteObjects(deleteObjectsRequestCaptor.capture());
    final Object[] deletedValues = deleteObjectsRequestCaptor.getValue().delete().getValueForField("Objects", Collection.class).get().toArray();

    assertEquals(EXPECTED_KEY, ((ObjectIdentifier) deletedValues[0]).key(), "Should have got the expected key to delete.");
  }

  @Test
  void testDeleteNotFound() {
    doThrow(S3Exception.builder().build()).when(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
    assertThrows(IOException.class, () -> service.deleteFile(UUID_CODE, FILENAME), "Should throw exception when file not found.");
  }

  @Test
  void testDeleteFiles() throws IOException {
    final ListObjectsResponse.Builder builder = ListObjectsResponse.builder();
    final S3Object.Builder objectBuilder = S3Object.builder();
    objectBuilder.key(EXPECTED_KEY);
    builder.contents(List.of(objectBuilder.build()));
    doReturn(builder.build()).when(s3Client).listObjects(any(ListObjectsRequest.class));

    service.deleteFiles(UUID_CODE);

    verify(s3Client).listObjects(listObjectsRequestCaptor.capture());
    assertEquals(EXPECTED_KEY_PREFIX, listObjectsRequestCaptor.getValue().prefix(), "When listing objects it should contain the uuid as prefix");

    verify(s3Client).deleteObjects(deleteObjectsRequestCaptor.capture());
    final Object[] deletedValues = deleteObjectsRequestCaptor.getValue().delete().getValueForField("Objects", Collection.class).get().toArray();
    assertEquals(EXPECTED_KEY, ((ObjectIdentifier) deletedValues[0]).key(), "Should have got the expected key to delete.");
  }
}
