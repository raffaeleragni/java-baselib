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
import java.io.StringReader;
import org.openjdk.jmh.annotations.Benchmark;

/**
 *
 * @author Raffaele Ragni
 */
public class JSONReaderBenchmark {
  public record TestRecord(int id, String name) {}

  public static void main(String[] args) {
    run(JSONReaderBenchmark.class);
  }

  @Benchmark
  public void runManual() {
    try (var r = new JSONReader(new StringReader(JSON))) {
      r.toObject();
    }
  }

  @Benchmark
  public void intoRecord() {
    try (var r = new JSONReader(new StringReader(JSON))) {
      r.toRecordList(TestRecord.class);
    }
  }

  private static final String JSON = """
                                     [{"id":1, "name":"one", "id":2,"name":"two"}]
                                     """;

}