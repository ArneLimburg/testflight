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
package space.testflight.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import space.testflight.ConfigProperty;
import space.testflight.Flyway;
import space.testflight.Flyway.DatabaseType;
import space.testflight.model.Customer;

@Flyway(
  database = DatabaseType.POSTGRESQL,
  testDataScripts = {"db/testdata/init.sql", "db/testdata/initTwo.sql"},
  configuration = {
  @ConfigProperty(key = "space.testflight.jdbc.url.property", value = "javax.persistence.jdbc.url"),
  @ConfigProperty(key = "space.testflight.jdbc.username.property", value = "javax.persistence.jdbc.user"),
  @ConfigProperty(key = "space.testflight.jdbc.password.property", value = "javax.persistence.jdbc.password")
})
public class PostgreSqlTest {

  private static EntityManagerFactory entityManagerFactory;
  private EntityManager entityManager;

  @BeforeAll
  static void createEntityManagerFactory() {
    entityManagerFactory = Persistence.createEntityManagerFactory("test-unit", System.getProperties());
  }

  @AfterAll
  static void closeEntityManagerFactory() {
    entityManagerFactory.close();
  }

  @BeforeEach
  void createEntityManager() {
    entityManager = entityManagerFactory.createEntityManager();
  }

  @AfterEach
  void closeEntityManager() {
    entityManager.close();
  }

  @Test
  void initialTest() {
    Customer hans = new Customer("Hans", "hans@mail.de");

    entityManager.getTransaction().begin();
    entityManager.persist(hans);
    entityManager.getTransaction().commit();

    List<Customer> customers = entityManager.createQuery("Select u from Customer u", Customer.class).getResultList();

    assertThat(customers).hasSize(6);
    assertThat(customers).anyMatch(c -> c.getUserName().equals("Hans"));
    assertThat(customers).anyMatch(c -> c.getUserName().equals("Admin")); // in flyway script
    assertThat(customers).anyMatch(c -> c.getUserName().equals("tesdataUser")); // in init script
    assertThat(customers).anyMatch(c -> c.getUserName().equals("tesdataUser2")); // in second init script
  }

  @Test
  void secondTest() {
    Customer peter = new Customer("Peter", "peter@mail.de");

    entityManager.getTransaction().begin();
    entityManager.persist(peter);
    entityManager.getTransaction().commit();

    List<Customer> customers = entityManager.createQuery("Select u from Customer u", Customer.class).getResultList();

    assertThat(customers).hasSize(6);
    assertThat(customers).anyMatch(c -> c.getUserName().equals("Peter"));
    assertThat(customers).anyMatch(c -> c.getUserName().equals("Admin")); // in flyway script
    assertThat(customers).anyMatch(c -> c.getUserName().equals("tesdataUser")); // in init script
    assertThat(customers).anyMatch(c -> c.getUserName().equals("tesdataUser2")); // in second init script
  }
}
