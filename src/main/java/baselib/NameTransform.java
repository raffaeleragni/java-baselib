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

import java.util.function.Function;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.compile;

/**
 * Thread safe.
 * @author Raffaele Ragni
 */
@FunctionalInterface
public interface NameTransform extends Function<String, String> {
  final NameTransform NONE = x -> x;
  final NameTransform SNAKE = new ToSplitCase('_');
  final NameTransform KEBAB = new ToSplitCase('-');

  @Override
  public default String apply(String t) {
    return transform(t);
  }

  String transform(String from);
}

class ToSplitCase implements NameTransform {

  final Pattern pattern = compile("(?<=[a-z0-9])[A-Z]");
  final char separator;

  public ToSplitCase(char separator) {
    this.separator = separator;
  }

  @Override
  public String transform(String from) {
    if (from == null)
      return null;
    return pattern.matcher(from)
      .replaceAll(match -> separator + match.group().toLowerCase())
      .toLowerCase();
  }

}