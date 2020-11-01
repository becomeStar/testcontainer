package testconainers.wrapper;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.CassandraContainer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.IntStream;

import static testconainers.constant.TestContainerConstant.*;

public class CassandraContainerWrapper extends CassandraContainer {

    public CassandraContainerWrapper(String confluentPlatformVersion) {
        super(confluentPlatformVersion);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        if (!reused) {
            System.out.println("#####################");
            System.out.println("######first used#####");
            System.out.println("#####################");
            Cluster cluster = this.getCluster();
            try (Session session = cluster.connect()) {
                session.execute(KEYSPACE_QUERY);
                session.execute(TABLE_CREATE_QUERY);
                PreparedStatement prepared = session.prepare(INSERT_PREPARED_STATEMENT);

                IntStream.range(1, 101)
                        .forEach(i ->
                                session.execute(prepared.bind("storage_data_key_" + i,
                                        LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                        );
            }
        } else {
            System.out.println("#####################");
            System.out.println("######re used#####");
            System.out.println("#####################");
        }
    }

}
