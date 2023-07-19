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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static space.testflight.FlywayExtension.TESTFLIGHT_PREFIX;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.model.Image;

@Tag("clean")
class DockerImageCleanerTest {

  private static DockerClient client;

  @BeforeAll
  static void createDockerClient() {
    client = DockerClientFactory.lazyClient();
  }

  @BeforeEach
  void tagImage() {
    try (PostgreSQLContainer<?> postgreSqlContainer = new PostgreSQLContainer<>("postgres:15.2")) {
      postgreSqlContainer.start();
      String commitedImage = client.commitCmd(postgreSqlContainer.getContainerId()).exec();
      client.tagImageCmd(commitedImage, PostgreSQLContainer.IMAGE, TESTFLIGHT_PREFIX + hashCode()).exec();
    }
  }

  @Test
  void clean() {
    ListImagesCmd dockerImageLs = client.listImagesCmd();

    assertTrue(containsTagWithTestflightPrefix(dockerImageLs.exec()));

    DockerImageCleaner.main();

    assertFalse(containsTagWithTestflightPrefix(dockerImageLs.exec()));
  }

  private boolean containsTagWithTestflightPrefix(List<Image> images) {
    return images.stream()
            .map(Image::getRepoTags)
            .filter(Objects::nonNull)
            .flatMap(Arrays::stream)
            .anyMatch(tag -> tag.contains(TESTFLIGHT_PREFIX));
  }
}
