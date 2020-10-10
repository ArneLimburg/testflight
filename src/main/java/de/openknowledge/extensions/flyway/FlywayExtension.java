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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.database.postgresql.PostgreSQLParser;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.core.internal.resource.LoadableResource;
import org.flywaydb.core.internal.resource.classpath.ClassPathResource;
import org.flywaydb.core.internal.sqlscript.SqlStatementIterator;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import de.openknowledge.extensions.flyway.Flyway.DatabaseType;

public class FlywayExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

  private static final String POSTGRESQL_STARTUP_LOG_MESSAGE = ".*database system is ready to accept connections.*\\s";
  private static final String JDBC_URL = "jdbc.url";
  private static final String JDBC_USERNAME = "jdbc.username";
  private static final String JDBC_PASSWORD = "jdbc.password";
  private static final String STORE_IMAGE = "image";
  private static final String STORE_CONTAINER = "container";

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    String currentMigrationTarget = getCurrentMigrationTarget();
    JdbcDatabaseContainer<?> container = createContainer(context, StartupType.SLOW);
    TaggableContainer taggableContainer = (TaggableContainer)container;
    if (!taggableContainer.containsTag(currentMigrationTarget)) {
      container.start();
      System.setProperty(JDBC_URL, container.getJdbcUrl());
      System.setProperty(JDBC_USERNAME, container.getUsername());
      System.setProperty(JDBC_PASSWORD, container.getPassword());

      org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
        .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword()).load();
      flyway.migrate();
      taggableContainer.tag(currentMigrationTarget);

      Optional<Flyway> extension = context.getTestClass().map(type -> type.getAnnotation(Flyway.class)).filter(fly -> fly != null);
      if (extension.isPresent()) {
        String[] testDataScripts = extension.get().testDataScripts();

        Configuration flywayConfiguration = flyway.getConfiguration();
        ParsingContext parsingContext = new ParsingContext();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(flywayConfiguration.getDataSource().getConnection());
        PostgreSQLParser postgreSQLParser = new PostgreSQLParser(flywayConfiguration, parsingContext);

        for (String testDataScript : testDataScripts) {
          LoadableResource loadableResource = new ClassPathResource(null, testDataScript, this.getClass().getClassLoader(), UTF_8);
          SqlStatementIterator parse = postgreSQLParser.parse(loadableResource);
          parse.forEachRemaining(p -> p.execute(jdbcTemplate));
        }
      }

      getExtensionStore(context).put(STORE_IMAGE, taggableContainer.getImageName(currentMigrationTarget));
      container.stop();
    } else {
      getExtensionStore(context).put(STORE_IMAGE, taggableContainer.getImageName(currentMigrationTarget));
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    JdbcDatabaseContainer<?> container = createContainer(context, StartupType.FAST);
    container.start();
    System.setProperty(JDBC_URL, container.getJdbcUrl());
    System.setProperty(JDBC_USERNAME, container.getUsername());
    System.setProperty(JDBC_PASSWORD, container.getPassword());

    getMethodStore(context).put(STORE_CONTAINER, container);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    PostgreSQLContainer<?> container = (PostgreSQLContainer<?>)getMethodStore(context).get(STORE_CONTAINER);
    container.stop();
  }

  private String getCurrentMigrationTarget() throws URISyntaxException, IOException {
    org.flywaydb.core.Flyway dryway = org.flywaydb.core.Flyway.configure().load();
    Location[] locations = dryway.getConfiguration().getLocations();
    List<Path> migrations = new ArrayList<>();
    for (Location location : locations) {
      URL resource = getClass().getClassLoader().getResource(location.getPath());
      File file = new File(resource.toURI());

      try (Stream<Path> paths = Files.walk(file.toPath())) {
        migrations.addAll(paths.filter(Files::isRegularFile).collect(Collectors.toList()));
      }
    }

    Optional<Path> latestMigration = migrations.stream().sorted(Comparator.reverseOrder()).findFirst();
    String latestMigrationFileName = latestMigration.get().getFileName().toString().split("__")[0].replace(".", "_");

    return latestMigrationFileName;
  }

  private ExtensionContext.Store getExtensionStore(ExtensionContext context) {
    return context.getStore(Namespace.create(FlywayExtension.class));
  }

  private ExtensionContext.Store getMethodStore(ExtensionContext context) {
    return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
  }

  private JdbcDatabaseContainer<?> createContainer(ExtensionContext context, StartupType startup) {
    JdbcDatabaseContainer<?> container;
    Optional<Flyway> configuration = context.getTestClass().map(type -> type.getAnnotation(Flyway.class)).filter(flyway -> flyway != null);
    if (!configuration.isPresent()) {
      container = new InContainerDataPostgreSqlContainer();
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
          Optional<String> imageName = ofNullable((String)getExtensionStore(context).get(STORE_IMAGE));
          imageName = ofNullable(imageName.orElse(flywayConfiguration.dockerImage())).filter(image -> !image.isEmpty());
          if (imageName.isPresent()) {
            container = new InContainerDataPostgreSqlContainer(imageName.get());
          } else {
            container = new InContainerDataPostgreSqlContainer();
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
