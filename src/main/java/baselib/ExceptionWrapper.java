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

/**
 * Wraps checked exceptions into a lambda.
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public final class ExceptionWrapper {

  private ExceptionWrapper() {
  }

  /**
   * Wraps checked exceptions into IllegalStateException.
   * This is the case of a callable that returns a value, the value is also
   * returned by this function.
   *
   * @param <T> type for a return function
   * @param callable the function to be wrapped, that throws checked exceptions
   * @return the return value that the callable function returns
   */
  public static <T> T ex(final WrapperT<T> callable) {
    try {
      return callable.call();
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException(ex.getMessage(), ex);
    }
  }

  /**
   * Wraps checked exceptions into IllegalStateException.
   * This is the case of a function that has no return value.
   *
   * @param callable the function to be wrapped, that throws checked exceptions
   */
  public static void ex(final Wrapper callable) {
    try {
      callable.call();
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException(ex.getMessage(), ex);
    }
  }

  interface Wrapper {
    void call() throws Exception; //NOSONAR
  }

  interface WrapperT<T> {
    T call() throws Exception; //NOSONAR
  }
}
