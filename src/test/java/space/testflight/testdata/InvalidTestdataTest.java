/*
 * Copyright 2023 Arne Limburg
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
package space.testflight.testdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.postgresql.util.PSQLException;

import space.testflight.AbstractJupiterTestEngineTests;
import space.testflight.Flyway;

class InvalidTestdataTest extends AbstractJupiterTestEngineTests {

  @Test
  void executeTestsWithInvalidTestdata() {
    assertThatNoException().isThrownBy(() -> {
      EngineExecutionResults results = executeTestsForClasses(
        TestWithInvalidTestdata.class
      );

      Optional<TestExecutionResult> testExecutionResult = results.containerEvents().finished()
        .filter(event -> event.getPayload().isPresent())
        .map(event -> event.getPayload().get())
        .filter(payload -> (payload instanceof TestExecutionResult))
        .map(TestExecutionResult.class::cast)
        .filter(result -> result.getStatus() == FAILED)
        .findAny();
      assertThat(testExecutionResult)
        .map(TestExecutionResult::getThrowable)
        .isPresent()
        .get()
        .extracting(Optional::get)
        .isInstanceOf(PSQLException.class);
    });
  }

  @Flyway(
    testDataScripts = "db/testdata/invalid.sql"
  )
  static class TestWithInvalidTestdata {
    @Test
    public void testMethodDisabled() {
      assertThat(false).isTrue();
    }
  }
}
