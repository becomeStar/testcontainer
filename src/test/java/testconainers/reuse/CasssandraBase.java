package testconainers.reuse;


public abstract class CasssandraBase {

    static final CassandraContainer<?> CASSANDRA_CONTAINER;

    static {
        CASSANDRA_CONTAINER = new CassandraContainer<>("testconainers.cassandra:3.11.2")
                .withNetwork(null)
                .withReuse(true);
        CASSANDRA_CONTAINER.start();
    }

}
