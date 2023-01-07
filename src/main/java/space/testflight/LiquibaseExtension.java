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
package space.testflight;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static space.testflight.DatabaseInstanceScope.PER_TEST_METHOD;

import java.util.Optional;

public class LiquibaseExtension extends AbstractDatabaseMigrationExtension {

  @Override
  protected TestflightConfiguration createConfiguration(Optional<Class<?>> testClass) {
    return new LiquibaseConfiguration(findAnnotation(testClass, Liquibase.class));
  }

  @Override
  protected DatabaseInstanceScope getDatabaseInstance(Optional<Class<?>> testClass) {
    Optional<Liquibase> configuration = findAnnotation(testClass, Liquibase.class);
    return configuration.map(Liquibase::databaseInstance).orElse(PER_TEST_METHOD);
  }

  @Override
  protected boolean getReuse(Optional<Class<?>> testClass) {
    Optional<Liquibase> configuration = findAnnotation(testClass, Liquibase.class);
    return configuration.map(Liquibase::reuse).orElse(false);
  }

  @Override
  protected Optional<DatabaseType> getDatabaseType(Optional<Class<?>> testClass) {
    Optional<Liquibase> configuration = findAnnotation(testClass, Liquibase.class);
    return configuration.map(Liquibase::database);
  }
}
