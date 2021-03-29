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

package baselib.jdbc;

import baselib.Env;
import static baselib.ExceptionWrapper.ex;
import baselib.Records;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

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
   *
   * @return a default jdbc instance: this will read jdbc url, user and password
   *         from environment variables: JDBC_URL, JDBC_USER, JDBC_PASSWORD
   */
  public static JdbcInstance defaultClient() {
    var env = new Env();
    return new JdbcInstance(() -> ex(() -> DriverManager.getConnection(
      env.get(() -> "JDBC_URL"),
      env.get(() -> "JDBC_USER"),
      env.get(() -> "JDBC_PASSWORD")
    )));
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
   * Executes a sql statement with no result.
   * For example, update statements.
   *
   * Returns the number of modified rows, so that it can be used for
   * optimistic locking statements and other statements requiring such
   * information back.
   *
   * @param sql the sql statement to execute: supports positional parameters '?'
   * @param paramSetter functional access to set statement parameters.
   * @return the number of records being modified
   */
  public int execute(
    final String sql,
    final ExConsumer<PreparedStatement> paramSetter) {

    Objects.requireNonNull(sql);
    Objects.requireNonNull(paramSetter);

    return ex(() -> {
      try (var connection = connectionSupplier.get()) {
        try (var st = connection.prepareStatement(sql,
                        ResultSet.TYPE_SCROLL_SENSITIVE,
                        ResultSet.CONCUR_READ_ONLY)) {
          paramSetter.accept(st);
          return st.executeUpdate();
        }
      }
    });
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

  /**
   * Handy function to make a lambda selector for a select result of java
   * records.
   * @param <T> java record type
   * @param clazz class of the java record type. must match, must be a java
   *              record class
   * @param sql statement to execute, supports positional parameters.
   * @param paramSetter setter for positional parameters on statement object.
   * @return a lambda that once executed will return a list of the records.
   */
  public <T> Supplier<List<T>> makeRecordSelector(
      Class<T> clazz,
      String sql,
      final ExConsumer<PreparedStatement> paramSetter) {
    var mapper = mapperOfRecord(clazz);
    return () -> {
      var list = new LinkedList<T>();
      streamed(sql, paramSetter, rs -> list.add(mapper.map(rs)));
      return list;
    };
  }

  public static <T> RecordMapper<T> mapperOfRecord(Class<T> clazz) {
    return new RecordMapperImpl<>(clazz);
  }

  @FunctionalInterface
  public interface ExConsumer<T> {
    void accept(T t) throws Exception; //NOSONAR
  }

  @FunctionalInterface
  public interface RecordMapper<T> {
    T map(ResultSet rs);
  }

  private static class RecordMapperImpl<T> implements RecordMapper<T> {
    private final Class<T> clazz;

    public RecordMapperImpl(Class<T> clazz) {
      if (!clazz.isRecord())
        throw new IllegalArgumentException("works only with record classes");
      this.clazz = clazz;
    }

    @Override
    public T map(ResultSet rs) {
      return Records.fromPropertyDiscover(clazz, name -> ex(() -> rs.getObject(name)));
    }
  }
}
