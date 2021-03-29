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

import baselib.TestHelper;
import static baselib.TestHelper.sql;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
class JdbcInstanceTest {
  public record Rec(int id, Instant timestamp, String value) {}
  public record Table(String name) {}

  JdbcInstance instance;

  @BeforeEach
  void setup() throws SQLException {
    var dbname = UUID.randomUUID().toString();
    try (var connection = TestHelper.memoryDB(dbname)) {
      createTables(connection);
      insertData(connection);
    }
    instance = new JdbcInstance(() -> TestHelper.memoryDB(dbname));
  }

  @Test
  void testConnectionOK() {
    assertThat(instance.healthCheck(), is(true));
  }

  @Test
  void testConnectionBad() throws SQLException {
    var connection = mock(Connection.class);
    when(connection.isValid(anyInt())).thenReturn(false);
    instance = new JdbcInstance(() -> connection);

    assertThat(instance.healthCheck(), is(false));
  }

  @Test
  void testDefaultClient() {
    System.setProperty("JDBC_URL", "jdbc:h2:mem:newdb;DB_CLOSE_DELAY=-1");
    assertThat(JdbcInstance.defaultClient().healthCheck(), is(true));
  }

  @Test
  void testStreamedSelect() {
    var set = new HashSet<String>();
    instance.streamed(
        "select name from test",
        st -> {},
        rs -> set.add(rs.getString("name")));

    assertThat(set.contains("test1"), is(true));
    assertThat(set.contains("test2"), is(true));
    assertThat(set.contains("test3"), is(true));
  }

  @Test
  void testExecute() {
    var rows = instance.execute("update test set name = 'test'", st -> {});
    var set = new HashSet<String>();
    instance.streamed(
        "select name from test",
        st -> {},
        rs -> set.add(rs.getString("name")));

    assertThat(rows, greaterThan(0));
    assertThat(set.size(), is(1));
    assertThat(set.contains("test"), is(true));
  }

  @Test
  void testRecordMapperForNoRecord() {
    assertThrows(IllegalArgumentException.class, () -> JdbcInstance.<Object>mapperOfRecord(Object.class));
  }

  @Test
  void testRecordMapper() throws SQLException {
    var mapper = JdbcInstance.<Rec>mapperOfRecord(Rec.class);
    var rs = mock(ResultSet.class);
    var now = Instant.now();
    var expected = new Rec(1, now, "test");

    when(rs.getObject("id")).thenReturn(1);
    when(rs.getObject("timestamp")).thenReturn(now);
    when(rs.getObject("value")).thenReturn("test");

    var rec = mapper.map(rs);

    assertThat(rec, is(expected));
  }

  @Test
  void testRecordMapperFromTable() {
    var mapper = JdbcInstance.<Table>mapperOfRecord(Table.class);
    var set = new HashSet<>();
    instance.streamed(
        "select * from test",
        st -> {},
        rs -> set.add(mapper.map(rs)));

    assertThat(set, hasItem(new Table("test1")));
  }

  @Test
  void testRecordSelector() {
    var mapper = JdbcInstance.<Table>mapperOfRecord(Table.class);
    var set = new HashSet<>();
    var selector = instance.makeRecordSelector(Table.class, "select * from test", st -> {});

    assertThat(selector.get(), hasItem(new Table("test1")));
  }

  private void createTables(Connection connection) {
    sql(connection,
    """
      create table if not exists test (
        uuid varchar(255),
        name varchar(255),
        primary key (uuid)
      );
    """);
  }

  private void insertData(Connection connection) {
    sql(connection,
    """
      insert into test(uuid, name) values('%s', 'test1');
      insert into test(uuid, name) values('%s', 'test2');
      insert into test(uuid, name) values('%s', 'test3');
    """.formatted(
      UUID.randomUUID().toString(),
      UUID.randomUUID().toString(),
      UUID.randomUUID().toString()));
  }
}

