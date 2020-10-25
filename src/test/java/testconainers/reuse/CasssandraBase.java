package testconainers.reuse;

import org.testcontainers.containers.CassandraContainer;

public abstract class CasssandraBase {

    static final CassandraContainer<?> CASSANDRA_CONTAINER;

    static {
        CASSANDRA_CONTAINER = new CassandraContainer<>("cassandra:3.11.2")
                .withNetwork(null)
                .withReuse(true);
        CASSANDRA_CONTAINER.start();
    }

}
