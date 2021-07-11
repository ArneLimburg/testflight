/*
 * Copyright 2021 Roman Ness
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
package space.testflight.dbrider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.database.rider.core.api.connection.ConnectionHolder;

import space.testflight.ConfigProperty;
import space.testflight.DatabaseInstanceScope;
import space.testflight.DatabaseType;
import space.testflight.Flyway;

@Flyway(
  database = DatabaseType.POSTGRESQL,
  databaseInstance = DatabaseInstanceScope.PER_TEST_METHOD,
  configuration = {
  @ConfigProperty(key = "space.testflight.jdbc.url.property", value = "javax.persistence.jdbc.url"),
  @ConfigProperty(key = "space.testflight.jdbc.username.property", value = "javax.persistence.jdbc.user"),
  @ConfigProperty(key = "space.testflight.jdbc.password.property", value = "javax.persistence.jdbc.password")
})
public class ConnectionHolderFieldWithoutAnnotationTest {

  private ConnectionHolder connectionHolder;  // field is missing TestResource and is ignored

  @Test
  void connectionHolderFieldIsIgnoredBecauseOfWrongType() {
    assertThat(connectionHolder).isNull();
  }
}
