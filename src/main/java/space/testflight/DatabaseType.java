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

import static org.testcontainers.containers.MySQLContainer.NAME;
import static org.testcontainers.containers.PostgreSQLContainer.IMAGE;

public enum DatabaseType {
  POSTGRESQL(IMAGE, ".*database system is ready to accept connections.*\\s"),
  MYSQL(NAME, "mysqld: ready for connections");

  private String image;
  private String startupLogMessage;

  DatabaseType(String databaseImage, String startupLogMessage) {
    this.image = databaseImage;
    this.startupLogMessage = startupLogMessage;
  }

  public String getDefaultImage() {
    return image;
  }

  String getStartupLogMessage() {
    return startupLogMessage;
  }
}
