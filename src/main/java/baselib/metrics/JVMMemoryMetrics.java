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

import static java.lang.String.valueOf;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 *
 * @author Raffaele Ragni
 */
class JVMMemoryMetrics implements MetricRegisterable {

  private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();

  @Override
  public void register(BiConsumer<String, Supplier<String>> registerFunction) {
    registerFunction.accept("jvm_memory_bytes_used{area=\"heap\"}", () -> valueOf(memory.getHeapMemoryUsage().getUsed()));
    registerFunction.accept("jvm_memory_bytes_used{area=\"nonheap\"}", () -> valueOf(memory.getNonHeapMemoryUsage().getUsed()));
    registerFunction.accept("jvm_memory_bytes_committed{area=\"heap\"}", () -> valueOf(memory.getHeapMemoryUsage().getCommitted()));
    registerFunction.accept("jvm_memory_bytes_committed{area=\"nonheap\"}", () -> valueOf(memory.getNonHeapMemoryUsage().getCommitted()));
    registerFunction.accept("jvm_memory_bytes_max{area=\"heap\"}", () -> valueOf(memory.getHeapMemoryUsage().getMax()));
    registerFunction.accept("jvm_memory_bytes_max{area=\"nonheap\"}", () -> valueOf(memory.getNonHeapMemoryUsage().getMax()));
    registerFunction.accept("jvm_memory_bytes_init{area=\"heap\"}", () -> valueOf(memory.getHeapMemoryUsage().getInit()));
    registerFunction.accept("jvm_memory_bytes_init{area=\"nonheap\"}", () -> valueOf(memory.getNonHeapMemoryUsage().getInit()));
  }
}
