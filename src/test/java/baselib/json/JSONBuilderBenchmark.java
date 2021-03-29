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
package baselib.json;

import static baselib.extra.BenchmarkRun.run;
import org.openjdk.jmh.annotations.Benchmark;

/**
 *
 * @author Raffaele Ragni
 */
public class JSONBuilderBenchmark {
  public record TestRecord(int id, String name) {}

  public static void main(String[] args) {
    run(JSONBuilderBenchmark.class);
  }

  @Benchmark
  public void runManual() {
    var b = new JSONBuilder();
    b.beginArray();
    for (int i = 0; i < 10; i++)
      addItem(b);
    b.endArray();
  }

  void addItem(JSONBuilder b) {
    b.beginObject();
    b.property("id");
    b.value(1);
    b.property("name");
    b.value("hello");
    b.endObject();
  }

  @Benchmark
  public void runRecord() {
    var b = new JSONBuilder();
    b.beginArray();
    for (int i = 0; i < 10; i++)
      b.value(new TestRecord(1, "test"));
    b.endArray();
  }

}
