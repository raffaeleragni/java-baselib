package baselib.http.rest;

import baselib.http.HttpServer.Context;
import baselib.http.HttpServer.HttpStatus;
import baselib.json.JSONReader;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimpleRESTTest {
  public static record Rec(String a) {}

  REST<Rec, String> rest;
  SimpleREST simple;

  @BeforeEach
  void setup() {
    rest = mock(REST.class);
    simple = SimpleREST.from(Rec.class, rest);
  }

  @Test
  void testConstructorValidation() {
    assertThrows(NullPointerException.class, () -> SimpleREST.from(Rec.class, null));
    assertThrows(NullPointerException.class, () -> SimpleREST.from(null, rest));
  }

  @ParameterizedTest
  @ValueSource(strings = {"value", "test", "anotherstring"})
  void testGetWithStringKey(String value) {
    Context ctx = contextWith("GET", "/", "key", "");

    var mockrec = new Rec(value);
    when(rest.get("key")).thenReturn(of(mockrec));

    var result = simple.apply(ctx);

    verify(rest).get("key");
    assertThat(toRec(result), is(mockrec));
  }

  @Test
  void testNotFound() {
    Context ctx = contextWith("GET", "/", "key", "");

    when(rest.get("key")).thenReturn(empty());


    var ex = assertThrows(HttpStatus.class, () -> simple.apply(ctx));

    assertThat(ex.status(), is(404));
  }

  @Test
  void testPostWithStringKey() {
    Context ctx = contextWith("POST", "/", "key", """
                                                  {"a":"b"}
                                                  """);

    var mockrec = new Rec("b");
    when(rest.post("key", mockrec)).thenReturn(mockrec);

    var result = simple.apply(ctx);

    verify(rest).post("key", mockrec);
    assertThat(toRec(result), is(mockrec));
  }

  @Test
  void testPutWithStringKey() {
    Context ctx = contextWith("PUT", "/", "key", """
                                                 {"a":"b"}
                                                 """);

    var mockrec = new Rec("b");
    when(rest.put("key", mockrec)).thenReturn(mockrec);

    var result = simple.apply(ctx);

    verify(rest).put("key", mockrec);
    assertThat(toRec(result), is(mockrec));
  }

  @Test
  void testPatchWithStringKey() {
    Context ctx = contextWith("PATCH", "/", "key", """
                                                   {"a":"b"}
                                                   """);

    var mockrec = new Rec("b");
    var modModdedRec = new Rec("c");
    when(rest.patch("key", mockrec)).thenReturn(modModdedRec);

    var result = simple.apply(ctx);

    verify(rest).patch("key", mockrec);
    assertThat(toRec(result), is(modModdedRec));
  }

  @Test
  void testDeleteWithStringKey() {
    Context ctx = contextWith("DELETE", "/", "key", "");

    var result = simple.apply(ctx);

    verify(rest).delete("key");
    assertThat(result, is(""));
  }

  @Test
  void testDefault() {
    Context ctx = contextWith("HEAD", "/", "key", "");

    var ex = assertThrows(HttpStatus.class, () -> simple.apply(ctx));

    assertThat(ex.status(), is(415));
  }

  Rec toRec(String result) {
    return JSONReader.toRecord(Rec.class, result);
  }

  Context contextWith(String method, String mappedPath, String variablePath, String body) {
    var ctx = mock(Context.class);
    when(ctx.method()).thenReturn(method);
    when(ctx.mappedPath()).thenReturn(mappedPath);
    when(ctx.variablePath()).thenReturn(variablePath);
    when(ctx.body()).thenReturn(body);
    return ctx;
  }
}


