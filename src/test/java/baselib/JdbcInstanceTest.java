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

import static baselib.TestHelper.sql;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.UUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
  JdbcInstance instance;

  @BeforeEach
  void setup() {
    var dbname = UUID.randomUUID().toString();
    var connection = TestHelper.memoryDB(dbname);
    createTables(connection);
    insertData(connection);
    instance = new JdbcInstance(() -> connection);
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
