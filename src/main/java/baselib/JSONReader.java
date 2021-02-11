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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

  public static Object toObject(final String string) {
    return new JSONReader(new StringReader(string)).toObject();
  }

  public static <T> T toRecord(Class<T> clazz, final String string) {
    return new JSONReader(new StringReader(string)).toRecord(clazz);
  }

  public static <T> List<T> toRecordList(Class<T> clazz, final String string) {
    return new JSONReader(new StringReader(string)).toRecordList(clazz);
  }

  public Object toObject() {
    try {
      return readItem(null);
    } finally {
      ex(reader::close);
    }
  }

  public <T> T toRecord(Class<T> clazz) {
    if (clazz == null || !clazz.isRecord())
      throw new IllegalArgumentException("Class need to be of record type.");

    var ch = nextCharNonWhitespace();
    if (ch != '{')
      return null;

    var map = readObject();
    if (map.isEmpty())
      return null;

    return Records.fromMap(clazz, map);
  }

  public <T> List<T> toRecordList(Class<T> clazz) {
    if (clazz == null || !clazz.isRecord())
      throw new IllegalArgumentException("Class need to be of record type.");

    var ch = nextCharNonWhitespace();
    if (ch != '[')
      return null;//NOSONAR

    var list = new LinkedList<T>();
    walkThroughJSONArray(o -> {
      var map = (Map<String, Object>) o;
      if (map == null || map.isEmpty())
        return;
      list.add(Records.fromMap(clazz, map));
    });

    return list;
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
    return ex(() -> reader.read()); //NOSONAR
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
    } catch (NumberFormatException e) {} //NOSONAR

    try {
      return Long.valueOf(s);
    } catch (NumberFormatException e) {} //NOSONAR

    try {
      return new BigDecimal(s);
    } catch (NumberFormatException e) {} //NOSONAR

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

  private List readArray() { //NOSONAR
    var list = new LinkedList<Object>();
    walkThroughJSONArray(list::add);
    return list;
  }

  private Map<String, Object> readObject() {
    var map = new HashMap<String, Object>();
    walkThroughJSONObject(map::put);
    return map;
  }

  private void walkThroughJSONArray(Consumer<Object> fn) {
    var ch = nextChar();
    while (ch != -1 && ch != ']') {
      fn.accept(readItem(ch));
      ch = nextCharNonWhitespace();
      if (ch == ',')
        ch = nextCharNonWhitespace();
    }
  }

  private void walkThroughJSONObject(BiConsumer<String, Object> fn) {
    var ch = nextCharNonWhitespace();
    while (ch != -1 && ch != '}') { //NOSONAR
      if (ch != '"')
        break;

      var prop = readString();
      ch = nextCharNonWhitespace();
      if (ch != ':')
        break;

      var value = readItem(null);
      fn.accept(prop, value);

      ch = nextCharNonWhitespace();
      if (ch == ',')
        ch = nextCharNonWhitespace();
    }
  }
}
