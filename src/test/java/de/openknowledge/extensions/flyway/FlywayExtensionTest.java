/*
 * Copyright 2020 René Frerichs
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
package de.openknowledge.extensions.flyway;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.openknowledge.extensions.User;

@Flyway
public class FlywayExtensionTest {

  private EntityManager entityManager;

  @BeforeAll
  void setUp() {
    EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("test-unit");
    entityManager = entityManagerFactory.createEntityManager();

    User peter = new User("Peter", "peter@mail.de");
    User hans = new User("Hans", "hans@mail.de");

    entityManager.persist(peter);
    entityManager.persist(hans);
  }

  @Test
  void initialTest() {
    List<User> users = entityManager.createQuery("Select u from User u", User.class).getResultList();

    //assert

  }
}
