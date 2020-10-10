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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.openknowledge.extensions.Customer;
import de.openknowledge.extensions.flyway.Flyway.DatabaseType;

@Flyway(database = DatabaseType.POSTGRESQL, testDataScripts = {"db/testdata/init.sql", "db/testdata/initTwo.sql"})
public class FlywayExtensionTest {

  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    Map<String, String> properties = new HashMap<>();
    properties.put("javax.persistence.jdbc.url", System.getProperty("jdbc.url"));
    properties.put("javax.persistence.jdbc.user", System.getProperty("jdbc.username"));
    properties.put("javax.persistence.jdbc.password", System.getProperty("jdbc.password"));
    EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("test-unit", properties);
    entityManager = entityManagerFactory.createEntityManager();

    Persistence.generateSchema("test-unit", properties);
  }

  @Test
  void initialTest() {
    System.out.println("===== initialTest start ======================================= ");
    Customer hans = new Customer("Hans", "hans@mail.de");

    entityManager.getTransaction().begin();
    entityManager.persist(hans);
    entityManager.getTransaction().commit();

    List<Customer> customers = entityManager.createQuery("Select u from Customer u", Customer.class).getResultList();

    assertThat(customers).hasSize(6);
    assertThat(customers).anyMatch(c -> c.getUserName().equals("Hans"));
    assertThat(customers).anyMatch(c -> c.getUserName().equals("Admin"));// in flyway script
    assertThat(customers).anyMatch(c -> c.getUserName().equals("tesdataUser"));// in init script
    assertThat(customers).anyMatch(c -> c.getUserName().equals("tesdataUser2"));// in second init script
    System.out.println("===== initialTest stop ======================================= ");
  }

  @Test
  void secondTest() {
    System.out.println("===== secondTest start ======================================= ");
    Customer peter = new Customer("Peter", "peter@mail.de");

    entityManager.getTransaction().begin();
    entityManager.persist(peter);
    entityManager.getTransaction().commit();

    List<Customer> customers = entityManager.createQuery("Select u from Customer u", Customer.class).getResultList();

    assertThat(customers).hasSize(6);
    assertThat(customers).anyMatch(c -> c.getUserName().equals("Peter"));
    assertThat(customers).anyMatch(c -> c.getUserName().equals("Admin"));// in flyway script
    assertThat(customers).anyMatch(c -> c.getUserName().equals("tesdataUser"));// in init script
    assertThat(customers).anyMatch(c -> c.getUserName().equals("tesdataUser2"));// in second init script
    System.out.println("===== secondTest stop ======================================= ");
  }
}
