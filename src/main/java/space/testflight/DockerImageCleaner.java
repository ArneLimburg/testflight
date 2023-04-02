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

import static java.util.Optional.ofNullable;
import static space.testflight.AbstractDatabaseMigrationExtension.TESTFLIGHT_PREFIX;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;

public class DockerImageCleaner {

  /**
   * Removes all Docker images created by testflight from the local registry.
   *
   * @param args ignored
   */
  public static void main(String... args) {
    Logger.getLogger(DockerImageCleaner.class.getName()).info("Removing Testflight docker images");
    DockerImageCleaner cleaner = new DockerImageCleaner();
    cleaner.removeTestflightImages();
  }

  public void removeTestflightImages() {
    DockerClient client = DockerClientFactory.lazyClient();
    List<Image> images = client
      .listImagesCmd()
      .exec();

    images.stream().filter(byTestflightPrefix())
      .forEach(image -> client.removeImageCmd(image.getId()).exec());
  }

  Predicate<Image> byTestflightPrefix() {
    return image -> ofNullable(image.getRepoTags())
            .map(Arrays::stream)
            .map(tags -> tags.anyMatch(tag -> tag.contains(":" + TESTFLIGHT_PREFIX)))
            .orElse(false);
  }
}
