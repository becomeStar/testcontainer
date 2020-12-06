package testconainers.reuse;


import com.github.dockerjava.api.command.CreateContainerCmd;
import testconainers.wrapper.CassandraContainerWrapper;

import java.util.function.Consumer;

public abstract class CasssandraBase {

    static final CassandraContainerWrapper CASSANDRA_CONTAINER;

    static {
        CASSANDRA_CONTAINER = (CassandraContainerWrapper) new CassandraContainerWrapper("cassandra:3.11.2")
                .withReuse(true)
                .withLabel("reuse.cassandra.version", "cassandra-1");

        CASSANDRA_CONTAINER.start();
    }

}
