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
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

class FailingInjectionTest {

  @Test
  void fieldInjectionIsFailingForWrongFieldName() throws Exception {
    TestExecutionResult result = executeTest(FailingFieldInjectionTest.class);
    assertThat(result.getThrowable()).isNotEmpty();
    assertThat(result.getThrowable().get()).hasMessageContaining("connection");
  }

  @Test
  void methodInjectionIsFailingForWrongType() throws Exception {
    TestExecutionResult result = executeTest(FailingMethodInjectionTest.class);
    assertThat(result.getThrowable()).isNotEmpty();
    assertThat(result.getThrowable().get()).hasMessageContaining("java.lang.Object");
  }

  TestExecutionResult executeTest(Class<?> testClass) throws Exception {
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
      .selectors(selectClass(testClass))
      .build();
    Launcher launcher = LauncherFactory.create();
    CompletableFuture<TestExecutionResult> result = new CompletableFuture<TestExecutionResult>();
    launcher.registerTestExecutionListeners(new RecordingExecutionListener(result));
    launcher.execute(request);

    return result.get();
  }

  private class RecordingExecutionListener implements TestExecutionListener {

    private CompletableFuture<TestExecutionResult> result;

    RecordingExecutionListener(CompletableFuture<TestExecutionResult> futureResult) {
      result = futureResult;
    }

    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
      result.complete(testExecutionResult);
    }
  }
}
