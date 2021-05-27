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
import baselib.metrics.MetricRegisterable;
import baselib.metrics.MetricsExporter;
import static java.lang.String.valueOf;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public final class HttpClient {

  private static final int TIMEOUT = 1;

  private static final Map<String, RequestLifecycleMetrics> METRICS = new HashMap<>();

  private HttpClient() {
  }

  public static HttpResponse<String> get(final String uri) {
    return metricOf(uri, "GET").wrap(() -> ex(() -> {
      var client = java.net.http.HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(TIMEOUT))
          .build();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(uri))
          .build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    }));
  }

  public static HttpResponse<String> post(final String uri, final String body) {
    return metricOf(uri, "POST").wrap(() -> ex(() -> {
      var client = java.net.http.HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(TIMEOUT))
          .build();
      HttpRequest request = HttpRequest.newBuilder()
          .method("POST", BodyPublishers.ofString(body))
          .uri(URI.create(uri))
          .build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
    }));
  }

  private static RequestLifecycleMetrics metricOf(String uri, String method) {
    String host = ex(() -> new URI(uri).getHost());
    return METRICS.computeIfAbsent(method+host, k -> {
      var result = new RequestLifecycleMetrics(host, method);
      MetricsExporter.DEFAULT.register(result);
      return result;
    });
  }

  private static class RequestLifecycleMetrics implements MetricRegisterable {

    final String uri;
    final String method;

    private final AtomicLong counter = new AtomicLong();
    private final AtomicLong timeSum = new AtomicLong();
    private final AtomicLong timeMin = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong timeMax = new AtomicLong(Long.MIN_VALUE);

    public RequestLifecycleMetrics(String uri, String method) {
      this.uri = uri;
      this.method = method;
    }

    <T> T wrap(Supplier<T> action) {
      var nanos = System.nanoTime();
      try {
        return action.get();
      } finally {
        measure(System.nanoTime() - nanos);
      }
    }

    void measure(long nanos) {
      counter.incrementAndGet();
      timeSum.addAndGet(nanos);
      timeMin.getAndUpdate(x -> nanos < x ? nanos : x);
      timeMax.getAndUpdate(x -> nanos > x ? nanos : x);
    }

    @Override
    public void register(BiConsumer<String, Supplier<String>> registerFunction) {
      registerFunction.accept(makeMetricLabel("httpclient_request_count", uri, method), counter::toString);
      registerFunction.accept(makeMetricLabel("httpclient_request_nanos_sum", uri, method), timeSum::toString);
      registerFunction.accept(makeMetricLabel("httpclient_request_nanos_min", uri, method), () -> {
        var x = timeMin.get();
        return x == Long.MAX_VALUE ? "0" : valueOf(x);
      });
      registerFunction.accept(makeMetricLabel("httpclient_request_nanos_max", uri, method), () -> {
        var x = timeMax.get();
        return x == Long.MIN_VALUE ? "0" : valueOf(x);
      });
    }

    static String makeMetricLabel(String name, String uri, String method) {
      return name+"{uri=\""+uri+"\",method=\""+method+"\"}";
    }

  }
}
