/*
 * Copyright 2020 Arne Limburg
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
package de.openknowledge.extensions.flyway;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class FlywayExtension implements BeforeAllCallback, BeforeEachCallback {

  private static final String JDBC_URL = "jdbc.url";
  private static final String JDBC_USERNAME = "jdbc.username";
  private static final String JDBC_PASSWORD = "jdbc.password";
  private static final String POSTGRES_CONTAINER_DIRECTORY = "/var/lib/postgresql/data";
  private static final String POSTGRES_HOST_DIRECTORY = "target/postgres";
  private static final String POSTGRES_BACKUP_DIRECTORY = "target/postgres-base";

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    PostgreSQLContainer<?> container = new PostgreSQLContainer();
    container.addFileSystemBind(POSTGRES_HOST_DIRECTORY, POSTGRES_CONTAINER_DIRECTORY, BindMode.READ_WRITE);
    container.start();
    System.setProperty(JDBC_URL, container.getJdbcUrl());
    System.setProperty(JDBC_USERNAME, container.getUsername());
    System.setProperty(JDBC_PASSWORD, container.getPassword());

    org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
      .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword()).load();
    flyway.migrate();
    container.stop();

    FileUtils.copyDirectory(new File(POSTGRES_HOST_DIRECTORY), new File(POSTGRES_BACKUP_DIRECTORY));
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    FileUtils.copyDirectory(new File(POSTGRES_BACKUP_DIRECTORY), new File(POSTGRES_HOST_DIRECTORY));
    PostgreSQLContainer<?> container = new PostgreSQLContainer();
    container.setWaitStrategy(Wait.forLogMessage(".*is ready.*", 1));
    container.addFileSystemBind(POSTGRES_HOST_DIRECTORY, POSTGRES_CONTAINER_DIRECTORY, BindMode.READ_WRITE);
    container.start();
    System.setProperty(JDBC_URL, container.getJdbcUrl());
    System.setProperty(JDBC_USERNAME, container.getUsername());
    System.setProperty(JDBC_PASSWORD, container.getPassword());
  }
}
