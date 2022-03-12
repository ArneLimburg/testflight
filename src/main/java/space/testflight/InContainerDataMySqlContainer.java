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

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

public class InContainerDataMySqlContainer extends MySQLContainer<InContainerDataMySqlContainer>
  implements DefaultTaggableContainer<InContainerDataMySqlContainer> {

  public InContainerDataMySqlContainer(String image) {
    setImage(new ImageFromDockerfile(image)
      .withDockerfileFromBuilder(builder ->
      builder
      .from(image)
      .run("mkdir -p /var/lib/mysql-data && chown -R mysql:mysql /var/lib/mysql-data")
      .build()));
    withClasspathResourceMapping("/space/testflight/mysql/mysql.cnf", "/etc/mysql/conf.d/mysql.cnf", BindMode.READ_ONLY);
    exposeContainerPort();
  }

  @Override
  public int getContainerPort() {
    return MYSQL_PORT;
  }

  @Override
  public String getDefaultImageName() {
    return NAME;
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
//
//      Ports ports = cmd.getHostConfig().getPortBindings();
//      if (!ports.getBindings().containsKey(containerPort)) {
//        ports.bind(ExposedPort.tcp(getContainerPort()), Ports.Binding.empty());
//        cmd.getHostConfig().withPortBindings(ports);
//      }
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
