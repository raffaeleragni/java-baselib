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

import com.sun.net.httpserver.HttpExchange; //NOSONAR
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
public final class HttpServer {
  private final int port;
  private final com.sun.net.httpserver.HttpServer server; //NOSONAR

  /**
   * Create a new server.
   * The server is not started yet, for that call the start().
   * The server will bind to the specified port, but without binding to any
   * specific interface.
   * This means that it will listen to 0.0.0.0 for this current implementation.
   * Future implementations may extend that feature.
   *
   * @param serverPort the port that the server will bind to.
   * @param handlers the http handlers to be mapped: key is the uri and value is
   *                 the lambda that is executed on that uri
   */
  public HttpServer(
      final int serverPort,
      final Map<String, Consumer<Context>> handlers) {

    this.port = serverPort;
    this.server = ex(() -> //NOSONAR
        com.sun.net.httpserver.HttpServer.create()); //NOSONAR
    Objects.requireNonNull(handlers.entrySet())
      .stream()
      .filter(e -> Objects.nonNull(e.getValue()))
      .forEach(e ->
        server.createContext(e.getKey(), exchange ->
            wrapExchange(e.getKey(), exchange, e.getValue()))
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
   * The server will be stopped abruptly and without waiting for requests
   * to be finished.
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
     * After this function the response will be in status 200 and the output
     * will be closed, ending the response.
     * @param body A full string to be written into the response.
     */
    void response(String body);

    /**
     *
     * @return the part of the path that was statically mapped to the handler.
     *         differently than variablePath(), it returns only the prefix.
     *         Example: if you map '/path' to a handler, but receive a request
     *         with '/path/var1/var2' then the original mapped string only will
     *         be returned, in this case '/path', regardless of the full request
     *         url.
     */
    String mappedPath();

    /**
     *
     * @return the variable part of the path.
     *         Example: if you map '/path' to a handler, but receive a request
     *         with '/path/var2/var3/' then the variable path is the part that
     *         is extra in the request url from the original path. In this case
     *         it will return with stripped leading/traling shash 'var2/var3'
     */
    String variablePath();
  }

  /**
   * Exception used to map http return values for status codes.
   */
  public static final class HttpStatus extends RuntimeException {
    private final int status;

    public HttpStatus(final int httpStatus) {
      super();
      this.status = httpStatus;
    }

    public int status() {
      return status;
    }
  }

  private static void wrapExchange(
      final String originalMappedPath,
      final HttpExchange exchange,
      final Consumer<Context> function) {

    var ctx = new ContextImpl(originalMappedPath, exchange);
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

    private static final int HTTP_OK = 200;
    private final String path;
    private final HttpExchange exchange;

    ContextImpl(
        final String originalMappedPath,
        final HttpExchange httpExchange) {

      this.path = originalMappedPath;
      this.exchange = httpExchange;
    }

    @Override
    public void response(final String body) {
      writer(out ->
        ex(() -> {
          exchange.sendResponseHeaders(HTTP_OK, body.length());
          out.write(body);
        })
      );
    }

    @Override
    public String mappedPath() {
      return path;
    }

    @Override
    public String variablePath() {
      var variable = exchange
          .getRequestURI()
          .getPath()
          .substring(path.length());
      if (variable.startsWith("/"))
        variable = variable.substring(1);
      if (variable.endsWith("/"))
        variable = variable.substring(0, variable.length() - 1);
      return variable;
    }

    private void writer(final Consumer<BufferedWriter> writer) {
      ex(() -> {
        try (var out = new BufferedWriter(
            new OutputStreamWriter(exchange.getResponseBody(), UTF_8))) {
          writer.accept(out);
        }
      });
    }
  }
}
