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
package space.testflight.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import space.testflight.ConfigProperty;
import space.testflight.Flyway;
import space.testflight.model.Customer;

@Flyway(configuration = {
  @ConfigProperty(key = "space.testflight.jdbc.url.property", value = "spring.datasource.url"),
  @ConfigProperty(key = "space.testflight.jdbc.username.property", value = "spring.datasource.username"),
  @ConfigProperty(key = "space.testflight.jdbc.password.property", value = "spring.datasource.password")
})
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class SpringWebTest {
  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  public void getAll() {
    List<Customer> result = restTemplate.exchange(
      "http://localhost:" + port + "/customers",
      HttpMethod.GET,
      HttpEntity.EMPTY,
      new ParameterizedTypeReference<List<Customer>>() { }).getBody();
    assertEquals(3, result.size());
  }
}
