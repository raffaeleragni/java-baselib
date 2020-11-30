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
import java.io.Reader;
import java.io.StringReader;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Character.isWhitespace;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * JSON reader, it wraps around an actual reader and uses constant memory within
 * reasonable limits.
 * Some string buffering may be involved to interpret literals.
 *
 * toObject() will return an object representation by mapping:
 *   "..." -> String
 *   [...] -> List
 *   {...} -> Map where keys are String
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class JSONReader {

  final Reader reader;
  public JSONReader(final Reader reader) {
    this.reader = reader;
  }

  public static Object fromJSON(final String string) {
    return new JSONReader(new StringReader(string)).toObject();
  }

  public Object toObject() {
    try {
      return readItem(null);
    } finally {
      ex(() -> reader.close());
    }
  }

  private Object readItem(Integer prev) {
    var ch = prev != null ? prev : nextCharNonWhitespace();
    return switch (ch) {
      case '"' -> readString();
      case '[' -> readArray();
      case '{' -> readObject();
      default -> readLiteral(ch);
    };
  }

  private int nextCharNonWhitespace() {
    var ch = nextChar();
    while (isWhitespace(ch)) {
      ch = nextChar();
    }
    return ch;
  }

  private int nextChar() {
    return ex(() -> reader.read());
  }

  private Object readLiteral(int ch) {
    var builder = new StringBuilder();
    while (ch != -1 && ch != ',' && ch != '}' && ch != ']') {
      builder.append((char)ch);
      ch = nextCharNonWhitespace();
    }
    var s = builder.toString();
    if ("null".equalsIgnoreCase(s))
      return null;
    if ("true".equalsIgnoreCase(s))
      return TRUE;
    if ("false".equalsIgnoreCase(s))
      return FALSE;

    try {
      return Integer.valueOf(s);
    } catch (NumberFormatException e) {}

    try {
      return Long.valueOf(s);
    } catch (NumberFormatException e) {}

    try {
      return new BigDecimal(s);
    } catch (NumberFormatException e) {}

    return null;
  }

  private String readString() {
    var builder = new StringBuilder();
    var prev = 0;
    var ch = nextChar();
    while (ch != -1 && (ch != '"' || prev == '\\')) {
      if (ch != '\\')
        builder.append((char)ch);
      prev = ch;
      ch = nextChar();
    }
    return builder.toString();
  }

  private List readArray() {
    var list = new LinkedList<Object>();
    var ch = nextChar();
    while (ch != -1 && ch != ']') {
      list.add(readItem(ch));
      ch = nextCharNonWhitespace();
      if (ch == ',')
        ch = nextCharNonWhitespace();
    }
    return list;
  }

  private Map readObject() {
    var map = new HashMap<String, Object>();
    var ch = nextCharNonWhitespace();
    while (ch != -1 && ch != '}') {
      if (ch != '"')
        break;

      var prop = readString();

      ch = nextCharNonWhitespace();
      if (ch != ':')
        break;

      var value = readItem(null);

      map.put(prop, value);

      ch = nextCharNonWhitespace();
      if (ch != ',')
        break;

      ch = nextCharNonWhitespace();
    }
    return map;
  }
}
