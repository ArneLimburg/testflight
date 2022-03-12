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

import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.PostgreSQLContainer;

public class InContainerDataPostgreSqlContainer extends PostgreSQLContainer<InContainerDataPostgreSqlContainer>
  implements DefaultTaggableContainer<InContainerDataPostgreSqlContainer> {

  public InContainerDataPostgreSqlContainer(String dockerImage) {
    super(dockerImage);
    withEnv("PGDATA", "/var/lib/postgresql/data-local");
    exposeContainerPort();
  }

  @Override
  public String getDefaultImageName() {
    return IMAGE;
  }

  @Override
  public int getContainerPort() {
    return POSTGRESQL_PORT;
  }

  @Override
  public void addFixedPort(int hostPort, int containerPort) {
    super.addFixedExposedPort(hostPort, containerPort, InternetProtocol.TCP);
  }

  private void exposeContainerPort() {
    withCreateContainerCmdModifier(cmd -> {
//      List<ExposedPort> exposedPorts = new ArrayList<>();
//      ExposedPort containerPort = null;
//      for (ExposedPort p : cmd.getExposedPorts()) {
//        exposedPorts.add(p);
//        if (p.getPort() == getContainerPort()) {
//          containerPort = p;
//        }
//      }
//      if (containerPort == null) {
//        containerPort = ExposedPort.tcp(getContainerPort());
//        exposedPorts.add(containerPort);
//      }
//      cmd.withExposedPorts(exposedPorts);

//      Ports ports = cmd.getHostConfig().getPortBindings();
//      ExposedPort exposedContainerPort = ExposedPort.tcp(getContainerPort());
//      Binding[] bindings = ports.getBindings().get(exposedContainerPort);
//      String hostIp = null;
//      for (Binding binding: bindings) {
//        hostIp = binding.getHostIp();
//      }
//      if (hostIp == null) {
//        ports.bind(exposedContainerPort, Ports.Binding.empty());
//        cmd.getHostConfig().withPortBindings(ports);
//      }
    });
  }
}
