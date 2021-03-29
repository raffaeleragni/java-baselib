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
package baselib.storage;

import static baselib.ExceptionWrapper.ex;
import static baselib.json.JSONBuilder.toJSON;
import static baselib.json.JSONReader.toRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * File system based key-value store.
 * @param <V> type of the storage record.
 * @author Raffaele Ragni
 */
public class FSKV<V> {
  static final String EXTENSION = ".json";
  final Path dir;
  final Class<V> clazz;

  public FSKV(Path dir, Class<V> clazz) {
    Objects.requireNonNull(dir);
    Objects.requireNonNull(clazz);
    ex(() -> Files.createDirectories(dir));

    this.dir = dir;
    this.clazz = clazz;
  }

  public void put(String uuid, V rec) {
    Objects.requireNonNull(uuid);
    Objects.requireNonNull(rec);

    var itemPath = dir.resolve(uuid + EXTENSION).normalize();
    ensureNotParented(itemPath);

    var itemString = toJSON(rec);
    ex(() -> Files.writeString(itemPath, itemString));
  }

  final void ensureNotParented(Path itemPath) {
    if (!itemPath.startsWith(dir))
      throw new IllegalArgumentException("Path is not absolute");
  }

  public V get(String uuid) {
    Objects.requireNonNull(uuid);

    var itemPath = dir.resolve(uuid + EXTENSION).normalize();
    ensureNotParented(itemPath);

    var itemString = ex(() -> Files.readString(itemPath));
    return toRecord(clazz, itemString);
  }
}
