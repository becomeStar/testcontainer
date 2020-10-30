package testconainers.compose;

import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

public abstract class DockerComposeBase {

    static final DockerComposeContainer<?> DOCKER_COMPOSE_CONTAINER;

    static final int LOCALSTACK_S3_PORT = 4572;

    static final int CASSANDRA_PORT = 9042;

    static {
        DOCKER_COMPOSE_CONTAINER =
                new DockerComposeContainer<>(
                        new File("src/test/resources/s3-cassandra.yml"))
                        .withExposedService("localstack_1", LOCALSTACK_S3_PORT)
                        .withExposedService("cassandra_1", CASSANDRA_PORT,
                                Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));

        DOCKER_COMPOSE_CONTAINER.start();
    }


}
