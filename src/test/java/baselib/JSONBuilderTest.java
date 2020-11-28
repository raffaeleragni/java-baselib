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

import static baselib.JSONBuilder.toJSON;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
class JSONBuilderTest {
  JSONBuilder jb;

  @BeforeEach
  void setup() {
    jb = new JSONBuilder();
  }

  @Test
  void testEmpty() {
    jb.beginObject();
    jb.endObject();
    assertThat(jb.toString(), is("{}"));
  }

  @Test
  void testObjectProperty() {
    verifyProperty("property\"/</", "{\"property\\\"/<\\/\":{}}");
  }

  @Test
  void testObjectPropertyMoreEscapes() {
    verifyProperty("property\b\t\n\f\r", "{\"property\\b\\t\\n\\f\\r\":{}}");
  }

  @Test
  void testObjectPropertyLowChars() {
    verifyProperty("property\u0010", "{\"property\\u0010\":{}}");
  }

  @Test
  void testObjectPropertyhighChars() {
    verifyProperty("property\u0080\u00a1\u2000\u2101", "{\"property\\u0080\u00a1\\u2000\u2101\":{}}");
  }

  void verifyProperty(String name, String result) {
    jb.beginObject();

    jb.property(name);
    jb.beginObject();
    jb.endObject();

    jb.endObject();

    assertThat(jb.toString(), is(result));
  }

  @Test
  void testDouble() {
    jb.beginObject();

    jb.property("text");
    jb.beginObject();
    jb.endObject();

    jb.property("text2");
    jb.beginObject();
    jb.endObject();

    jb.endObject();
    assertThat(jb.toString(), is("{\"text\":{},\"text2\":{}}"));
  }

  @Test
  void testValues() {
    jb.beginObject();

    jb.property("text");
    jb.value("asd");

    jb.property("text2");
    jb.value(null);

    jb.property("int");
    jb.value(1);

    jb.endObject();
    assertThat(jb.toString(), is("{\"text\":\"asd\",\"text2\":null,\"int\":1}"));
  }

  @Test
  void testArray() {
    jb.beginArray();

    jb.value(1);
    jb.value(3);

    jb.beginObject();
    jb.property("text");
    jb.value("asd");
    jb.endObject();

    jb.endArray();

    assertThat(jb.toString(), is("[1,3,{\"text\":\"asd\"}]"));
  }

  @Test
  void testBolean() {
    jb.value(true);
    assertThat(jb.toString(), is("true"));
  }

  @Test
  void testDate() {
    var now = Instant.now();
    var snow = now.toString();
    jb.value(now);
    assertThat(jb.toString(), is('"' + snow + '"'));
  }

  @Test
  void testList() {
    jb.value(List.of(1, 2, 3));
    assertThat(jb.toString(), is("[1,2,3]"));
  }

  @Test
  void testMap() {
    jb.value(Map.of("test", "asd"));
    assertThat(jb.toString(), is("{\"test\":\"asd\"}"));
  }

  @Test
  void testNativeArray() {
    jb.value(new int[]{3, 2, 1});
    assertThat(jb.toString(), is("[3,2,1]"));
  }

  @Test
  void testNativeCharArray() {
    jb.value(new char[]{'a', 'b', 'c'});
    assertThat(jb.toString(), is("[\"a\",\"b\",\"c\"]"));
  }

  @Test
  void testObjectArray() {
    assertThat(toJSON(new Character[]{'a', 'b', 'c'}), is("[\"a\",\"b\",\"c\"]"));
  }

  @Test
  void testRecord() {
    var rec = new My(1, "c", Map.of("a", "b"));
    assertThat(toJSON(rec), is("{\"x\":1,\"a\":\"c\",\"map\":{\"a\":\"b\"}}"));
  }
}

record My(int x, String a, Map<String, String> map) {}
