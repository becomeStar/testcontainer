package testconainers;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import testconainers.object.StorageDataKey;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static constant.TestContainerConstant.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class CassandraTest {

    @Container
    public static final CassandraContainer<?> cassandra =
            new CassandraContainer<>("cassandra:3.11.2");


    @Test
    public void test() {
        Cluster cluster = cassandra.getCluster();

        try (Session session = cluster.connect()) {

            session.execute(KEYSPACE_QUERY);
            session.execute(TABLE_CREATE_QUERY);

            PreparedStatement prepared = session.prepare(INSERT_PREPARED_STATEMENT);

            IntStream.range(1, 101)
                    .forEach(i ->
                            session.execute(prepared.bind("storage_data_key_" + i,
                                    LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                    );

            IntStream.range(1, 101)
                    .forEach(i ->
                            session.execute(prepared.bind("storage_garbage_key_" + i,
                                    LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                    );

            MappingManager mappingManager = new MappingManager(session);
            Mapper<StorageDataKey> m = mappingManager.mapper(StorageDataKey.class);

            List<StorageDataKey> result = m.map(session.execute(SELECT_QUERY)).all();

            assertEquals(100, result.size());


        }
    }

}
