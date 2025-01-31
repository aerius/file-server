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
package nl.aerius.fileserver.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

/**
 * Test class for {@link FileServerClient}.
 */
@ExtendWith(MockitoExtension.class)
class FileServerClientTest {

  private static final String UUID_CODE = "123";
  private static final String FILE_CONTENTS = "test";
  private MockWebServer mockWebServer;

  @Mock private FileServerProperties properties;

  private FileServerClient fileServerClient;

  @BeforeEach
  void init() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    doReturn(String.format("http://localhost:%s", mockWebServer.getPort())).when(properties).getBaseUrl();
    fileServerClient = new FileServerClient(WebClient.builder(), properties);
  }

  @AfterEach
  void destroy() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void testRetrieveFile() throws InterruptedException {
    final String expectedFileName = "fileServiceResponse.json";

    mockFileServiceResponse(expectedFileName, FILE_CONTENTS.getBytes(StandardCharsets.UTF_8), HttpStatus.OK.value());

    final FilenameAwareByteArrayResource result = fileServerClient.retrieveFile(ExampleFileServerFile.VALIDATION,
        (fileName, inputStream) -> new FilenameAwareByteArrayResource(inputStream.readAllBytes(), fileName), UUID_CODE);

    assertRecordedRequest(HttpMethod.GET, UUID_CODE);

    assertEquals(expectedFileName, result.getFilename(), "Filename from fileservice should be available in the result stream handler.");
    assertArrayEquals(FILE_CONTENTS.getBytes(StandardCharsets.UTF_8), result.getByteArray(),
        "FileContents from fileService should be available in the result stream handler.");
  }

  @Test
  void testRetrieveFileServiceNotFoundError() throws InterruptedException {
    mockFileServiceResponse("", new byte[0], HttpStatus.NOT_FOUND.value());

    final ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> fileServerClient.retrieveFile(ExampleFileServerFile.VALIDATION, (name, is) -> null, UUID_CODE),
        "A ResponseStatusException should be thrown when the fileService returns a client error status.");

    assertRecordedRequest(HttpMethod.GET, UUID_CODE);
    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode(),
        "A 404 from fileService should be passed back to the end user as a 404.");
  }

  @Test
  void testRetrieveFileServiceClientError() throws InterruptedException {
    mockFileServiceResponse("", new byte[0], HttpStatus.FORBIDDEN.value());

    final ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> fileServerClient.retrieveFile(ExampleFileServerFile.VALIDATION, (name, is) -> null, UUID_CODE),
        "A ResponseStatusException should be thrown when the fileService returns a client error status other than 404.");

    assertRecordedRequest(HttpMethod.GET, UUID_CODE);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode(),
        "A client exception from fileService other than 404 should be passed back to the end user as an internal error because"
            + " that would mean connect doesn't call the fileService properly.");
  }

  @Test
  void testRetrieveFileServiceServerError() throws InterruptedException {
    mockFileServiceResponse("", new byte[0], HttpStatus.BAD_GATEWAY.value());

    final ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> fileServerClient.retrieveFile(ExampleFileServerFile.VALIDATION, (name, is) -> null, UUID_CODE),
        "A ResponseStatusException should be thrown when the fileService returns a server error status.");

    assertRecordedRequest(HttpMethod.GET, UUID_CODE);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode(),
        "A server exception from fileService should be passed back to the end user as an internal error.");
  }

  @Test
  void testWrite() throws InterruptedException {
    mockFileServiceResponse(HttpStatus.OK.value());

    fileServerClient.writeJson(UUID_CODE, ExampleFileServerFile.VALIDATION, FileServerExpireTag.NEVER, "test");
    assertRecordedRequest(HttpMethod.PUT, UUID_CODE);
  }

  @Test
  void testCopy() throws InterruptedException {
    mockFileServiceResponse(HttpStatus.OK.value());
    final String destinationCode = "456";
    final String filename = "SomeFile";

    fileServerClient.copy(UUID_CODE, destinationCode, filename, FileServerExpireTag.NEVER);
    assertRecordedRequest(HttpMethod.PUT, UUID_CODE, destinationCode, filename);
  }

  @Test
  void testDelete() throws InterruptedException {
    mockFileServiceResponse("", new byte[0], HttpStatus.OK.value());

    fileServerClient.deleteFilesForId(UUID_CODE);
    assertRecordedRequest(HttpMethod.DELETE, UUID_CODE);
  }

  @Test
  void testDeleteFileServerError() throws InterruptedException {
    mockFileServiceResponse("", new byte[0], HttpStatus.INTERNAL_SERVER_ERROR.value());

    assertDoesNotThrow(() -> fileServerClient.deleteFilesForId(UUID_CODE), "A file service error should not cause an exception.");
    assertRecordedRequest(HttpMethod.DELETE, UUID_CODE);
  }

  private void mockFileServiceResponse(final int status) {
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(status));
  }

  private void mockFileServiceResponse(final String fileName, final byte[] fileContents, final int status) {
    try (final Buffer buffer = new Buffer()) {
      mockWebServer.enqueue(new MockResponse()
          .setResponseCode(status)
          .addHeader("Content-Type:application/json")
          .addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
          .setBody(buffer.write(fileContents)));
    }
  }

  private void assertRecordedRequest(final HttpMethod httpMethod, final String... pathSegments) throws InterruptedException {
    final RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals(httpMethod.name(), recordedRequest.getMethod(), "File write should use a " + httpMethod.name() + " request.");
    assertNotNull(recordedRequest.getPath(), "A request path to the fileService should be build.");
    for (final String pathSegment : pathSegments) {
      assertTrue(recordedRequest.getPath().contains(pathSegment), "\"" + pathSegment + "\" should be passed to the fileService in the path.");
    }
  }

  static class FilenameAwareByteArrayResource extends ByteArrayResource {
    private final String filename;

    public FilenameAwareByteArrayResource(final byte[] bytes, final String filename) {
      super(bytes);
      this.filename = filename;
    }

    @Override
    public String getFilename() {
      return filename;
    }
  }
}
