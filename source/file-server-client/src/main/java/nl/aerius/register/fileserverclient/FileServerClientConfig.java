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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Fileserver client configuration, to be used via {@link @import} annotation.
 */
@Configuration
public class FileServerClientConfig {

  @Bean
  public FileServerProperties fileServerProperties() {
    return new FileServerProperties();
  }

  @Bean
  public FileServerClient fileServerClient(final WebClient.Builder webClientBuilder, final FileServerProperties fileServerProperties) {
    return new FileServerClient(webClientBuilder, fileServerProperties);
  }
}
