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
 * An object that wraps around a jdbc 'instance'.
 * Here for instance is not intended a connection, but more generally a
 * data source.
 * The lambda passed to the constructor will request a new connection every time
 * a method is called on this class, so that pools can taken advantage of.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public final class JdbcInstance {
  private final Supplier<Connection> connectionSupplier;

  /**
   *
   * @param supplier the function that will supply the connection.
   *                 This function will be called on each of this
   *                 class method.
   */
  public JdbcInstance(final Supplier<Connection> supplier) {
    this.connectionSupplier = Objects.requireNonNull(supplier);
  }

  /**
   * Makes a validation check of the connection.
   * This relies not on specific sql statements but on the isValid() method of
   * the Connection object so it is cross database safe.
   * @return true if the connection validate correctly, either false, or
   *              possibly throws some exception.
   */
  public boolean healthCheck() {
    return ex(() -> connectionSupplier.get().isValid(0));
  }

  /**
   * Executes the query using concur TYPE_SCROLL_SENSITIVE + CONCUR_READ_ONLY
   * (meanint it uses a cursor) and streams the result without either the db nor
   * the client caching the whole list in memory.
   *
   * @param sql the statement to run. No string replacement is used, but only
   *            positional parameters are supported, the ones with '?'.
   * @param paramSetter the function that access to the prepared statement so
   *                    that positional parameters can be set
   * @param resultGetter a function that gets called per each record.
   *                     a result set is passed as parameter.
   *                     ResultSet.next() is already called automatically so do
   *                     not call next() or you will be ending up skipping
   *                     records.
   */
  public void streamed(
      final String sql,
      final ExConsumer<PreparedStatement> paramSetter,
      final ExConsumer<ResultSet> resultGetter) {

    Objects.requireNonNull(sql);
    Objects.requireNonNull(paramSetter);
    Objects.requireNonNull(resultGetter);

    ex(() -> {
      try (var connection = connectionSupplier.get()) {
        try (var st = connection.prepareStatement(sql,
                        ResultSet.TYPE_SCROLL_SENSITIVE,
                        ResultSet.CONCUR_READ_ONLY)) {
          paramSetter.accept(st);
          try (var rs = st.executeQuery()) {
            while (rs.next()) {
              resultGetter.accept(rs);
            }
          }
        }
      }
    });
  }

  @FunctionalInterface
  interface ExConsumer<T> {
    void accept(T t) throws Exception; //NOSONAR
  }
}
