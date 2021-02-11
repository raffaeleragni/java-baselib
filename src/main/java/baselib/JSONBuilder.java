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
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public final class JSONBuilder {

  private final StringBuilder sb;
  private boolean comma;
  private boolean prop;

  public JSONBuilder() {
    this.sb = new StringBuilder();
  }

  public static String toJSON(Object value) {
    var jb = new JSONBuilder();
    jb.value(value);
    return jb.toString();
  }

  @Override
  public String toString() {
    return sb.toString();
  }

  public void beginArray() {
    if (comma)
      sb.append(',');
    sb.append('[');
    comma = false;
    prop = false;
  }

  public void endArray() {
    sb.append(']');
    comma = true;
    prop = false;
  }

  public void beginObject() {
    if (comma)
      sb.append(',');
    sb.append('{');
    comma = false;
    prop = false;
  }

  public void endObject() {
    sb.append('}');
    comma = true;
    prop = false;
  }

  public void property(final String property) {
    Objects.requireNonNull(property);

    if (comma)
      sb.append(',');

    sb.append('"');
    sanitizeAppend(property);
    sb.append('"');
    sb.append(':');

    comma = false;
    prop = true;
  }

  public void value(Object o) {
    if (comma && !prop)
      sb.append(',');
    comma = false;

    if (o instanceof Optional op)
      o = op.orElse(null);

    if (o == null) {
      sb.append("null");
    } else if (o instanceof String) {
      doString(o.toString());
    } else if (o instanceof Character) {
      doString(o.toString());
    } else if (o instanceof Number) {
      doLiteral(o);
    } else if (o instanceof Boolean) {
      doLiteral(o);
    } else if (o instanceof Map m) {
      doMap(m);
    } else if (o instanceof Collection c) {
      doCollection(c);
    } else if (o.getClass().isArray()) {
      doArray(o);
    } else if (o.getClass().isRecord()) {
      doRecord(o);
    } else {
      doString(o.toString());
    }

    comma = true;
    prop = false;
  }

  private void doRecord(Object o) {
    beginObject();
    for (var e: o.getClass().getRecordComponents()) {
      if (!e.getAccessor().canAccess(o))
        continue;

      Object value = ex(() -> e.getAccessor().invoke(o));
      if (value != null) {
        property(e.getName());
        value(value);
      }
    }
    endObject();
  }

  private void doLiteral(Object o) {
    sb.append(o.toString());
  }

  private void doString(String s) {
    sb.append('"');
    sanitizeAppend(s);
    sb.append('"');
  }

  private void doArray(Object o) {
    beginArray();
    if (o.getClass().getComponentType().isPrimitive()) {
      int length = Array.getLength(o);
      for (int i = 0; i < length; i++) {
        value(Array.get(o, i));
      }
    } else {
      Object[] objects = (Object[]) o;
      for (Object obj : objects) {
        if (obj != null)
          value(obj);
      }
    }
    endArray();
  }

  private void doCollection(Collection<?> c) {
    beginArray();
    c.stream().filter(Objects::nonNull).forEach(this::value);
    endArray();
  }

  private void doMap(Map<?,?> m) {
    beginObject();
    m.entrySet().stream()
      .filter(e -> Objects.nonNull(e.getKey()))
      .filter(e -> Objects.nonNull(e.getValue()))
      .forEach(e -> {
        property(e.getKey().toString());
        value(e.getValue());
      });
    endObject();
  }

  private void sanitizeAppend(final String s) {
    char b;
    char c = 0;
    int i;
    int len = s.length();
    for (i = 0; i < len; i += 1) {
      b = c;
      c = s.charAt(i);
      switch (c) {
        case '\\', '"' -> { //NOSONAR
          sb.append('\\');
          sb.append(c);
        }
        case '/' -> { //NOSONAR
          if (b == '<')
            sb.append('\\');
          sb.append(c);
        }
        case '\b' -> sb.append("\\b");
        case '\t' -> sb.append("\\t");
        case '\n' -> sb.append("\\n");
        case '\f' -> sb.append("\\f");
        case '\r' -> sb.append("\\r");
        default -> { //NOSONAR
          if (c < ' ' || c >= '\u0080' && c < '\u00a0' || c >= '\u2000' && c < '\u2100')
            sb.append("\\u%04x".formatted((int)c));
          else
            sb.append(c);
        }
      }
    }
  }
}
