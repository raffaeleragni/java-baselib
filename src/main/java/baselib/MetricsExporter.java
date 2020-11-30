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
import java.io.IOException;
import java.io.Writer;
import static java.lang.String.valueOf;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class MetricsExporter {
  public static final MetricsExporter DEFAULT = new MetricsExporter();

  private final MemoryMXBean memory;
  private final ClassLoadingMXBean classload;
  private final ThreadMXBean threads;

  public MetricsExporter() {
    this.memory = ManagementFactory.getMemoryMXBean();
    this.classload = ManagementFactory.getClassLoadingMXBean();
    this.threads = ManagementFactory.getThreadMXBean();
  }

  public void export(Writer writer) {
    ex(() -> appendJVMMetrics(writer));
    ex(() -> appendClassLoaderMetrics(writer));
    ex(() -> appendThreadsMetrics(writer));
  }

  private void appendJVMMetrics(Writer writer) throws IOException {
    var heap = memory.getHeapMemoryUsage();
    var nonheap = memory.getNonHeapMemoryUsage();
    writeMetric(writer, "jvm_memory_bytes_used{area=\"heap\"}", heap.getUsed());
    writeMetric(writer, "jvm_memory_bytes_used{area=\"nonheap\"}", nonheap.getUsed());
    writeMetric(writer, "jvm_memory_bytes_committed{area=\"heap\"}", heap.getCommitted());
    writeMetric(writer, "jvm_memory_bytes_committed{area=\"nonheap\"}", nonheap.getCommitted());
    writeMetric(writer, "jvm_memory_bytes_max{area=\"heap\"}", heap.getMax());
    writeMetric(writer, "jvm_memory_bytes_max{area=\"nonheap\"}", nonheap.getMax());
    writeMetric(writer, "jvm_memory_bytes_init{area=\"heap\"}", heap.getInit());
    writeMetric(writer, "jvm_memory_bytes_init{area=\"nonheap\"}", nonheap.getInit());
  }

  private void appendClassLoaderMetrics(Writer writer) throws IOException {
    writeMetric(writer, "jvm_classes_loaded", classload.getLoadedClassCount());
    writeMetric(writer, "jvm_classes_loaded_total", classload.getTotalLoadedClassCount());
    writeMetric(writer, "jvm_classes_unloaded_total", classload.getUnloadedClassCount());
  }

  private void appendThreadsMetrics(Writer writer) throws IOException {
    writeMetric(writer, "jvm_threads_current", threads.getThreadCount());
    writeMetric(writer, "jvm_threads_daemon", threads.getDaemonThreadCount());
    writeMetric(writer, "jvm_threads_peak", threads.getPeakThreadCount());
    writeMetric(writer, "jvm_threads_started_total", threads.getTotalStartedThreadCount());
    writeMetric(writer, "jvm_threads_deadlocked", len(threads.findDeadlockedThreads()));
    writeMetric(writer, "jvm_threads_deadlocked_monitor", len(threads.findMonitorDeadlockedThreads()));
  }

  private void writeMetric(Writer writer, String metric, long value) throws IOException {
    writer.write(metric);
    writer.write(' ');
    writer.write(valueOf(value));
    writer.write('\n');
  }

  private static long len(long[] arr) {
    return arr == null ? 0 : arr.length;
  }
}
