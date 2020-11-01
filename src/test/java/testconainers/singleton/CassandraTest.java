package testconainers.singleton;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import testconainers.object.StorageDataKey;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static testconainers.constant.TestContainerConstant.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CassandraTest extends AbstractContainerBaseTest{


    @BeforeAll
    void setUp() {
        Cluster cluster = CASSANDRA_CONTAINER.getCluster();

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
        }
    }

    @AfterAll
    void confirmContainerIsRunning() {
        assertTrue(LOCAL_STACK_CONTAINER.isRunning());
        assertTrue(CASSANDRA_CONTAINER.isRunning());
    }

    @Test
    public void test() {
        Cluster cluster = CASSANDRA_CONTAINER.getCluster();

        try (Session session = cluster.connect()) {

            MappingManager mappingManager = new MappingManager(session);
            Mapper<StorageDataKey> m = mappingManager.mapper(StorageDataKey.class);

            List<StorageDataKey> result = m.map(session.execute(SELECT_QUERY)).all();

            assertEquals(200, result.size());

        }
    }

}
