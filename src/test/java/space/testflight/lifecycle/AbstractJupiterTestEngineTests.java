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

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

public class AbstractJupiterTestEngineTests {
  private final JupiterTestEngine engine = new JupiterTestEngine();

  protected EngineExecutionResults executeTestsForClasses(Class<?>... testClasses) {
    final ClassSelector[] classSelectors = Arrays.stream(testClasses)
      .map(DiscoverySelectors::selectClass)
      .collect(Collectors.toList())
      .toArray(new ClassSelector[]{});

    return executeTests(classSelectors);
  }

  protected EngineExecutionResults executeTestsForClass(Class<?> testClass) {
    return executeTests(selectClass(testClass));
  }

  protected EngineExecutionResults executeTests(DiscoverySelector... selectors) {
    return executeTests(request().selectors(selectors).build());
  }

  protected EngineExecutionResults executeTests(LauncherDiscoveryRequest request) {
    return EngineTestKit.execute(this.engine, request);
  }

  protected TestDescriptor discoverTests(DiscoverySelector... selectors) {
    return discoverTests(request().selectors(selectors).build());
  }

  protected TestDescriptor discoverTests(LauncherDiscoveryRequest request) {
    return engine.discover(request, UniqueId.forEngine(engine.getId()));
  }

  protected UniqueId discoverUniqueId(Class<?> clazz, String methodName) {
    TestDescriptor engineDescriptor = discoverTests(selectMethod(clazz, methodName));
    Set<? extends TestDescriptor> descendants = engineDescriptor.getDescendants();
    // @formatter:off
    TestDescriptor testDescriptor = descendants.stream()
      .skip(descendants.size() - 1)
      .findFirst()
      .orElseGet(() -> fail("no descendants"));
    // @formatter:on
    return testDescriptor.getUniqueId();
  }
}
