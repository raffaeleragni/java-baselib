package baselib.http.rest;

import baselib.http.HttpServer;
import static baselib.json.JSONBuilder.toJSON;
import static baselib.json.JSONReader.toRecord;
import java.util.Objects;
import java.util.function.Function;

public class SimpleREST<E extends Record> implements Function<HttpServer.Context, String> {
  private static final HttpServer.HttpStatus HTTP_404 = new HttpServer.HttpStatus(404);
  private static final HttpServer.HttpStatus HTTP_415 = new HttpServer.HttpStatus(415);

  Class<E> clazz;
  REST<E, String> rest;

  SimpleREST(Class<E> clazz, REST<E, String> rest) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(rest);
    this.rest = rest;
    this.clazz = clazz;
  }

  public static <E extends Record> SimpleREST<E> from(Class<E> clazz, REST<E, String> rest) {
    return new SimpleREST<>(clazz, rest);
  }

  @Override
  public String apply(HttpServer.Context ctx) {
    return switch (ctx.method()) {
      case "GET" -> getMethod(ctx);
      case "POST" -> postMethod(ctx);
      case "PUT" -> putMethod(ctx);
      case "PATCH" -> patchMethod(ctx);
      case "DELETE" -> deleteMethod(ctx);
      default -> throw HTTP_415;
    };
  }

  String getMethod(HttpServer.Context ctx) {
    return toJSON(rest
      .get(ctx.variablePath())
      .orElseThrow(() -> HTTP_404));
  }

  String postMethod(HttpServer.Context ctx) {
    return toJSON(rest.post(ctx.variablePath(), toRecord(clazz, ctx.body())));
  }

  String putMethod(HttpServer.Context ctx) {
    return toJSON(rest.put(ctx.variablePath(), toRecord(clazz, ctx.body())));
  }

  String patchMethod(HttpServer.Context ctx) {
    return toJSON(rest.patch(ctx.variablePath(), toRecord(clazz, ctx.body())));
  }

  String deleteMethod(HttpServer.Context ctx) {
    rest.delete(ctx.variablePath());
    return "";
  }

}
