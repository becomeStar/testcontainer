package testconainers.singleton;

import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractContainerBaseTest {

    static final CassandraContainer<?> CASSANDRA_CONTAINER;

    static final LocalStackContainer LOCAL_STACK_CONTAINER;

    static {
        CASSANDRA_CONTAINER = new CassandraContainer<>("testconainers.cassandra:3.11.2");
        CASSANDRA_CONTAINER.start();
        LOCAL_STACK_CONTAINER = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.11.3"))
                .withServices(LocalStackContainer.Service.S3);
        LOCAL_STACK_CONTAINER.start();
    }

}
