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

import java.sql.Connection;
import java.util.Objects;
import java.util.function.Supplier;
import static baselib.ExceptionWrapper.ex;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
class JdbcInstance {
  private final Supplier<Connection> connectionSupplier;

  public JdbcInstance(Supplier<Connection> connectionSupplier) {
    this.connectionSupplier = Objects.requireNonNull(connectionSupplier);
  }

  public boolean healthCheck() {
    return ex(() -> connectionSupplier.get().isValid(0));
  }

  public void streamed(
      String sql,
      ExConsumer<PreparedStatement> paramSetter,
      ExConsumer<ResultSet> resultGetter) {

    Objects.requireNonNull(sql);
    Objects.requireNonNull(paramSetter);
    Objects.requireNonNull(resultGetter);

    ex(() -> {
      try (var connection = connectionSupplier.get()) {
        try (var st = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
          paramSetter.accept(st);
          try (var rs = st.executeQuery()) {
            while (rs.next())
              resultGetter.accept(rs);
          }
        }
      }
    });
  }

  @FunctionalInterface
  public static interface ExConsumer<T> {
    void accept(T t) throws Exception;//NOSONAR
  }
}
