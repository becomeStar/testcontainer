package testconainers.reuse.simple_reuse;

import org.testcontainers.containers.CassandraContainer;

public abstract class CasssandraBase {

    static final CassandraContainer<?> CASSANDRA_CONTAINER;

    static {
        CASSANDRA_CONTAINER = new CassandraContainer<>("cassandra:3.11.2")
                .withReuse(true)
                .withLabel("reuse.image.name", "reuse-test-version-2");

        CASSANDRA_CONTAINER.start();
    }

}
