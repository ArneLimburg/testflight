/*
 * Copyright 2021 Arne Limburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package space.testflight;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionHandler implements InvocationHandler {

  private String jdbcUrl;
  private String jdbcUsername;
  private String jdbcPassword;
  private Connection connection;

  public ConnectionHandler(String url, String username, String password) {
    jdbcUrl = url;
    jdbcUsername = username;
    jdbcPassword = password;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    return method.invoke(getConnection(), args);
  }

  private Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed() || !connection.isValid(1)) {
      connection = DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }
    return connection;
  }
}
