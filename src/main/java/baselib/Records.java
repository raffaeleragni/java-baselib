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

import static baselib.ExceptionWrapper.ex;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public final class Records {

  private Records() {
  }

  public static Map<String, Object> toMap(Object rec) {
    Objects.requireNonNull(rec);
    var result = new HashMap<String, Object>();
    if (!rec.getClass().isRecord())
      throw new IllegalArgumentException();

    for (var e: rec.getClass().getRecordComponents()) {
      if (!e.getAccessor().canAccess(rec))
        continue;
      var value = ex(() -> e.getAccessor().invoke(rec));
      if (value.getClass().isRecord())
        result.put(e.getName(), toMap(value));
      else
        result.put(e.getName(), value);
    }

    return result;
  }

  public static <T> T fromMap(Class<T> clazz, Map<String, Object> map) {
    if (map == null)
      return null;
    return fromPropertyDiscover(clazz, map::get);
  }

  public static <T> T fromPropertyDiscover(Class<T> clazz, Function<String, Object> fetch) {
    if (fetch == null)
      return null;
    if (!clazz.isRecord())
      throw new IllegalArgumentException();

    var params = new LinkedList<Object>();
    var types = new LinkedList<Class<?>>();
    for (var e: clazz.getRecordComponents()) {
      types.add(e.getType());
      params.add(fetch.apply(e.getName()));
    }

    var constructor = ex(() -> clazz.getDeclaredConstructor(types.toArray(new Class[]{})));

    return ex(() -> constructor.newInstance(params.toArray()));
  }
}
