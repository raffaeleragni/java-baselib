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
package baselib.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 *
 * @author Raffaele Ragni
 */
class JVMThreadMetrics implements MetricRegisterable {

  private final ThreadMXBean threads = ManagementFactory.getThreadMXBean();

  @Override
  public void register(BiConsumer<String, Supplier<Object>> registerFunction) {
    registerFunction.accept("jvm_threads_current", () -> threads.getThreadCount());
    registerFunction.accept("jvm_threads_daemon", () -> threads.getDaemonThreadCount());
    registerFunction.accept("jvm_threads_peak", () -> threads.getPeakThreadCount());
    registerFunction.accept("jvm_threads_started_total", () -> threads.getTotalStartedThreadCount());
    registerFunction.accept("jvm_threads_deadlocked", () -> len(threads.findDeadlockedThreads()));
    registerFunction.accept("jvm_threads_deadlocked_monitor", () -> len(threads.findMonitorDeadlockedThreads()));
  }

  private static long len(long[] arr) {
    return arr == null ? 0 : arr.length;
  }
}
