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

import de.openknowledge.extensions.flyway.Flyway.DatabaseType;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.util.Optional;

public class FlywayExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

  private static final String POSTGRESQL_STARTUP_LOG_MESSAGE = ".*database system is ready to accept connections.*\\s";
  private static final String JDBC_URL = "jdbc.url";
  private static final String JDBC_USERNAME = "jdbc.username";
  private static final String JDBC_PASSWORD = "jdbc.password";
  private static final String POSTGRES_CONTAINER_DIRECTORY = "/var/lib/postgresql/data";
  private static final String POSTGRES_HOST_DIRECTORY = "target/postgres";
  private static final String POSTGRES_BACKUP_DIRECTORY = "target/postgres-base";
  private static final String STORE_CONTAINER = "container";

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    JdbcDatabaseContainer<?> container = createContainer(context, StartupType.SLOW);
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
    JdbcDatabaseContainer<?> container = createContainer(context, StartupType.FAST);
    container.addFileSystemBind(POSTGRES_HOST_DIRECTORY, POSTGRES_CONTAINER_DIRECTORY, BindMode.READ_WRITE);
    container.start();
    System.setProperty(JDBC_URL, container.getJdbcUrl());
    System.setProperty(JDBC_USERNAME, container.getUsername());
    System.setProperty(JDBC_PASSWORD, container.getPassword());

    getStore(context).put(STORE_CONTAINER, container);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    PostgreSQLContainer<?> container = (PostgreSQLContainer<?>) getStore(context).get(STORE_CONTAINER);
    container.stop();
  }

  private ExtensionContext.Store getStore(ExtensionContext context) {
    return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
  }

  private JdbcDatabaseContainer<?> createContainer(ExtensionContext context, StartupType startup) {
    JdbcDatabaseContainer<?> container;
    Optional<Flyway> configuration = context.getTestClass().map(type -> type.getAnnotation(Flyway.class)).filter(flyway -> flyway != null);
    if (!configuration.isPresent()) {
      container = new PostgreSQLContainer();
      if (startup == StartupType.FAST) {
        container.setWaitStrategy(Wait.forLogMessage(POSTGRESQL_STARTUP_LOG_MESSAGE, 1));
      }
    } else {
      Flyway flywayConfiguration = configuration.get();
      switch (flywayConfiguration.database()) {
        case POSTGRESQL:
          if (flywayConfiguration.database() != DatabaseType.POSTGRESQL) {
            throw new IllegalStateException("Currently only PostgreSQL is supported");
          }
          if (flywayConfiguration.dockerImage().length() > 0) {
            container = new PostgreSQLContainer(flywayConfiguration.dockerImage());
          } else {
            container = new PostgreSQLContainer();
          }
          if (startup == StartupType.FAST) {
            container.setWaitStrategy(Wait.forLogMessage(POSTGRESQL_STARTUP_LOG_MESSAGE, 1));
          }
          break;
        default:
          throw new IllegalStateException("Database type " + flywayConfiguration.database() + " is not supported");
      }
    }
    return container;
  }

  private enum StartupType {
    SLOW, FAST;
  }
}
