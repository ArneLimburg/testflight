/*
 * Copyright 2020 Arne Limburg
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

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import space.testflight.Flyway.DatabaseType;
import space.testflight.FlywayExtension.StartupType;

public class DatabaseContainerFactory {

  private static final Map<DatabaseType, String> DATABASE_CONTAINER_CLASS_NAMES;

  static {
    EnumMap<DatabaseType, String> databaseContainerClassNames = new EnumMap<DatabaseType, String>(DatabaseType.class);
    databaseContainerClassNames.put(DatabaseType.POSTGRESQL, "space.testflight.InContainerDataPostgreSqlContainer");
    databaseContainerClassNames.put(DatabaseType.MYSQL, "space.testflight.InContainerDataMySqlContainer");
    DATABASE_CONTAINER_CLASS_NAMES = unmodifiableMap(databaseContainerClassNames);
  }

  public <C extends JdbcDatabaseContainer<C> & TaggableContainer> C createDatabaseContainer(
    ExtensionContext context, DatabaseType databaseType, StartupType startup) {

    Optional<Flyway> configuration = findAnnotation(context.getTestClass(), Flyway.class);
    String tag = FlywayExtension.getClassStore(context).get(FlywayExtension.MIGRATION_TAG, String.class);
    Optional<String> imageName = ofNullable(FlywayExtension.getGlobalStore(context, tag).get(FlywayExtension.STORE_IMAGE, String.class));
    imageName = of(imageName.orElse(configuration.map(Flyway::dockerImage).orElse(""))).filter(image -> !image.isEmpty());

    C container = createDatabaseContainer(databaseType, imageName);
    if (startup == StartupType.FAST) {
      container.setWaitStrategy(Wait.forLogMessage(databaseType.getStartupLogMessage(), 1));
    }
    return (C)container;
  }

  private <C extends JdbcDatabaseContainer<C> & TaggableContainer> C createDatabaseContainer(
    DatabaseType databaseType, Optional<String> imageName) {

    try {
      String className = DATABASE_CONTAINER_CLASS_NAMES.get(databaseType);
      Class<C> containerType = (Class)DatabaseContainerFactory.class.getClassLoader().loadClass(className);
      if (imageName.isPresent()) {
        return containerType.getDeclaredConstructor(String.class).newInstance(imageName.get());
      } else {
        return containerType.getDeclaredConstructor().newInstance();
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
