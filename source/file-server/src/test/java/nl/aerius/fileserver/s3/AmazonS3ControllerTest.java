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
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nl.aerius.fileserver.storage.StorageService;

/**
 * Test class for {@link AmazonS3Controller}.
 */
@AutoConfigureMockMvc
@ActiveProfiles("s3")
@SpringBootTest(properties = "aerius.file.storage.s3.bucketName=dev")
class AmazonS3ControllerTest {

  private static final String HTTP_LOCALHOST = "http://localhost/";
  private static final String UUID_CODE = "00000000-0000-0000-0000-000000000001";
  private static final Object AMAZON_URL = "https://s3/uuid/filename";

  @Autowired private MockMvc mvc;

  @MockitoBean private StorageService storageService;

  @Test
  void testGetFile() throws Exception {
    final String tempFilename = "filename";
    doReturn(AMAZON_URL).when(storageService).getFile(UUID_CODE, tempFilename);
    final MockHttpServletResponse response = mvc.perform(get(HTTP_LOCALHOST + UUID_CODE + "/" + tempFilename)).andExpect(status().is3xxRedirection())
        .andReturn().getResponse();

    assertEquals(AMAZON_URL, response.getRedirectedUrl(), "Expects url as redirect url");
  }
}
