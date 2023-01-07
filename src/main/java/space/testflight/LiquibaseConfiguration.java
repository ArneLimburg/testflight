/*
 * Copyright 2021 - 2023 Arne Limburg
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

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static space.testflight.DatabaseInstanceScope.PER_TEST_METHOD;
import static space.testflight.DatabaseType.POSTGRESQL;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.testcontainers.containers.JdbcDatabaseContainer;

import liquibase.Scope;
import liquibase.change.core.SQLFileChange;
import liquibase.configuration.AbstractConfigurationValueProvider;
import liquibase.configuration.ProvidedValue;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

public class LiquibaseConfiguration extends TestflightConfiguration {

  private String changeLogFile;
  private List<SQLFileChange> sqlFileChanges;

  public LiquibaseConfiguration(Optional<space.testflight.Liquibase> liquibaseConfiguration) {
    super(
      liquibaseConfiguration.map(Liquibase::database).orElse(POSTGRESQL),
      liquibaseConfiguration.map(Liquibase::dockerImage).orElse(""),
      liquibaseConfiguration.map(Liquibase::databaseInstance).orElse(PER_TEST_METHOD),
      liquibaseConfiguration.map(Liquibase::testDataScripts).orElse(EMPTY_STRING_ARRAY),
      liquibaseConfiguration.map(Liquibase::configuration).orElse(EMPTY_PROPERTIES_ARRAY));
    changeLogFile = getChangelogFile();
    initializeTestDataScriptResources();
    Scope.getCurrentScope().getSingleton(liquibase.configuration.LiquibaseConfiguration.class)
      .registerProvider(new TestConfigurationValueProvider(getProperties()));
  }

  private String getChangelogFile() {
    return ofNullable(getProperties().get("changelogFile"))
        .orElseGet(() -> ofNullable(getProperties().get("liquibase.changelogFile"))
            .orElseGet(() -> ofNullable(getProperties().get("spring.liquibase.changelog"))
                .orElse("changelog.xml")));
  }

  public int getTestDataTagSuffix() {
    StringBuilder stringBuilder = new StringBuilder();
    for (SQLFileChange sqlFileChange : sqlFileChanges) {
      stringBuilder.append(sqlFileChange.getPath());
    }

    return stringBuilder.toString().hashCode();
  }

  public Optional<String> getCurrentMigrationTarget() throws URISyntaxException, IOException {
    ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader());
    try (liquibase.Liquibase liquibase = new liquibase.Liquibase(changeLogFile, resourceAccessor, (Database)null)) {
      return Optional.ofNullable(liquibase.getDatabaseChangeLog().getChangeLogId());
    } catch (LiquibaseException e) {
      throw new IllegalStateException(e);
    }
  }

  public void applyToDatabase(
    JdbcDatabaseContainer<?> container) throws SQLException {
    try {
      try (JdbcConnection connection = new JdbcConnection()) {
        Properties jdbcProperties = new Properties();
        jdbcProperties.put("user", container.getUsername());
        jdbcProperties.put("password", container.getPassword());
        connection.open(container.getJdbcUrl(), container.getJdbcDriverInstance(), jdbcProperties);
        ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader());
        try (liquibase.Liquibase liquibase = new liquibase.Liquibase(changeLogFile, resourceAccessor, connection)) {
          liquibase.update();
          for (SQLFileChange change : sqlFileChanges) {
            liquibase.getDatabase().executeStatements(change, liquibase.getDatabaseChangeLog(), emptyList());
          }
        } catch (LiquibaseException e) {
          throw new SQLException(e);
        }
      }
    } catch (DatabaseException e) {
      throw new SQLException(e);
    }
  }

  private void initializeTestDataScriptResources() {
    sqlFileChanges = new ArrayList<>();
    for (String sqlFile : getTestDataScripts()) {
      SQLFileChange change = new SQLFileChange();
      change.setPath(sqlFile);
      sqlFileChanges.add(change);
    }
  }

  public static class TestConfigurationValueProvider extends AbstractConfigurationValueProvider {

    private static final int TEST_PRIORITY = 500;
    private static final String SOURCE_DESCRIPTION = "@Liquibase";

    private Map<String, String> properties;

    public TestConfigurationValueProvider(Map<String, String> properties) {
      this.properties = properties;
    }

    @Override
    public int getPrecedence() {
      return TEST_PRIORITY;
    }

    @Override
    public ProvidedValue getProvidedValue(String... keyAndAliases) {
      for (String key: keyAndAliases) {
        String value = properties.get(key);
        if (value != null) {
          return new ProvidedValue(keyAndAliases[0], key, value, SOURCE_DESCRIPTION, this);
        }
      }
      return null;
    }
  }
}
