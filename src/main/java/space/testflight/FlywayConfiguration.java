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

import static java.nio.charset.StandardCharsets.UTF_8;
import static space.testflight.DatabaseInstanceScope.PER_TEST_METHOD;
import static space.testflight.DatabaseType.POSTGRESQL;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.database.postgresql.PostgreSQLParser;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;
import org.flywaydb.core.internal.parser.Parser;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.core.internal.resource.classpath.ClassPathResource;
import org.flywaydb.core.internal.resource.filesystem.FileSystemResource;
import org.flywaydb.core.internal.scanner.LocationScannerCache;
import org.flywaydb.core.internal.scanner.ResourceNameCache;
import org.flywaydb.core.internal.scanner.classpath.ClassPathScanner;
import org.flywaydb.core.internal.scanner.filesystem.FileSystemScanner;
import org.flywaydb.core.internal.sqlscript.SqlStatementIterator;
import org.flywaydb.database.mysql.MySQLParser;
import org.testcontainers.containers.JdbcDatabaseContainer;

public class FlywayConfiguration extends TestflightConfiguration {

  private List<LoadableResource> testDataScriptResources;

  public FlywayConfiguration(Optional<space.testflight.Flyway> flywayConfiguration) {
    super(
      flywayConfiguration.map(Flyway::database).orElse(POSTGRESQL),
      flywayConfiguration.map(Flyway::dockerImage).orElse(""),
      flywayConfiguration.map(Flyway::databaseInstance).orElse(PER_TEST_METHOD),
      flywayConfiguration.map(Flyway::testDataScripts).orElse(EMPTY_STRING_ARRAY),
      flywayConfiguration.map(Flyway::configuration).orElse(EMPTY_PROPERTIES_ARRAY));
    initializeTestDataScriptResources();
  }

  public int getTestDataTagSuffix() {
    StringBuilder stringBuilder = new StringBuilder();
    for (LoadableResource loadableTestDataResource : testDataScriptResources) {
      stringBuilder.append(loadableTestDataResource.getFilename());
    }

    return stringBuilder.toString().hashCode();
  }

  public Optional<String> getCurrentMigrationTarget() throws URISyntaxException, IOException {
    Configuration configuration = org.flywaydb.core.Flyway.configure()
      .configuration(getProperties())
      .load()
      .getConfiguration();
    Location[] locations = configuration.getLocations();
    List<LoadableResource> migrations = new ArrayList<>();

    FileSystemScanner fileSystemScanner = new FileSystemScanner(configuration.getEncoding(), false, false, false);
    for (Location location : locations) {
      if (location.isClassPath()) {
        ClassPathScanner<JavaMigration> scanner = new ClassPathScanner<JavaMigration>(
          JavaMigration.class,
          configuration.getClassLoader(),
          configuration.getEncoding(),
          location,
          new ResourceNameCache(),
          new LocationScannerCache(),
          false);
        migrations.addAll(scanner.scanForResources());
      } else if (location.isFileSystem()) {
        migrations.addAll(fileSystemScanner.scanForResources(location));
      } else {
        throw new IllegalStateException("Unsupported location " + location);
      }
    }

    Optional<LoadableResource> latestMigration = migrations.stream().sorted(Comparator.reverseOrder()).findFirst();
    return latestMigration.map(p -> p.getFilename().toString().split("__")[0].replace(".", "_"));
  }

  public void applyToDatabase(
    JdbcDatabaseContainer<?> container) throws SQLException {
    FluentConfiguration flywayConfiguration = org.flywaydb.core.Flyway.configure()
      .configuration(getProperties())
      .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword());
    flywayConfiguration.load().migrate();

    ParsingContext parsingContext = new ParsingContext();
    JdbcTemplate jdbcTemplate = new JdbcTemplate(flywayConfiguration.getDataSource().getConnection());
    Parser parser;
    switch (getDatabaseType()) {
      case POSTGRESQL:
        parser = new PostgreSQLParser(flywayConfiguration, parsingContext);
        break;
      case MYSQL:
        parser = new MySqlParserFactory().createParser(flywayConfiguration, parsingContext);
        break;
      default:
        throw new IllegalStateException("Unsupported database type " + getDatabaseType());
    }

    for (LoadableResource testDataScript : testDataScriptResources) {
      SqlStatementIterator parse = parser.parse(testDataScript);
      parse.forEachRemaining(p -> p.execute(jdbcTemplate));
    }
  }

  private void initializeTestDataScriptResources() {
    testDataScriptResources = new ArrayList<>();
    for (String testDataScript : getTestDataScripts()) {
      Location scriptLocation = new Location(testDataScript);
      if (scriptLocation.isClassPath()) {
        LoadableResource loadableResource = new ClassPathResource(null, testDataScript, this.getClass().getClassLoader(), UTF_8);
        testDataScriptResources.add(loadableResource);
      } else if (scriptLocation.isFileSystem()) {
        LoadableResource loadableResource = new FileSystemResource(null, scriptLocation.getPath(), UTF_8, false);
        testDataScriptResources.add(loadableResource);
      } else {
        throw new IllegalStateException("Unsupported test data location " + scriptLocation);
      }
    }
  }

  private static class MySqlParserFactory {
    public Parser createParser(Configuration configuration, ParsingContext context) {
      return new MySQLParser(configuration, context);
    }
  }
}
