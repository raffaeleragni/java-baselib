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
package baselib;

import com.sun.net.httpserver.HttpExchange;//NOSONAR
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import static baselib.ExceptionWrapper.ex;

/**
 * Creates a simple http server with basic url mappings.
 * Supported by the standard jvm http server.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class HttpServer {
  private final int port;
  private final com.sun.net.httpserver.HttpServer server;//NOSONAR

  /**
   * Create a new server.
   * The server is not started yet, for that call the start().
   * The server will bind to the specified port, but without binding to any specific interface.
   * This means that it will listen to 0.0.0.0 for this current implementation.
   * Future implementations may extend that feature.
   *
   * @param port the port that the server will bind to.
   * @param handlers the http handlers to be mapped: key is the uri and value is the lambda that is executed on that uri
   */
  public HttpServer(int port, Map<String, Consumer<Context>> handlers) {
    this.port = port;
    this.server = ex(() -> com.sun.net.httpserver.HttpServer.create());//NOSONAR
    Objects.requireNonNull(handlers.entrySet())
      .stream()
      .filter(e -> Objects.nonNull(e.getValue()))
      .forEach(e ->
        server.createContext(e.getKey(), exchange -> wrapExchange(exchange, e.getValue()))
      );
  }

  /**
   * The server will be started and the port occupied.
   * After this moment the server will start accepting requests.
   */
  public void start() {
    ex(() -> {
      server.bind(new InetSocketAddress(port), 0);
      server.start();
    });
  }

  /**
   * Stops the server.
   * The server will be stopped abruptly and without waiting for requests to be finished.
   * Future implementation may add an option for a wait time.
   *
   * Once stopped, this instance of server cannot be reused.
   */
  public void stop() {
    server.stop(0);
  }

  /**
   * Request context.
   * with this context instance you can access request and response.
   */
  public interface Context {

    /**
     * Write a string as a response.
     * After this function the response will be in status 200 and the output will be closed, ending the response.
     * @param body A full string to be written into the response.
     */
    void response(String body);
  }

  public static class HttpStatus extends RuntimeException {
    private final int status;

    public HttpStatus(int status) {
      super();
      this.status = status;
    }

    public int status() {
      return status;
    }
  }

  private static void wrapExchange(HttpExchange exchange, Consumer<Context> function) {
    var ctx = new ContextImpl(exchange);
    try {
      function.accept(ctx);
    } catch (HttpStatus e) {
      ex(() -> {
        exchange.sendResponseHeaders(e.status(), 0);
        exchange.getResponseBody().close();
      });
    }
  }

  private static class ContextImpl implements Context {
    final HttpExchange exchange;
    ContextImpl(HttpExchange exchange) {
      this.exchange = exchange;
    }

    @Override
    public void response(String body) {
      writer(out ->
        ex(() -> {
          exchange.sendResponseHeaders(200, body.length());
          out.write(body);
        })
      );
    }

    private void writer(Consumer<BufferedWriter> writer) {
      ex(() -> {
        try (var out = new BufferedWriter(new OutputStreamWriter(exchange.getResponseBody(), UTF_8))) {
          writer.accept(out);
        }
      });
    }
  }
}
