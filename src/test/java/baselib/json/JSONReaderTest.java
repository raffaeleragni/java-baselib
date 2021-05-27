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

package baselib.json;

import static baselib.json.JSONReader.toObject;
import static baselib.json.JSONReader.toRecord;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
class JSONReaderTest {
  public record JsonRecord(int id, String name) {}
  public record JsonRecordGrouped(int id, JsonRecord rec) {}

  @Test
  void testEmptyResult() {
    assertThat(toObject(""), is(nullValue()));
    assertThat(toObject("null"), is(nullValue()));
    assertThat(toObject("{}"), is(Map.of()));
  }

  @Test
  void testString() {
    assertThat(toObject("\"\"   "), is(""));
    assertThat(toObject("  \"asd\""), is("asd"));
    assertThat(toObject("\t\t\"as\\\"d\""), is("as\"d"));
  }

  @Test
  void testLiteralsBooleans() {
    assertThat(toObject("true"), is(true));
    assertThat(toObject("false"), is(false));
  }

  @Test
  void testLiteralsNumbers() {
    assertThat(toObject("1"), is(1));
    assertThat(toObject("2"), is(2));
    assertThat(toObject("3"), is(3));
  }

  @Test
  void testLiteralsDecimals() {
    assertThat(toObject("1.1"), is(new BigDecimal("1.1")));
  }

  @Test
  void testArrays() {
    assertThat(toObject("[1, 2, 3]"), is(List.of(1, 2, 3)));
  }

  @Test
  void testObjects() {
    assertThat(toObject("""
                        {
                          "a": "b",
                          "c": 1
                        }
                        """),
        is(Map.of("a", "b", "c", 1)));


    assertThat(toObject("""
                        {
                          "a": "b",
                          "c": {
                            "d": 5
                          }
                        }
                        """),
        is(Map.of("a", "b", "c", Map.of("d", 5))));
  }

  @Test
  void testNoRecordToRecord() {
    assertThrows(IllegalArgumentException.class, () -> {
      JSONReader.toRecord(null, "");
    });
    assertThrows(IllegalArgumentException.class, () -> {
      JSONReader.toRecord(Object.class, "");
    });
  }

  @Test
  void testRecord() {
    var rec = JSONReader.toRecord(JsonRecord.class,
    """
    {
      "id": 1,
      "name": "test"
    }
    """);

    assertThat(rec, is(new JsonRecord(1, "test")));
  }

  @Test
  void testNestedRecord() {
    var rec = new JsonRecord(2, "nested");
    var expected = new JsonRecordGrouped(1, rec);
    assertThat(toRecord(JsonRecordGrouped.class,
        """
        {
          "id": 1,
          "rec": {
            "id": 2,
            "name": "nested"
          }
        }
        """),
        is(expected));
  }

  @Test
  void testNonRecordList() {
    assertThat(JSONReader.toRecordList(JsonRecord.class, "{}"), is(nullValue()));
  }

  @Test
  void testRecordList() {
    var rec = JSONReader.toRecordList(JsonRecord.class,
    """
     [{
       "id": 1,
       "name": "test1"
     }, {
       "id": 2,
       "name": "test2"
     }, {
       "id": 3,
       "name": "test3"
     }]
    """);

    assertThat(rec, is(List.of(
      new JsonRecord(1, "test1"),
      new JsonRecord(2, "test2"),
      new JsonRecord(3, "test3")
    )));
  }

  @Test
  void testCloseCalled() throws IOException {
    var reader = mock(Reader.class);
    var jreader = new JSONReader(reader);

    jreader.close();
    verify(reader).close();
  }
}

