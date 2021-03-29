/*
 * Copyright 2020 Raffaele Ragni <raffaele.ragni@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package baselib.http;

import static baselib.ExceptionWrapper.ex;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public final class HttpClient {

  private static final int TIMEOUT = 1;

  private HttpClient() {
  }

  public static HttpResponse<String> get(final String url) {
    return ex(() -> {
      var client = java.net.http.HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(TIMEOUT))
          .build();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    });
  }

  public static HttpResponse<String> post(final String url, final String body) {
    return ex(() -> {
      var client = java.net.http.HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(TIMEOUT))
          .build();
      HttpRequest request = HttpRequest.newBuilder()
          .method("POST", BodyPublishers.ofString(body))
          .uri(URI.create(url))
          .build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    });
  }
}
