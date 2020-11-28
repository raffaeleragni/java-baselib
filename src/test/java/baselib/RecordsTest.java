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
import java.util.Map;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
class RecordsTest {

  @Test
  void testNoRecord() {
    assertThrows(IllegalArgumentException.class, () -> {
      Records.toMap(new Object());
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
    assertThrows(IllegalArgumentException.class, () -> fromMap(Object.class, Map.of()));
  }

  @Test
  void testToRecord() {
    var rec = Records.fromMap(Sample.class, Map.of("id", 1, "name", "test"));
    var expected = new Sample(1, "test");
    assertThat(rec, is(expected));
  }
}

record Sample(int id, String name) {}
record Nested(boolean visible, Sample sample) {}
