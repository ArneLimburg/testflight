/*
 * Copyright 2020 - 2023 Arne Limburg
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
import static space.testflight.DatabaseInstanceScope.PER_TEST_METHOD;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

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

public abstract class AbstractDatabaseMigrationExtension implements BeforeAllCallback, BeforeEachCallback, BeforeTestExecutionCallback,
  AfterTestExecutionCallback, AfterEachCallback, AfterAllCallback, TestInstancePostProcessor, ParameterResolver {

  public static final String TESTFLIGHT_PREFIX = "testflight-";
  static final String JDBC_URL = "space.testflight.jdbc.url";
  static final String JDBC_USERNAME = "space.testflight.jdbc.username";
  static final String JDBC_PASSWORD = "space.testflight.jdbc.password";
  static final String STORE_IMAGE = "image";
  static final String STORE_CONTAINER = "container";
  private static final String STORE_CONFIGURATION = "configuration";
  private static final String STORE_MIGRATION_TAG = "migration.tag";
  private static final String JDBC_URL_PROPERTY = "space.testflight.jdbc.url.property";
  private static final String JDBC_USERNAME_PROPERTY = "space.testflight.jdbc.username.property";
  private static final String JDBC_PASSWORD_PROPERTY = "space.testflight.jdbc.password.property";
  private static final String JDBC_PORT = "jdbc.port";

  private final ResourceInjector injector = new ResourceInjector();
  private final DatabaseContainerFactory containerFactory = new DatabaseContainerFactory();

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    initialize(context);

    if (getDatabaseInstance(context) == DatabaseInstanceScope.PER_TEST_CLASS) {
      startDatabase(context);
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    if (getDatabaseInstance(context) == DatabaseInstanceScope.PER_TEST_METHOD) {
      startDatabase(context);
    }
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    if (getDatabaseInstance(context) == DatabaseInstanceScope.PER_TEST_EXECUTION) {
      startDatabase(context);
    }
  }

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    if (getDatabaseInstance(context) == DatabaseInstanceScope.PER_TEST_EXECUTION) {
      teardownDatabase(context);
    }
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    if (getDatabaseInstance(context) == DatabaseInstanceScope.PER_TEST_METHOD) {
      teardownDatabase(context);
    }
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (getDatabaseInstance(context) == DatabaseInstanceScope.PER_TEST_CLASS) {
      teardownDatabase(context);
    }

    TestflightConfiguration configuration = getConfiguration(context);
    if (configuration.getDatabaseInstanceScope() != DatabaseInstanceScope.PER_TEST_SUITE) {
      String tagName = getMigrationTagName(context);
      getContainerStore(context, configuration.getDatabaseInstanceScope()).get(tagName, AutoCloseable.class).close();
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

  protected abstract TestflightConfiguration createConfiguration(Optional<Class<?>> testClass);

  protected abstract DatabaseInstanceScope getDatabaseInstance(Optional<Class<?>> testClass);

  protected abstract boolean getReuse(Optional<Class<?>> testClass);

  protected abstract Optional<DatabaseType> getDatabaseType(Optional<Class<?>> testClass);

  private void startDatabase(ExtensionContext context) {

    Store containerStore = getContainerStore(context, getConfiguration(context).getDatabaseInstanceScope());
    String tagName = getMigrationTagName(context);
    JdbcDatabaseContainer<?> container = containerStore.get(tagName, JdbcDatabaseContainer.class);
    if (!container.isRunning()) {
      container = createContainer(context, ImageType.TAGGED);
      container.start();
    }

    containerStore.put(tagName, container);
  }

  private void teardownDatabase(ExtensionContext context) throws Exception {
    Store containerStore = getContainerStore(context, getConfiguration(context).getDatabaseInstanceScope());
    containerStore.get(getMigrationTagName(context), AutoCloseable.class).close();
  }

  private <C extends JdbcDatabaseContainer<C> & TaggableContainer> void initialize(ExtensionContext context)
      throws IOException, SQLException, URISyntaxException {
    TestflightConfiguration configuration = createConfiguration(context.getTestClass());
    Optional<String> currentMigrationTarget = configuration.getCurrentMigrationTarget();
    int testDataTagSuffix = configuration.getTestDataTagSuffix();
    String tagName = TESTFLIGHT_PREFIX + currentMigrationTarget.orElse("") + testDataTagSuffix;

    Store containerStore = getContainerStore(context, configuration.getDatabaseInstanceScope());
    storeConfiguration(context, configuration, tagName);

    String image = configuration.getDockerImage(tagName);

    C container;
    C previous = (C)containerStore.get(tagName);
    if (previous != null && previous.isRunning()) {
      container = previous;
    } else if (!existsImage(image)) {
      container = createContainer(context, ImageType.DEFAULT);
      container.start();

      prefillDatabase(configuration, container);
      container.tag(tagName);
    } else {
      container = createContainer(context, ImageType.TAGGED);
      container.start();
    }
    containerStore.put(JDBC_URL, container.getJdbcUrl());
    containerStore.put(JDBC_USERNAME, container.getUsername());
    containerStore.put(JDBC_PASSWORD, container.getPassword());
    containerStore.put(JDBC_PORT, container.getMappedPort(container.getContainerPort()));
    containerStore.put(tagName, container);
    setSystemProperties(configuration, containerStore);
    if (configuration.getDatabaseInstanceScope() == DatabaseInstanceScope.PER_TEST_SUITE && previous == null) {
      Runtime.getRuntime().addShutdownHook(new Thread(container::stop));
    }
  }

  private void storeConfiguration(ExtensionContext context, TestflightConfiguration configuration, String tagName) {
    Store classStore = getClassStore(context);
    classStore.put(STORE_CONFIGURATION, configuration);
    classStore.put(STORE_MIGRATION_TAG, tagName);
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

  static ExtensionContext.Store getContainerStore(ExtensionContext context, DatabaseInstanceScope scope) {
    return context.getRoot().getStore(Namespace.create(AbstractDatabaseMigrationExtension.class, scope));
  }

  static String getMigrationTagName(ExtensionContext context) {
    return getClassStore(context).get(STORE_MIGRATION_TAG, String.class);
  }

  static TestflightConfiguration getConfiguration(ExtensionContext context) {
    return getClassStore(context).get(STORE_CONFIGURATION, TestflightConfiguration.class);
  }

  private static ExtensionContext.Store getClassStore(ExtensionContext context) {
    return context.getStore(Namespace.create(context.getRequiredTestClass()));
  }

  private <C extends JdbcDatabaseContainer<C> & TaggableContainer> C createContainer(ExtensionContext context, ImageType imageType) {
    Optional<DatabaseType> databaseType = getDatabaseType(context.getTestClass());
    C container;
    if (!databaseType.isPresent()) {
      container = containerFactory.createDatabaseContainer(context, DatabaseType.POSTGRESQL, imageType);
    } else {
      container = containerFactory.createDatabaseContainer(context, databaseType.get(), imageType);
      container.withReuse(getReuse(context.getTestClass()));
    }
    Store containerStore = getContainerStore(context, getDatabaseInstance(context.getTestClass()));
    Optional.ofNullable(containerStore.get(JDBC_PORT, Integer.class))
      .ifPresent(hostPort -> container.addFixedPort(hostPort, container.getContainerPort()));
    return container;
  }

  private boolean existsImage(String imageName) {
    List<Image> imageList = DockerClientFactory.lazyClient()
      .listImagesCmd()
      .exec();

    return imageList.stream()
      .map(Image::getRepoTags)
      .filter(Objects::nonNull)
      .flatMap(Arrays::stream)
      .anyMatch(t -> t.equals(imageName));

  }

  private DatabaseInstanceScope getDatabaseInstance(ExtensionContext context) {
    return ofNullable(getConfiguration(context))
        .map(TestflightConfiguration::getDatabaseInstanceScope)
        .orElse(PER_TEST_METHOD);
  }

  enum ImageType {
    DEFAULT, TAGGED;
  }
}
