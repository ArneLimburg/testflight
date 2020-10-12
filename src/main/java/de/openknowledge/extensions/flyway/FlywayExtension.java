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
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
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
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class FlywayExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

  private static final String POSTGRESQL_STARTUP_LOG_MESSAGE = ".*database system is ready to accept connections.*\\s";
  private static final String JDBC_URL = "jdbc.url";
  private static final String JDBC_USERNAME = "jdbc.username";
  private static final String JDBC_PASSWORD = "jdbc.password";
  private static final String JDBC_PORT = "jdbc.port";
  private static final String STORE_IMAGE = "image";
  private static final String STORE_CONTAINER = "container";

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    initialize(context);
  }

  private <C extends JdbcDatabaseContainer & TaggableContainer> void initialize(ExtensionContext context) throws Exception {
    String currentMigrationTarget = getCurrentMigrationTarget();
    List<LoadableResource> loadableTestDataResources = getTestDataScriptResources(context);
    int testDataTagSuffix = getTestDataTagSuffix(loadableTestDataResources);
    String tagName = currentMigrationTarget + testDataTagSuffix;

    C container = createContainer(context, StartupType.SLOW);
    Store store = getExtensionStore(context);
    if (!ofNullable(store.get(JDBC_URL)).isPresent() || !container.containsTag(tagName)) {
      container.start();

      prefillDatabase(container, loadableTestDataResources);
      container.tag(tagName);

      store.put(STORE_IMAGE, container.getImageName(tagName));
      store.put(JDBC_URL, container.getJdbcUrl());
      store.put(JDBC_USERNAME, container.getUsername());
      store.put(JDBC_PASSWORD, container.getPassword());
      store.put(JDBC_PORT, container.getMappedPort(container.getContainerPort()));
      store.put(STORE_CONTAINER, container);
    } else {
      store.put(STORE_IMAGE, container.getImageName(tagName));
    }
    System.setProperty(JDBC_URL, store.get(JDBC_URL, String.class));
    System.setProperty(JDBC_USERNAME, store.get(JDBC_USERNAME, String.class));
    System.setProperty(JDBC_PASSWORD, store.get(JDBC_PASSWORD, String.class));
  }

  private void prefillDatabase(JdbcDatabaseContainer<?> container, List<LoadableResource> loadableTestDataResources) throws SQLException {
    org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
      .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword()).load();
    flyway.migrate();

    Configuration flywayConfiguration = flyway.getConfiguration();
    ParsingContext parsingContext = new ParsingContext();
    JdbcTemplate jdbcTemplate = new JdbcTemplate(flywayConfiguration.getDataSource().getConnection());
    PostgreSQLParser postgreSQLParser = new PostgreSQLParser(flywayConfiguration, parsingContext);

    for (LoadableResource testDataScript : loadableTestDataResources) {
      SqlStatementIterator parse = postgreSQLParser.parse(testDataScript);
      parse.forEachRemaining(p -> p.execute(jdbcTemplate));
    }
  }

  private List<LoadableResource> getTestDataScriptResources(ExtensionContext context) {
    Optional<Flyway> extension = context.getTestClass().map(type -> type.getAnnotation(Flyway.class)).filter(fly -> fly != null);
    List<LoadableResource> loadableResources = new ArrayList<>();
    if (extension.isPresent()) {
      String[] testDataScripts = extension.get().testDataScripts();
      for (String testDataScript : testDataScripts) {
        LoadableResource loadableResource = new ClassPathResource(null, testDataScript, this.getClass().getClassLoader(), UTF_8);
        loadableResources.add(loadableResource);
      }
    }

    return loadableResources;
  }

  private int getTestDataTagSuffix(List<LoadableResource> loadableTestDataResources) {
    StringBuilder stringBuilder = new StringBuilder();
    for (LoadableResource loadableTestDataResource : loadableTestDataResources) {
      stringBuilder.append(loadableTestDataResource.getFilename());
    }

    return stringBuilder.toString().hashCode();
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    JdbcDatabaseContainer<?> container = getExtensionStore(context).get(STORE_CONTAINER, JdbcDatabaseContainer.class);
    if (!container.isRunning()) {
      container = createContainer(context, StartupType.FAST);
      container.start();
    }

    getMethodStore(context).put(STORE_CONTAINER, container);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    AutoCloseable container = getMethodStore(context).get(STORE_CONTAINER, AutoCloseable.class);
    container.close();
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

  private <C extends JdbcDatabaseContainer & TaggableContainer> C createContainer(ExtensionContext context, StartupType startup) {
    Optional<Flyway> configuration = findAnnotation(context.getTestClass(), Flyway.class);
    C container;
    if (!configuration.isPresent()) {
      container = (C)createPostgreSqlContainer(context, startup);
    } else {
      Flyway flywayConfiguration = configuration.get();
      switch (flywayConfiguration.database()) {
        case POSTGRESQL:
          container = (C)createPostgreSqlContainer(context, startup);
          break;
        default:
          throw new IllegalStateException("Database type " + flywayConfiguration.database() + " is not supported");
      }
    }
    Optional.ofNullable(getExtensionStore(context).get(JDBC_PORT, Integer.class))
      .ifPresent(hostPort -> container.addFixedPort(hostPort, container.getContainerPort()));
    return container;
  }

  private InContainerDataPostgreSqlContainer createPostgreSqlContainer(ExtensionContext context, StartupType startup) {
    Optional<Flyway> configuration = findAnnotation(context.getTestClass(), Flyway.class);
    Optional<String> imageName = ofNullable((String)getExtensionStore(context).get(STORE_IMAGE));
    imageName = of(imageName.orElse(configuration.map(Flyway::dockerImage).orElse(""))).filter(image -> !image.isEmpty());

    InContainerDataPostgreSqlContainer container = imageName
      .map(name -> new InContainerDataPostgreSqlContainer(name))
      .orElseGet(() -> new InContainerDataPostgreSqlContainer());
    if (startup == StartupType.FAST) {
      container.setWaitStrategy(Wait.forLogMessage(POSTGRESQL_STARTUP_LOG_MESSAGE, 1));
    }
    return container;
  }

  private enum StartupType {
    SLOW, FAST;
  }
}
