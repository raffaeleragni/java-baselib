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
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 *
 * @author Raffaele Ragni
 */
class JVMClassloaderMetrics implements MetricRegisterable {

  private final ClassLoadingMXBean classload = ManagementFactory.getClassLoadingMXBean();

  @Override
  public void register(BiConsumer<String, Supplier<String>> registerFunction) {
    registerFunction.accept("jvm_classes_loaded", () -> valueOf(classload.getLoadedClassCount()));
    registerFunction.accept("jvm_classes_loaded_total", () -> valueOf(classload.getTotalLoadedClassCount()));
    registerFunction.accept("jvm_classes_unloaded_total", () -> valueOf(classload.getUnloadedClassCount()));
  }
}
