/*
 * Copyright 2020 Roman Ness
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
import javax.persistence.NoResultException;
import javax.persistence.Persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import space.testflight.ConfigProperty;
import space.testflight.DatabaseInstanceScope;
import space.testflight.DatabaseType;
import space.testflight.Flyway;
import space.testflight.model.Customer;

@Flyway(
  database = DatabaseType.POSTGRESQL,
  databaseInstance = DatabaseInstanceScope.PER_TEST_EXECUTION,
  configuration = {
    @ConfigProperty(key = "space.testflight.jdbc.url.property", value = "javax.persistence.jdbc.url"),
    @ConfigProperty(key = "space.testflight.jdbc.username.property", value = "javax.persistence.jdbc.user"),
    @ConfigProperty(key = "space.testflight.jdbc.password.property", value = "javax.persistence.jdbc.password")
  }
)
@TestMethodOrder(MethodOrderer.MethodName.class)
class PerTestExecutionTest {

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
  void aaainsertCustomer() {
    assertThat(getHans()).isNull();

    entityManager.getTransaction().begin();
    entityManager.persist(new Customer("Hans", "hans@mail.de"));
    entityManager.getTransaction().commit();

    assertThat(getHans().getUserName()).isEqualTo("Hans");
  }

  @Test
  void bbbinsertedCustomerDoesNoLongerExist() {
    assertThat(getHans()).isNull(); // container replaced with every method
  }

  @ParameterizedTest
  @ValueSource(strings = {"Georg", "Gregor"})
  void parameterizedTestsReceiveTheirOwnContainer(String username) {

    List<Customer> customers = entityManager.createQuery("Select u from Customer u", Customer.class).getResultList();
    assertThat(customers).hasSize(3);

    entityManager.getTransaction().begin();
    entityManager.persist(new Customer(username, username + "@mail.de"));
    entityManager.getTransaction().commit();

    customers = entityManager.createQuery("Select u from Customer u", Customer.class).getResultList();
    assertThat(customers).hasSize(4)
      .extracting(Customer::getUserName).contains(username);
  }

  private Customer getHans() {
    try {
      return entityManager.createQuery("Select u from Customer u where u.userName = 'Hans'", Customer.class).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
