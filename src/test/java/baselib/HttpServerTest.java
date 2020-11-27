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

import baselib.HttpServer.HttpStatus;
import static baselib.TestHelper.portOccupied;
import static baselib.TestHelper.requestGet;
import java.util.Map;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
class HttpServerTest {
  private static final int PORT = 54321;

  HttpServer server;

  @BeforeEach
  void setup() {
    server = new HttpServer(PORT, Map.of());
  }

  @Test
  void testServer() {
    assertThat(portOccupied(PORT), is(false));

    server.start();
    assertThat(portOccupied(PORT), is(true));

    server.stop();
    assertThat(portOccupied(PORT), is(false));
  }

  @Test
  void testSimpleBodyOutput() {
    var url = "http://localhost:"+PORT;
    server = new HttpServer(PORT, Map.of(
      "/", ctx -> ctx.response("hello world"),
      "/exception", ctx -> {throw new RuntimeException();},
      "/500", ctx -> {throw new HttpStatus(500);}
    ));
    server.start();

    assertThat(requestGet(url+"/"), is("hello world"));

    assertThrows(RuntimeException.class, () ->
      requestGet(url+"/exception")
    );

    var status = assertThrows(HttpStatus.class, () ->
      requestGet(url+"/500")
    );
    assertThat(status.status(), is(500));

    server.stop();
  }
}
