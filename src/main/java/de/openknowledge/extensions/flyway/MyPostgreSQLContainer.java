package de.openknowledge.extensions.flyway;

import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.file.Path;

/**
 * @author Olaf Prins - open knowledge GmbH
 */
public class MyPostgreSQLContainer extends PostgreSQLContainer {

    public Path createVolumeDirectory() {
        return this.createVolumeDirectory(true);
    }
}
