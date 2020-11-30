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

import static baselib.JSONReader.fromJSON;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
class JSONReaderTest {

  @Test
  void testEmptyResult() {
    assertThat(fromJSON(""), is(nullValue()));
    assertThat(fromJSON("null"), is(nullValue()));
    assertThat(fromJSON("{}"), is(Map.of()));
  }

  @Test
  void testString() {
    assertThat(fromJSON("\"\"   "), is(""));
    assertThat(fromJSON("  \"asd\""), is("asd"));
    assertThat(fromJSON("\t\t\"as\\\"d\""), is("as\"d"));
  }

  @Test
  void testLiteralsBooleans() {
    assertThat(fromJSON("true"), is(true));
    assertThat(fromJSON("false"), is(false));
  }

  @Test
  void testLiteralsNumbers() {
    assertThat(fromJSON("1"), is(1));
    assertThat(fromJSON("2"), is(2));
    assertThat(fromJSON("3"), is(3));
  }

  @Test
  void testLiteralsDecimals() {
    assertThat(fromJSON("1.1"), is(new BigDecimal("1.1")));
  }

  @Test
  void testArrays() {
    assertThat(fromJSON("[1, 2, 3]"), is(List.of(1, 2, 3)));
  }

  @Test
  void testObjects() {
    assertThat(fromJSON("""
                        {
                          "a": "b",
                          "c": 1
                        }
                        """),
        is(Map.of("a", "b", "c", 1)));


    assertThat(fromJSON("""
                        {
                          "a": "b",
                          "c": {
                            "d": 5
                          }
                        }
                        """),
        is(Map.of("a", "b", "c", Map.of("d", 5))));
  }
}
