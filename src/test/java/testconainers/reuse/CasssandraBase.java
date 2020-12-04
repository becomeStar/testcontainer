package testconainers.reuse;


import testconainers.wrapper.CassandraContainerWrapper;

public abstract class CasssandraBase {

    static final CassandraContainerWrapper CASSANDRA_CONTAINER;

    static {
        CASSANDRA_CONTAINER = (CassandraContainerWrapper) new CassandraContainerWrapper("cassandra:3.11.2")
                .withReuse(true)
                .withLabel("reuse.image.name", "reuse-test-version-1");

        CASSANDRA_CONTAINER.start();
    }

}
