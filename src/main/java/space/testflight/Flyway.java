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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static org.testcontainers.containers.MySQLContainer.NAME;
import static org.testcontainers.containers.PostgreSQLContainer.IMAGE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(FlywayExtension.class)
@Target({ANNOTATION_TYPE, TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Flyway {
  DatabaseType database() default DatabaseType.POSTGRESQL;
  String dockerImage() default "";
  DatabaseInstanceScope databaseInstance() default DatabaseInstanceScope.PER_TEST_METHOD;
  String[] testDataScripts() default {};
  ConfigProperty[] configuration() default {};

  enum DatabaseType {
    POSTGRESQL(IMAGE),
    MYSQL(NAME);

    private String image;

    DatabaseType(String databaseImage) {
      this.image = databaseImage;
    }

    public String getImage() {
      return image;
    }

    public String getImage(String tag) {
      return getImage() + ":" + tag;
    }
  }

  enum DatabaseInstanceScope {
    PER_TEST_EXECUTION, // after @BeforeEach works with parameterized tests
    PER_TEST_METHOD,    // before @BeforeEach
    PER_TEST_CLASS      // before @BeforeAll
  }
}
