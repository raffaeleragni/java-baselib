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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a simple http server with basic url mappings.
 * Supported by the standard jvm http server.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public final class HttpServer {
  private static final Logger LOGGER = Logger.getLogger(HttpServer.class.getName());
  private static final int HTTP_ERROR_CODE = 500;
  private static final int HTTP_OK = 200;
  private final int port;
  private final com.sun.net.httpserver.HttpServer server; //NOSONAR

  private HttpServer(
      final int serverPort,
      final int threads,
      final Map<String, Function<Context, String>> handlers) {

    this.port = serverPort;
    this.server = ex(() -> //NOSONAR
        com.sun.net.httpserver.HttpServer.create()); //NOSONAR
    this.server.setExecutor(Executors.newFixedThreadPool(threads));
    Objects.requireNonNull(handlers.entrySet())
      .stream()
      .filter(e -> Objects.nonNull(e.getValue()))
      .forEach(e ->
        server.createContext(e.getKey(), exchange ->
            wrapExchange(e.getKey(), exchange, e.getValue()))
      );
  }

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
   * @return
   */
  public static HttpServer create(final int serverPort,
      final int threads,
      final Map<String, Function<Context, String>> handlers) {
    return new HttpServer(serverPort, threads, handlers);
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
     *
     * @return fetch the request body, useful for POSTs
     */
    String body();

    /**
     * Write a string as a response.
     * After this function the response will be in status 200 and the output
     * will be closed, ending the response.
     * @param body A full string to be written into the response.
     */
    void response(String body);

    /**
     * This method is used to output in a streamed way.
     * Use only a single outputing technique, either this or response(String)
     * or any other new outputing method, but don't mix them.
     * @param consumer lambda to consume the writer
     */
    void writer(Consumer<BufferedWriter> consumer);

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
      final Function<Context, String> function) {

    var ctx = new ContextImpl(originalMappedPath, exchange);
    try {
      var resp = function.apply(ctx);
      if (resp != null && !resp.isEmpty())
        ctx.response(resp);

      if (!ctx.responseSent())
        ex(() -> {
          exchange.sendResponseHeaders(HTTP_OK, 0);
          exchange.getResponseBody().close();
        });
    } catch (HttpStatus e) {
      ex(() -> {
        exchange.sendResponseHeaders(e.status(), 0);
        exchange.getResponseBody().close();
      });
    } catch (RuntimeException e) {
      ex(() -> {
        exchange.sendResponseHeaders(HTTP_ERROR_CODE, 0);
        exchange.getResponseBody().close();
      });
      LOGGER.log(Level.SEVERE, e, e::getMessage);
    }
  }

  private static class ContextImpl implements Context {

    private final String path;
    private final String variablePath;
    private final HttpExchange exchange;
    private boolean responseSent;

    ContextImpl(
        final String originalMappedPath,
        final HttpExchange httpExchange) {

      this.path = originalMappedPath;
      this.exchange = httpExchange;
      var extraPath = exchange
          .getRequestURI()
          .getPath()
          .substring(path.length());
      if (extraPath.startsWith("/"))
        extraPath = extraPath.substring(1);
      if (extraPath.endsWith("/"))
        extraPath = extraPath.substring(0, extraPath.length() - 1);
      this.variablePath = extraPath;
      this.responseSent = false;
    }

    @Override
    public String body() {
      return consumeReader(in -> ex(() -> {
        var w = new StringWriter();
        in.transferTo(w);
        return w.toString();
      }));
    }

    @Override
    public void response(final String body) {
      if (responseSent)
        return;
      consumeWriter(out ->
        ex(() -> {
          exchange.sendResponseHeaders(HTTP_OK, body.length());
          out.write(body);
          responseSent = true;
        })
      );
    }

    @Override
    public void writer(Consumer<BufferedWriter> consumer) {
      if (responseSent)
        return;
      ex(() -> {
        exchange.sendResponseHeaders(HTTP_OK, 0);
        consumeWriter(consumer);
        responseSent = true;
      });
    }

    @Override
    public String mappedPath() {
      return path;
    }

    @Override
    public String variablePath() {
      return variablePath;
    }

    boolean responseSent() {
      return responseSent;
    }

    private <T> T consumeReader(final Function<BufferedReader, T> reader) {
      return ex(() -> {
        try (var in = new BufferedReader(
            new InputStreamReader(exchange.getRequestBody(), UTF_8))) {
          return reader.apply(in);
        }
      });
    }

    private void consumeWriter(final Consumer<BufferedWriter> writer) {
      ex(() -> {
        try (var out = new BufferedWriter(
            new OutputStreamWriter(exchange.getResponseBody(), UTF_8))) {
          writer.accept(out);
        }
      });
    }
  }
}
