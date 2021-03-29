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

import baselib.ExceptionWrapper.Wrapper;
import baselib.ExceptionWrapper.WrapperT;
import static baselib.ExceptionWrapper.ex;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
class ExceptionWrapperTest {

  @Test
  void testNoThrows() {
    assertThat(ex(() -> 1), is(1));
  }

  @Test
  void testRuntimeException() {
    assertThrows(RuntimeException.class, () -> ex((Wrapper)() -> {throw new RuntimeException();}));
  }

  @Test
  void testException() {
    assertThrows(IllegalStateException.class, () -> ex((Wrapper)() -> {throw new Exception();}));
  }

  @Test
  void testRuntimeExceptionR() {
    assertThrows(RuntimeException.class, () -> ex((WrapperT)() -> {throw new RuntimeException();}));
  }

  @Test
  void testExceptionR() {
    assertThrows(IllegalStateException.class, () -> ex((WrapperT)() -> {throw new Exception();}));
  }
}
