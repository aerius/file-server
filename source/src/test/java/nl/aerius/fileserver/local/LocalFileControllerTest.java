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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import nl.aerius.fileserver.storage.StorageService;

/**
 * Test class for {@link LocalFileController}.
 */
@AutoConfigureMockMvc
@ActiveProfiles("local")
// Set s3 required property otherwise test will fail.
// Using only LocalFileController.class as parameter here somehow fails in getFile.
// Don't know why it fails. Therefore just setting required S3 property even if that property is not used in this test
@SpringBootTest(properties = "aerius.file.storage.s3.bucketName=dev")
class LocalFileControllerTest {

  private static final String HTTP_LOCALHOST = "http://localhost/";
  private static final String UUID_CODE = "00000000-0000-0000-0000-000000000001";
  private static final String FILENAME = "test.gml";
  private static final String URL_UUID = HTTP_LOCALHOST + UUID_CODE;
  private static final String URL = URL_UUID + "/" + FILENAME;
  private static final String URL_NOT_EXISTING = HTTP_LOCALHOST + UUID_CODE + "/2";
  private static final String URL_BAD_UUID = HTTP_LOCALHOST + "1".repeat(1000) + "/" + FILENAME;
  private static final String URL_BAD_FILENAME = HTTP_LOCALHOST + UUID_CODE + "/" + "1".repeat(1000);
  private static final String EXPIRE_TAG_VALUE = "never";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private StorageService storageService;

  @TempDir
  File tempDir;

  @Test
  void testPutFile() throws Exception {
    final MockMultipartFile multipartFile = new MockMultipartFile("file", FILENAME, "text/plain", "AERIUS".getBytes());
    final long size = multipartFile.getSize();

    doAnswer(a -> null).when(storageService).putFile(eq(UUID_CODE), eq(FILENAME), eq(size), isNotNull(), any());
    mvc.perform(put(URL + "?expires=never").content("AERIUS".getBytes())).andExpect(status().isOk());
    verify(storageService).putFile(eq(UUID_CODE), eq(FILENAME), eq(size), eq(EXPIRE_TAG_VALUE), any());
  }

  @Test
  void testPutFileInvalidParameter() throws Exception {
    mvc.perform(put(URL_BAD_FILENAME).content("AERIUS".getBytes())).andExpect(status().is4xxClientError());
    mvc.perform(put(URL_BAD_UUID).content("AERIUS".getBytes())).andExpect(status().is4xxClientError());
  }

  @Test
  void testPutFileOverwrite() throws Exception {
    final MockMultipartFile multipartFile = new MockMultipartFile("file", FILENAME, "text/plain", "AERIUS".getBytes());
    final long size = multipartFile.getSize();
    final MockMultipartFile multipartFile2 = new MockMultipartFile("file", FILENAME, "text/plain", "Tweede content".getBytes());
    final long size2 = multipartFile2.getSize();

    doAnswer(a -> null).when(storageService).putFile(eq(UUID_CODE), eq(FILENAME), eq(size), isNotNull(), any());
    mvc.perform(put(URL + "?expires=never").content("AERIUS".getBytes())).andExpect(status().isOk());
    mvc.perform(put(URL + "?expires=sometime").content("Tweede content".getBytes())).andExpect(status().isOk());
    verify(storageService).putFile(eq(UUID_CODE), eq(FILENAME), eq(size), eq(EXPIRE_TAG_VALUE), any());
    verify(storageService).putFile(eq(UUID_CODE), eq(FILENAME), eq(size2), eq("sometime"), any());
  }

  @Test
  void testGetFile() throws Exception {
    final String tempFilename = UUID.randomUUID().toString();
    final File tmpFile = new File(tempDir, tempFilename);
    final String content = "test";
    Files.writeString(tmpFile.toPath(), content);
    doReturn(tmpFile.getAbsolutePath()).when(storageService).getFile(eq(UUID_CODE), eq(tempFilename));
    final MockHttpServletResponse response = mvc.perform(get(HTTP_LOCALHOST + UUID_CODE + "/" + tempFilename)).andExpect(status().isOk()).andReturn()
        .getResponse();

    assertEquals("attachment; filename=\"" + tempFilename + "\"", response.getHeader(HttpHeaders.CONTENT_DISPOSITION),
        "Header should contain filename");
    assertEquals(content, response.getContentAsString(), "Expects file content to be in data");
  }

  @Test
  void testGetFile404Missing() throws Exception {
    doThrow(new FileNotFoundException()).when(storageService).getFile(any(), any());
    mvc.perform(get(URL_NOT_EXISTING)).andExpect(status().isNotFound());
  }

  @Test
  void testGetFileInvalidParameter() throws Exception {
    mvc.perform(get(URL_BAD_FILENAME)).andExpect(status().is4xxClientError());
    mvc.perform(get(URL_BAD_UUID)).andExpect(status().is4xxClientError());
  }

  @Test
  void testCopyFile() throws Exception {
    final String destinationUuid = UUID.randomUUID().toString();
    mvc.perform(put(HTTP_LOCALHOST + "copy/" + UUID_CODE + "/" + destinationUuid + "/" + FILENAME + "?expires=never"))
        .andExpect(status().isOk());
    verify(storageService).copyFile(UUID_CODE, destinationUuid, FILENAME, EXPIRE_TAG_VALUE);
  }

  @Test
  void testCopyFile404Missing() throws Exception {
    final String destinationUuid = UUID.randomUUID().toString();
    doThrow(new FileNotFoundException()).when(storageService).copyFile(any(), any(), any(), any());
    mvc.perform(put(HTTP_LOCALHOST + "copy/" + UUID_CODE + "/" + destinationUuid + "/" + FILENAME + "?expires=never"))
        .andExpect(status().isNotFound());
  }

  @Test
  void testDeleteFile() throws Exception {
    mvc.perform(delete(URL)).andExpect(status().isOk());
  }

  @Test
  void testDeleteFiles() throws Exception {
    mvc.perform(delete(URL_UUID)).andExpect(status().isOk());
  }

  @Test
  void testDeleteNoneExisitingFile() throws Exception {
    doThrow(new FileNotFoundException()).when(storageService).deleteFile(any(), any());
    mvc.perform(delete(URL_NOT_EXISTING)).andExpect(status().isOk());
  }

  @Test
  void testDeleteFileInvalidParameter() throws Exception {
    // Delete just always return ok.
    mvc.perform(delete(URL_BAD_FILENAME)).andExpect(status().isOk());
    mvc.perform(delete(URL_BAD_UUID)).andExpect(status().isOk());
  }
}
