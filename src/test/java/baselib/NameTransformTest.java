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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 *
 * @author Raffaele Ragni
 */
class NameTransformTest {

  @Test
  void testNull() {
    assertThat(NameTransform.NONE.transform(null), is(nullValue()));
    assertThat(NameTransform.SNAKE.transform(null), is(nullValue()));
  }

  @ParameterizedTest
  @CsvSource(value = {
    ",",
    "a,a",
    "aA,aA",
    "wordWithAnotherWord,wordWithAnotherWord",
    "snake_case_words,snake_case_words",
    "kebab-case-words,kebab-case-words",
  })
  void testNoTransformer(String from, String to) {
    assertTransformed(NameTransform.NONE, from, to);
  }

  @ParameterizedTest
  @CsvSource(value = {
    "a,a",
    "A,a",
    "aA,a_a",
    "wordWithAnotherWord,word_with_another_word",
    "word2Numbers,word2_numbers",
    "word2numbers,word2numbers"
  })
  void testCamelToSnake(String from, String to) {
    assertTransformed(NameTransform.SNAKE, from, to);
  }

  @ParameterizedTest
  @CsvSource(value = {
    "a,a",
    "A,a",
    "aA,a-a",
    "wordWithAnotherWord,word-with-another-word",
    "word2Numbers,word2-numbers",
    "word2numbers,word2numbers"
  })
  void testCamelToKebab(String from, String to) {
    assertTransformed(NameTransform.KEBAB, from, to);
  }

  private void assertTransformed(NameTransform transformer, String from, String to) {
    assertThat(transformer.transform(from), is(to));
    assertThat(transformer.apply(from), is(to));
  }
}
