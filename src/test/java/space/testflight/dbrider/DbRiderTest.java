/*
 * Copyright 2021 Arne Limburg
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
package space.testflight.dbrider;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.configuration.Orthography;
import com.github.database.rider.core.api.connection.ConnectionHolder;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.SeedStrategy;
import com.github.database.rider.junit5.api.DBRider;

import space.testflight.ConfigProperty;
import space.testflight.Flyway;
import space.testflight.model.Customer;

@DBRider
@DBUnit(schema = "public", caseInsensitiveStrategy = Orthography.LOWERCASE)
@DataSet(value = "dbrider/customers.yml", strategy = SeedStrategy.INSERT)
@Flyway(
  database = Flyway.DatabaseType.POSTGRESQL,
  databaseInstance = Flyway.DatabaseInstanceScope.PER_TEST_METHOD,
  configuration = {
  @ConfigProperty(key = "space.testflight.jdbc.url.property", value = "javax.persistence.jdbc.url"),
  @ConfigProperty(key = "space.testflight.jdbc.username.property", value = "javax.persistence.jdbc.user"),
  @ConfigProperty(key = "space.testflight.jdbc.password.property", value = "javax.persistence.jdbc.password")
})
public class DbRiderTest {

  private static EntityManagerFactory entityManagerFactory;

  private ConnectionHolder connectionHolder;  // dbRider accesses this field with reflections

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
  void deleteUser() {
    assertThatUserWereCreatedByDbRider();

    entityManager.getTransaction().begin();
    entityManager.createQuery("Delete from Customer u where u.userName = 'dbrider'").executeUpdate();
    entityManager.getTransaction().commit();

    assertThat(loadUser("dbrider")).isNull();
  }

  @Test
  void createUser() {
    assertThatUserWereCreatedByDbRider();

    assertThat(loadUser("Hans")).isNull();

    entityManager.getTransaction().begin();
    entityManager.persist(new Customer("Hans", "hans@mail.de"));
    entityManager.getTransaction().commit();

    assertThat(loadUser("Hans").getUserName()).isEqualTo("Hans");
  }

  private void assertThatUserWereCreatedByDbRider() {
    Customer customer =
      entityManager.createQuery("Select u from Customer u where u.userName = 'dbrider'", Customer.class).getSingleResult();
    assertThat(customer).isNotNull();
  }

  private Customer loadUser(String username) {
    try {
      return entityManager.createQuery(String.format("Select u from Customer u where u.userName = '%s'", username), Customer.class)
        .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
