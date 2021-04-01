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
import static baselib.TestHelper.portOccupied;
import static baselib.http.HttpClient.get;
import static baselib.http.HttpClient.post;
import baselib.http.HttpServer.HttpStatus;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
class HttpServerTest {
  private static final int PORT = new Random(System.currentTimeMillis()).nextInt(50_000)+1024;
  private static final int THREADS = 10;

  @Test
  void testServer() {
    var server = HttpServer.create(PORT, THREADS, Map.of());

    assertThat(portOccupied(PORT), is(false));
    withServer(server, () -> {
      assertThat(portOccupied(PORT), is(true));
    });
    assertThat(portOccupied(PORT), is(false));
  }

  @Test
  void testSimpleBodyOutput() {
    var url = "http://localhost:"+PORT;
    var server = HttpServer.create(PORT, THREADS, Map.of(
      "/", ctx -> "hello world",
      "/exception", ctx -> {throw new RuntimeException();},
      "/500", ctx -> {throw new HttpStatus(500);},
      "/nooutput", ctx -> ""
    ));

    withServer(server, () -> {
      assertThat(get(url+"/").body(), is("hello world"));

      assertThat(get(url+"/nooutput").body(), is(""));

      assertThat(get(url+"/exception").statusCode(), is(500));
      assertThat(get(url+"/500").statusCode(), is(500));

      assertThat(get(url+"/metrics").statusCode(), is(200));
    });
  }

  @Test
  void testMetricsOverride() {
    var url = "http://localhost:"+PORT;
    var server = HttpServer.create(PORT, THREADS, Map.of(
      "/metrics", ctx -> "mymetrics"
    ));

    withServer(server, () -> {
      assertThat(get(url+"/metrics").statusCode(), is(200));
      assertThat(get(url+"/metrics").body(), is("mymetrics"));
    });
  }

  @Test
  void testPaths() {
    var paths = new HashSet<String>();
    var vars = new HashSet<String>();
    var url = "http://localhost:"+PORT;
    var server = HttpServer.create(PORT, THREADS, Map.of(
      "/test", ctx -> {
        paths.add(ctx.mappedPath());
        vars.add(ctx.variablePath());
        return "";
      }
    ));

    withServer(server, () -> {
      get(url+"/test");
      get(url+"/test/v1");
      get(url+"/test/");
      get(url+"/test/v2/");
      get(url+"/test/v3/v2/v4/");

      assertThat(paths, is(Set.of("/test")));
      assertThat(vars, is(Set.of("", "v1", "v2", "v3/v2/v4")));
    });
  }

  @Test
  void testStringProducer() {
    var url = "http://localhost:"+PORT;
    var server = HttpServer.create(PORT, THREADS, Map.of(
      "/test", ctx -> "test"
    ));

    withServer(server, () -> {
      assertThat(get(url+"/test").body(), is("test"));
    });
  }

  @Test
  void testWriter() {
    var url = "http://localhost:"+PORT;
    var server = HttpServer.create(PORT, THREADS, Map.of(
      "/test", ctx -> {
        ctx.writer(out -> ex(() -> out.write("test")));
        return "";
      }
    ));

    withServer(server, () -> {
      assertThat(get(url+"/test").body(), is("test"));
    });
  }

  @Test
  void testWriterRemapped() {
    var url = "http://localhost:"+PORT;
    var server = HttpServer.create(PORT, THREADS, Map.of(
      "/test", ctx -> {
        ctx.writer(out -> ex(() -> out.write("test")));
        return "";
      }
    ));

    withServer(server, () -> {
      assertThat(get(url+"/test").body(), is("test"));
    });
  }

  @Test
  void testBodyReader() {
    var body = new String[1];
    var url = "http://localhost:"+PORT;
    var server = HttpServer.create(PORT, THREADS, Map.of(
      "/read", ctx -> {
        body[0] = ctx.body();
        return "";
      }
    ));
    var requestBody = """
                      {"id":1, "test":"test2"}
                      """;

    withServer(server, () -> {
      post(url+"/read", requestBody);

      assertThat(body[0], is(requestBody));
    });
  }

  void withServer(HttpServer server, Runnable runnable) {
    server.start();
    try {
      runnable.run();
    } finally {
      server.stop();
    }
  }
}
