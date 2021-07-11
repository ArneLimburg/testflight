/*
 * Copyright 2020 - 2021 Arne Limburg
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

import static java.util.Optional.ofNullable;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static space.testflight.DatabaseInstanceScope.PER_TEST_METHOD;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.github.dockerjava.api.model.Image;

public class FlywayExtension implements BeforeAllCallback, BeforeEachCallback, BeforeTestExecutionCallback,
  AfterTestExecutionCallback, AfterEachCallback, AfterAllCallback, TestInstancePostProcessor, ParameterResolver {

  public static final String TESTFLIGHT_PREFIX = "testflight-";
  static final String JDBC_URL = "space.testflight.jdbc.url";
  static final String JDBC_USERNAME = "space.testflight.jdbc.username";
  static final String JDBC_PASSWORD = "space.testflight.jdbc.password";
  static final String STORE_CONFIGURATION = "configuration";
  static final String MIGRATION_TAG = "migration.tag";
  static final String STORE_IMAGE = "image";
  static final String STORE_CONTAINER = "container";
  private static final String JDBC_URL_PROPERTY = "space.testflight.jdbc.url.property";
  private static final String JDBC_USERNAME_PROPERTY = "space.testflight.jdbc.username.property";
  private static final String JDBC_PASSWORD_PROPERTY = "space.testflight.jdbc.password.property";
  private static final String JDBC_PORT = "jdbc.port";
  private static Map<String, JdbcDatabaseContainer<?>> suiteContainers = new ConcurrentHashMap<String, JdbcDatabaseContainer<?>>();

  private final ResourceInjector injector = new ResourceInjector();
  private final DatabaseContainerFactory containerFactory = new DatabaseContainerFactory();

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    initialize(context);

    if (getDatabaseInstance(context) == DatabaseInstanceScope.PER_TEST_CLASS) {
      startDatabase(context, false);
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    if (getDatabaseInstance(context) == DatabaseInstanceScope.PER_TEST_METHOD) {
      startDatabase(context, true);
    }
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    if (getDatabaseInstance(context) == DatabaseInstanceScope.PER_TEST_EXECUTION) {
      startDatabase(context, true);
    }
  }

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    if (getDatabaseInstance(context) == DatabaseInstanceScope.PER_TEST_EXECUTION) {
      teardownDatabase(context, true);
    }
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    if (getDatabaseInstance(context) == DatabaseInstanceScope.PER_TEST_METHOD) {
      teardownDatabase(context, true);
    }
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (getDatabaseInstance(context) == DatabaseInstanceScope.PER_TEST_CLASS) {
      teardownDatabase(context, false);
    }

    Store classStore = getClassStore(context);
    TestflightConfiguration configuration = classStore.get(STORE_CONFIGURATION, TestflightConfiguration.class);
    if (configuration.getDatabaseInstanceScope() != DatabaseInstanceScope.PER_TEST_SUITE) {
      getGlobalStore(context, classStore.get(MIGRATION_TAG, String.class)).get(STORE_CONTAINER, AutoCloseable.class).close();
    }
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
    injector.inject(testInstance, context);
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return injector.canInject(parameterContext.getParameter());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    try {
      return injector.getInjectedValue(parameterContext.getParameter(), extensionContext);
    } catch (ReflectiveOperationException e) {
      throw new ParameterResolutionException(e.getMessage(), e);
    }
  }

  private void startDatabase(ExtensionContext context, boolean useMethodLevelStore) {
    String tag = getClassStore(context).get(MIGRATION_TAG, String.class);
    JdbcDatabaseContainer<?> container = getGlobalStore(context, tag).get(STORE_CONTAINER, JdbcDatabaseContainer.class);
    if (!container.isRunning()) {
      container = createContainer(context, StartupType.FAST);
      container.start();
    }

    if (useMethodLevelStore) {
      getMethodStore(context).put(STORE_CONTAINER, container);
    } else {
      getClassStore(context).put(STORE_CONTAINER, container);
    }
  }

  private void teardownDatabase(ExtensionContext context, boolean useMethodLevelStore) throws Exception {
    if (useMethodLevelStore) {
      getMethodStore(context).get(STORE_CONTAINER, AutoCloseable.class).close();
    } else {
      getClassStore(context).get(STORE_CONTAINER, AutoCloseable.class).close();
    }
  }

  private <C extends JdbcDatabaseContainer<C> & TaggableContainer> void initialize(ExtensionContext context) throws Exception {
    TestflightConfiguration configuration = new FlywayConfiguration(findAnnotation(context.getTestClass(), Flyway.class));
    Optional<String> currentMigrationTarget = configuration.getCurrentMigrationTarget();
    int testDataTagSuffix = configuration.getTestDataTagSuffix();
    String tagName = TESTFLIGHT_PREFIX + currentMigrationTarget.orElse("") + testDataTagSuffix;

    Store globalUrlStore = getGlobalStore(context);
    Store globalTaggedStore = getGlobalStore(context, tagName);
    Store classStore = getClassStore(context);
    String image = configuration.getDockerImage(tagName);
    classStore.put(STORE_CONFIGURATION, configuration);
    classStore.put(MIGRATION_TAG, tagName);

    C container;
    C suiteContainer = (C)suiteContainers.get(tagName);
    if (suiteContainer != null && suiteContainer.isRunning()) {
      container = suiteContainer;
    } else if (!existsImage(image)) {
      container = createContainer(context, StartupType.SLOW);
      container.start();

      prefillDatabase(configuration, container);
      container.tag(tagName);
      globalTaggedStore.put(STORE_IMAGE, image);
    } else {
      globalTaggedStore.put(STORE_IMAGE, image);
      container = createContainer(context, StartupType.FAST);
      container.start();
    }
    globalUrlStore.put(JDBC_URL, container.getJdbcUrl());
    globalUrlStore.put(JDBC_USERNAME, container.getUsername());
    globalUrlStore.put(JDBC_PASSWORD, container.getPassword());
    globalUrlStore.put(JDBC_PORT, container.getMappedPort(container.getContainerPort()));
    globalTaggedStore.put(STORE_CONTAINER, container);
    setSystemProperties(configuration, globalUrlStore);
    if (configuration.getDatabaseInstanceScope() == DatabaseInstanceScope.PER_TEST_SUITE) {
      Object previous = suiteContainers.putIfAbsent(tagName, container);
      if (previous == null) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> container.stop()));
      }
    }
  }

  private void prefillDatabase(
    TestflightConfiguration configuration,
    JdbcDatabaseContainer<?> container) throws SQLException {
    configuration.applyToDatabase(container);
  }

  private void setSystemProperties(TestflightConfiguration configuration, Store globalStore) {
    String urlPropertyName = configuration.getProperties().entrySet()
      .stream()
      .filter(entry -> entry.getKey().equals(JDBC_URL_PROPERTY))
      .findAny()
      .map(Entry::getValue)
      .orElse(JDBC_URL);
    String userPropertyName = configuration.getProperties().entrySet()
      .stream()
      .filter(entry -> entry.getKey().equals(JDBC_USERNAME_PROPERTY))
      .findAny()
      .map(Entry::getValue)
      .orElse(JDBC_USERNAME);
    String passwordPropertyName = configuration.getProperties().entrySet()
      .stream()
      .filter(entry -> entry.getKey().equals(JDBC_PASSWORD_PROPERTY))
      .findAny()
      .map(Entry::getValue)
      .orElse(JDBC_PASSWORD);
    System.setProperty(urlPropertyName, globalStore.get(JDBC_URL, String.class));
    System.setProperty(userPropertyName, globalStore.get(JDBC_USERNAME, String.class));
    System.setProperty(passwordPropertyName, globalStore.get(JDBC_PASSWORD, String.class));
  }

  static ExtensionContext.Store getGlobalStore(ExtensionContext context) {
    return context.getRoot().getStore(Namespace.create(FlywayExtension.class));
  }

  static ExtensionContext.Store getGlobalStore(ExtensionContext context, String tag) {
    return context.getRoot().getStore(Namespace.create(FlywayExtension.class, tag));
  }

  static ExtensionContext.Store getClassStore(ExtensionContext context) {
    return context.getStore(Namespace.create(context.getRequiredTestClass()));
  }

  private ExtensionContext.Store getMethodStore(ExtensionContext context) {
    return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
  }

  private <C extends JdbcDatabaseContainer<C> & TaggableContainer> C createContainer(ExtensionContext context, StartupType startup) {
    Optional<Flyway> configuration = findAnnotation(context.getTestClass(), Flyway.class);
    C container;
    if (!configuration.isPresent()) {
      container = containerFactory.createDatabaseContainer(context, DatabaseType.POSTGRESQL, startup);
    } else {
      Flyway flywayConfiguration = configuration.get();
      container = containerFactory.createDatabaseContainer(context, flywayConfiguration.database(), startup);
    }
    Optional.ofNullable(getGlobalStore(context).get(JDBC_PORT, Integer.class))
      .ifPresent(hostPort -> container.addFixedPort(hostPort, container.getContainerPort()));
    return container;
  }

  private boolean existsImage(String imageName) {
    List<Image> imageList = DockerClientFactory.lazyClient()
      .listImagesCmd()
      .exec();

    return imageList.stream()
      .map(i -> i.getRepoTags())
      .filter(Objects::nonNull)
      .flatMap(Arrays::stream)
      .anyMatch(t -> t.equals(imageName));

  }

  private DatabaseInstanceScope getDatabaseInstance(ExtensionContext context) {
    return ofNullable(getClassStore(context).get(STORE_CONFIGURATION, TestflightConfiguration.class))
        .map(TestflightConfiguration::getDatabaseInstanceScope)
        .orElse(PER_TEST_METHOD);
  }

  enum StartupType {
    SLOW, FAST;
  }
}
