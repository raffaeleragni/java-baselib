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

package baselib.metrics;

import java.io.StringWriter;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
class MetricsExporterTest {
  MetricsExporter exporter;

  @BeforeEach
  void setup() {
    exporter = new MetricsExporter();
  }

  @Test
  void testMemoryExport() {
    String metrics = exportMetrics();

    assertThat(metrics, containsString("jvm_memory_bytes_used{area=\"heap\"} "));
    assertThat(metrics, containsString("jvm_memory_bytes_used{area=\"nonheap\"} "));
    assertThat(metrics, containsString("jvm_memory_bytes_committed{area=\"heap\"} "));
    assertThat(metrics, containsString("jvm_memory_bytes_committed{area=\"nonheap\"} "));
    assertThat(metrics, containsString("jvm_memory_bytes_max{area=\"heap\"} "));
    assertThat(metrics, containsString("jvm_memory_bytes_max{area=\"nonheap\"} "));
    assertThat(metrics, containsString("jvm_memory_bytes_init{area=\"heap\"} "));
    assertThat(metrics, containsString("jvm_memory_bytes_init{area=\"nonheap\"} "));
  }

  @Test
  void testClassLoaderExport() {
    String metrics = exportMetrics();

    assertThat(metrics, containsString("jvm_classes_loaded "));
    assertThat(metrics, containsString("jvm_classes_loaded_total "));
    assertThat(metrics, containsString("jvm_classes_unloaded_total "));
  }

  @Test
  void testThreadsExport() {
    String metrics = exportMetrics();

    assertThat(metrics, containsString("jvm_threads_current "));
    assertThat(metrics, containsString("jvm_threads_daemon "));
    assertThat(metrics, containsString("jvm_threads_peak "));
    assertThat(metrics, containsString("jvm_threads_started_total "));
    assertThat(metrics, containsString("jvm_threads_deadlocked "));
    assertThat(metrics, containsString("jvm_threads_deadlocked_monitor "));
  }

  private String exportMetrics() {
    var sw = new StringWriter();
    exporter.export(sw);
    var metrics = sw.toString();
    return metrics;
  }
}
