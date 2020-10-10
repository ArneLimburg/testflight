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
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.PostgreSQLContainer;

public class FlywayExtension implements BeforeEachCallback {

  private static final String POSTGRES_HOST_DIRECTORY = "/target/postgres";

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    PostgreSQLContainer<?> container = new PostgreSQLContainer();
    container.addFileSystemBind(POSTGRES_HOST_DIRECTORY, "/var/lib/postgresql/data", BindMode.READ_WRITE);
    container.start();
    FileUtils.copyDirectory(new File(POSTGRES_HOST_DIRECTORY), new File("/target/postgres-base"));
    System.setProperty("jdbc.url", container.getJdbcUrl());
    System.setProperty("jdbc.username", container.getUsername());
    System.setProperty("jdbc.password", container.getPassword());

    org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
           .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
           .load();
    flyway.migrate();
  }
}
