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
package space.testflight.injection;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;

import space.testflight.ConfigProperty;
import space.testflight.DatabaseInstanceScope;
import space.testflight.DatabaseType;
import space.testflight.Flyway;
import space.testflight.TestResource;

@Flyway(
  database = DatabaseType.POSTGRESQL,
  databaseInstance = DatabaseInstanceScope.PER_TEST_METHOD,
  configuration = {
    @ConfigProperty(key = "space.testflight.jdbc.url.property", value = "javax.persistence.jdbc.url"),
    @ConfigProperty(key = "space.testflight.jdbc.username.property", value = "javax.persistence.jdbc.user"),
    @ConfigProperty(key = "space.testflight.jdbc.password.property", value = "javax.persistence.jdbc.password")
  }
)
public class UrlInjectionTest {

  @TestResource
  private String jdbcUrl;
  @TestResource
  private String jdbcUser;
  @TestResource
  private String jdbcPassword;

  @Test
  void injectConnectionString(@TestResource URI uri) {
    assertThat(jdbcUrl).isEqualTo(uri.toString());
    assertThat(jdbcUser).isEqualTo(System.getProperty("javax.persistence.jdbc.user"));
    assertThat(jdbcPassword).isEqualTo(System.getProperty("javax.persistence.jdbc.password"));
  }
}
