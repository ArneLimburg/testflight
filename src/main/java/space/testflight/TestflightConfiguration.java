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

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.testcontainers.containers.JdbcDatabaseContainer;

public abstract class TestflightConfiguration {

  protected static final String[] EMPTY_STRING_ARRAY = new String[0];
  protected static final ConfigProperty[] EMPTY_PROPERTIES_ARRAY = new ConfigProperty[0];

  private final DatabaseType databaseType;
  private final String dockerImage;
  private final DatabaseInstanceScope databaseInstanceScope;
  private final List<String> testDataScripts;
  private final Map<String, String> properties;

  public TestflightConfiguration(
    DatabaseType databaseType,
    String dockerImage,
    DatabaseInstanceScope databaseInstanceScope,
    String[] testDataScripts,
    ConfigProperty[] properties) {

    this.databaseType = databaseType;
    this.dockerImage = dockerImage;
    this.databaseInstanceScope = databaseInstanceScope;
    this.testDataScripts = unmodifiableList(asList(testDataScripts));
    this.properties = stream(properties).collect(toUnmodifiableMap(ConfigProperty::key, ConfigProperty::value));
  }

  public DatabaseType getDatabaseType() {
    return databaseType;
  }

  public String getDockerImage() {
    return !dockerImage.isEmpty() ? dockerImage : getDatabaseType().getDefaultImage();
  }

  public String getDockerImage(String tagName) {
    return getDockerImage() + ":" + tagName;
  }

  public DatabaseInstanceScope getDatabaseInstanceScope() {
    return databaseInstanceScope;
  }

  public List<String> getTestDataScripts() {
    return testDataScripts;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public abstract int getTestDataTagSuffix();

  public abstract Optional<String> getCurrentMigrationTarget() throws URISyntaxException, IOException;

  public abstract void applyToDatabase(JdbcDatabaseContainer<?> container) throws SQLException;
}
