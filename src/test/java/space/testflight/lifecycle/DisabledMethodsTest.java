/*
 * Copyright 2021 Marvin Kienitz
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
package space.testflight.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineExecutionResults;

import space.testflight.AbstractJupiterTestEngineTests;
import space.testflight.ConfigProperty;
import space.testflight.DatabaseInstanceScope;
import space.testflight.DatabaseType;
import space.testflight.Flyway;

class DisabledMethodsTest extends AbstractJupiterTestEngineTests {

  @Test
  void executeTestsWithDisabledTestClass() {
    assertThatNoException().isThrownBy(() -> {
      EngineExecutionResults results = executeTestsForClasses(
        TestClassWithOnlyDisabledTestMethodsTestCase.class,
        TestClassWithNonDisabledTestMethodsTestCase.class
      );

      assertThat(results.testEvents().started().count()).isEqualTo(1);
      assertThat(results.testEvents().skipped().count()).isEqualTo(1);
      assertThat(results.testEvents().succeeded().count()).isEqualTo(1);
      assertThat(results.testEvents().failed().count()).isZero();
    });
  }

  @Flyway(
    database = DatabaseType.POSTGRESQL,
    databaseInstance = DatabaseInstanceScope.PER_TEST_METHOD,
    configuration = {
      @ConfigProperty(key = "space.testflight.jdbc.url.property", value = "javax.persistence.jdbc.url"),
      @ConfigProperty(key = "space.testflight.jdbc.username.property", value = "javax.persistence.jdbc.user"),
      @ConfigProperty(key = "space.testflight.jdbc.password.property", value = "javax.persistence.jdbc.password")
    }
  )
  static class TestClassWithOnlyDisabledTestMethodsTestCase {
    @Test
    @Disabled("test, if testflight works with only disabled test cases")
    void testMethodDisabled() {
      assertTrue(false);
    }
  }

  @Flyway(
    database = DatabaseType.POSTGRESQL,
    databaseInstance = DatabaseInstanceScope.PER_TEST_METHOD,
    configuration = {
      @ConfigProperty(key = "space.testflight.jdbc.url.property", value = "javax.persistence.jdbc.url"),
      @ConfigProperty(key = "space.testflight.jdbc.username.property", value = "javax.persistence.jdbc.user"),
      @ConfigProperty(key = "space.testflight.jdbc.password.property", value = "javax.persistence.jdbc.password")
    }
  )
  static class TestClassWithNonDisabledTestMethodsTestCase {
    @Test
    void testMethodNonDisabled() {
      assertTrue(true);
    }
  }
}
