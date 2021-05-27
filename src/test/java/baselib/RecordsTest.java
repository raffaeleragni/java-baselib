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

import static baselib.Records.fromMap;
import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
class RecordsTest {

  public record Sample(int id, String name) {}
  public record Nested(boolean visible, Sample sample) {}
  public record SampleMoreNames(int id, String nameDifferent) {}

  @Test
  void testNoRecord() {
    var o = new Object();
    assertThrows(IllegalArgumentException.class, () -> {
      Records.toMap(o);
    });
  }

  @Test
  void testRecord() {
    var rec = new Sample(1, "test");
    var map = Records.toMap(rec);
    assertThat(map, is(Map.of("id", 1, "name", "test")));
  }

  @Test
  void testNested() {
    var rec = new Sample(1, "test");
    var nest = new Nested(true, rec);
    var map = Records.toMap(nest);
    assertThat(map, is(Map.of("visible", true, "sample", Map.of("id", 1, "name", "test"))));
  }

  @Test
  void testToRecordFromNull() {
    var rec = fromMap(Sample.class, null);
    assertThat(rec, is(nullValue()));
  }

  @Test
  void testToNotRecord() {
    var map = new HashMap<String, Object>();
    assertThrows(IllegalArgumentException.class, () -> fromMap(Object.class, map));
  }

  @Test
  void testToRecord() {
    var rec = Records.fromMap(Sample.class, Map.of("id", 1, "name", "test"));
    var expected = new Sample(1, "test");
    assertThat(rec, is(expected));
  }

  @Test
  void testToNedtedRecord() {
    var map = Map.of("visible", true, "sample", Map.of("id", 1, "name", "test"));
    var rec = new Sample(1, "test");
    var expected = new Nested(true, rec);

    assertThat(Records.fromMap(Nested.class, map), is(expected));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "nameDifferent",
    "name_different",
    "NAME_DIFFERENT",
    "name-different",
    "NAME-DIFFERENT"})
  void testNameCaseNone() {
    var rec = Records.fromMap(SampleMoreNames.class, Map.of("id", 1, "nameDifferent", "test"));
    var expected = new SampleMoreNames(1, "test");
    assertThat(rec, is(expected));
  }
}