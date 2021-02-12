/*
 * Copyright 2021 Raffaele Ragni.
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

import java.nio.file.Path;
import java.util.UUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni
 */
class FSKVTest {
  FSKV<TestRecord> store;

  @BeforeEach
  void setup() {
    var dir = Path.of(System.getProperty("java.io.tmpdir"),
      "storetest" + System.currentTimeMillis());
    store = new FSKV<>(dir, TestRecord.class);
  }

  @Test
  void testPutGet() {
    var rec = new TestRecord(UUID.randomUUID().toString(), "test");

    store.put(rec.uuid(), rec);

    var rec2 = store.get(rec.uuid());

    assertThat(rec, is(rec2));
  }

  @Test
  void testInvalidPaths() {
    var rec = new TestRecord(UUID.randomUUID().toString(), "test");

    assertThrows(IllegalArgumentException.class, () -> store.put("../a", rec));

    assertThrows(IllegalArgumentException.class, () -> store.get("../a"));
  }

}

record TestRecord(String uuid, String name) {}
