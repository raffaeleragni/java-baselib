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
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.function.Consumer;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
final class TestHelper {
  private TestHelper() {}

  static boolean portOccupied(int port) {
    try (var sock = new Socket("127.0.0.1", port)) {
      return true;
    } catch (IOException | RuntimeException ex) {
      return false;
    }
  }

  static Connection memoryDB(String name) {
    return ex(() ->
      DriverManager.getConnection("jdbc:h2:mem:"+name+";DB_CLOSE_DELAY=-1", "sa", "")
    );
  }

  static int sql(Connection connection, String sql) {
    return ex(() -> {
      try (var st = connection.prepareStatement(sql)) {
        return st.executeUpdate();
      }
    });
  }

  static void sql(Connection connection, String sql, Consumer<ResultSet> consumer) {
    ex(() -> {
      try (var st = connection.prepareStatement(sql)) {
        try (var rs = st.executeQuery()) {
          while (rs.next())
            consumer.accept(rs);
        }
      }
    });
  }
}
