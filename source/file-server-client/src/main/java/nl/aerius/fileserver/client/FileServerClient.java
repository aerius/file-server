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

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

public class FileServerClient {

  private static final Logger LOG = LoggerFactory.getLogger(FileServerClient.class);
  private static final String COPY_URI_TEMPLATE = "copy/{sourceId}/{destinationId}/{filename}";
  private static final Duration WEBCLIENT_TIMEOUT = Duration.ofMinutes(1);

  private final WebClient fileServerWebClient;
  private final WebClient fileServerWebClientWithoutRedirect;

  private static final String ALL_FILES = "{uuid}";

  public FileServerClient(final WebClient.Builder webClientBuilder, final FileServerProperties properties) {
    this.fileServerWebClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
    this.fileServerWebClientWithoutRedirect = webClientBuilder.baseUrl(properties.getBaseUrl())
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(false))).build();
  }

  @PostConstruct
  void fakeCall() {
    // Webflux/netty seems to have a bug where the body of the first object is retained in memory.
    // To reduce this memory use, use a normal get method, even though it'll result in nonsense.
    try {
      this.fileServerWebClient.get().retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(1));
    } catch (final WebClientException e) {
      LOG.debug("Ignoring exception, just avoiding unnecessary memory use", e);
    }
  }

  /**
   * Retrieve a file from the file server. It uses a response handler to implement different ways to handle the response.
   * It uses a file server that doesn't follow redirect. This allows for an implementation to redirect the file instead of downloading it.
   *
   * @param fileServerFile type of file
   * @param responseHandler handler to implement how to handle the response
   * @param pathValues path variables to replace in the url.
   * @return Response entity
   */
  public ResponseEntity<Resource> retrieveFile(final FileServerFile fileServerFile,
      final Function<ClientResponse, Mono<ResponseEntity<Resource>>> responseHandler, final String... pathValues) {
    return fileServerWebClientWithoutRedirect.get()
        .uri(fileServerFile.uriTemplate(), uriBuilder -> uriBuilder.build((Object[]) pathValues))
        .exchangeToMono(responseHandler)
        .block(WEBCLIENT_TIMEOUT);
  }

  /**
   * Take care when implementing MapInputStreamFunction:
   * The inputstream is not automatically closed, so depending on the use case the stream has to be captured in a try-with-resources.
   * If the inputstream is used for a InputStreamResource (to send the result directly to the caller), don't bother closing it.
   */
  public <T> T retrieveFile(final FileServerFile fileServerFile, final MapInputStreamFunction<T> function, final String... pathValues) {
    return fileServerWebClient.get()
        .uri(fileServerFile.uriTemplate(), uriBuilder -> uriBuilder.build((Object[]) pathValues))
        .retrieve()
        .toEntity(DataBuffer.class)
        .map(responseEntity -> handleResponse(responseEntity, function))
        .onErrorMap(WebClientResponseException.class, FileServerClient::handleError)
        .block(WEBCLIENT_TIMEOUT);
  }

  private static <T> T handleResponse(final ResponseEntity<DataBuffer> responseEntity, final MapInputStreamFunction<T> function) {
    try {
      final DataBuffer body = responseEntity.getBody();
      if (body == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
      return function.apply(responseEntity.getHeaders().getContentDisposition().getFilename(), body.asInputStream(true));
    } catch (final IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, null, e);
    }
  }

  private static WebClientResponseException handleError(final WebClientResponseException e) {
    if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public void write(final FileServerFile fileServerFile, final InputStream inputStream, final FileServerExpireTag expire,
      final String... fileParts) {
    putResource(fileServerFile, new InputStreamResource(inputStream), expire, fileParts)
        .block(WEBCLIENT_TIMEOUT);
  }

  public void writeJson(final String id, final FileServerFile fileServerFile, final FileServerExpireTag expire, final Object object) {
    putJson(fileServerFile, object, expire, id)
        .block(WEBCLIENT_TIMEOUT);
  }

  private Mono<Void> putJson(final FileServerFile fileServerFile, final Object object, final FileServerExpireTag expire, final String... fileParts) {
    final String uriTemplate = fileServerFile.uriTemplate();
    final RequestBodySpec requestWithUri = putWithUri(uriTemplate, expire, fileParts)
        .contentType(MediaType.APPLICATION_JSON);

    final RequestHeadersSpec<?> requestWithUriAndBody = requestWithUri.bodyValue(object);

    return retrieve(requestWithUriAndBody, uriTemplate);
  }

  private Mono<Void> putResource(final FileServerFile fileServerFile, final Resource resource, final FileServerExpireTag expire,
      final String... fileParts) {
    final String uriTemplate = fileServerFile.uriTemplate();
    final RequestBodySpec requestWithUri = putWithUri(uriTemplate, expire, fileParts)
        .contentType(MediaType.APPLICATION_OCTET_STREAM);

    final RequestHeadersSpec<?> requestWithUriAndBody = requestWithUri.body(BodyInserters.fromResource(resource));

    return retrieve(requestWithUriAndBody, uriTemplate);
  }

  private RequestBodySpec putWithUri(final String uriTemplate, final FileServerExpireTag expire, final String... fileParts) {
    return fileServerWebClient.put()
        .uri(uriTemplate,
            uriBuilder -> uriBuilder.queryParam(FileServerExpireTag.tagKey(), FileServerExpireTag.safeTagValue(expire)).build((Object[]) fileParts));
  }

  private static Mono<Void> retrieve(final RequestHeadersSpec<?> requestSpec, final String uriTemplate) {
    return requestSpec
        .retrieve()
        .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> clientResponse.bodyToMono(String.class).map(message -> {
          LOG.error("Fileserver error while posting to '{}' with message: {}", uriTemplate, message);
          return new IllegalStateException("Error while posting to fileserver.");
        }))
        .onStatus(HttpStatusCode::is4xxClientError, ClientResponse::createError)
        .bodyToMono(Void.class);
  }

  public void copy(final String sourceId, final String destinationId, final String filename, final FileServerExpireTag expire) {
    final RequestBodySpec copyRequest = putWithUri(COPY_URI_TEMPLATE, expire, sourceId, destinationId, filename);

    retrieve(copyRequest, COPY_URI_TEMPLATE)
        .block(WEBCLIENT_TIMEOUT);
  }

  /**
   * Delete all files on the file server registered under the given id.
   *
   * @param id id to delete the files for.
   */
  public void deleteFilesForId(final String id) {
    fileServerWebClient.delete()
        .uri(ALL_FILES, uriBuilder -> uriBuilder.build(id))
        .retrieve()
        .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> clientResponse.bodyToMono(String.class).map(message -> {
          LOG.error("Fileserver server error with message: {}", message);
          return new IllegalStateException("Error while posting to fileserver.");
        }))
        // Ignore not found errors on delete requests
        .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), clientResponse -> Mono.empty())
        .bodyToMono(Void.class)
        .block(WEBCLIENT_TIMEOUT);
  }

  public interface MapInputStreamFunction<T> {
    T apply(String filename, InputStream inputStream) throws IOException;
  }

  /**
   * Creates a unique id with the given prefix prepended to the id.
   *
   * @param prefix string to prefix
   * @return unique id
   */
  public static String createId(final String prefix) {
    return prefix + UUID.randomUUID().toString().replace("-", "");
  }
}
