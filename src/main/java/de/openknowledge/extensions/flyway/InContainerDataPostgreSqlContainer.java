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
package de.openknowledge.extensions.flyway;

import java.util.List;
import java.util.Objects;

import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.PostgreSQLContainer;

import com.github.dockerjava.api.model.Image;

public class InContainerDataPostgreSqlContainer extends PostgreSQLContainer<InContainerDataPostgreSqlContainer>
  implements TaggableContainer {

  private static final String IMAGE_NAME = "postgres";

  public InContainerDataPostgreSqlContainer() {
    super();
    initCommand();
  }

  public InContainerDataPostgreSqlContainer(String dockerImage) {
    super(dockerImage);
    initCommand();
  }

  private void initCommand() {
    this.withEnv("PGDATA", "/var/lib/postgresql/data-local");
  }

  public void tag(String hash) {
    String commitedImage = getDockerClient().commitCmd(getContainerId()).exec();
    getDockerClient().tagImageCmd(commitedImage, IMAGE_NAME, hash).exec();
  }

  public boolean containsTag(String tag) {
    String repoTag = getImageName(tag);
    List<Image> imageList = getDockerClient()
      .listImagesCmd()
      .exec();

    return imageList.stream()
      .map(i -> i.getRepoTags())
      .filter(Objects::nonNull)
      .anyMatch(t -> t.equals(repoTag));
  }

  @Override
  public String getImageName(String tag) {
    return IMAGE_NAME + ":" + tag;
  }

  @Override
  public int getContainerPort() {
    return POSTGRESQL_PORT;
  }

  @Override
  public void addFixedExposedPort(int hostPort, int containerPort) {
    super.addFixedExposedPort(hostPort, containerPort, InternetProtocol.TCP);
  }
}
