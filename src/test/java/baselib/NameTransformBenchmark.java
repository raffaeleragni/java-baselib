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

import static baselib.extra.BenchmarkRun.run;
import org.openjdk.jmh.annotations.Benchmark;

/**
 *
 * @author Raffaele Ragni
 */
public class NameTransformBenchmark {
  public static void main(String[] args) {
    run(NameTransformBenchmark.class);
  }

  @Benchmark
  public void withSnake() {
    NameTransform.SNAKE.transform("wordWithAnotherWord");
  }

  @Benchmark
  public void withKebab() {
    NameTransform.KEBAB.transform("wordWithAnotherWord");
  }
}
