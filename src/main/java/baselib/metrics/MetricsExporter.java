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

import static baselib.ExceptionWrapper.ex;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class MetricsExporter {
  public static final MetricsExporter DEFAULT = new MetricsExporter();

  private final Map<String, Supplier<String>> metricPrinters;
  private final BiConsumer<String, Supplier<String>> registerFunction;

  public MetricsExporter() {
    metricPrinters = new HashMap<>();
    registerFunction = (name, value) -> metricPrinters.put(name, value);
    registerJVMMetrics();
  }

  public void register(MetricRegisterable registerable) {
    registerable.register(registerFunction);
  }

  public void export(Writer writer) {
    metricPrinters.forEach((name, value) -> printMetric(writer, name, value.get()));
  }

  private void printMetric(Writer writer, String name, String value) {
    ex(() -> writeMetric(writer, name, value.toString()));
  }

  private void registerJVMMetrics() {
    new JVMMemoryMetrics().register(registerFunction);
    new JVMClassloaderMetrics().register(registerFunction);
    new JVMThreadMetrics().register(registerFunction);
  }

  private void writeMetric(Writer writer, String metric, String value) throws IOException {
    writer.write(metric);
    writer.write(' ');
    writer.write(value);
    writer.write('\n');
  }
}
