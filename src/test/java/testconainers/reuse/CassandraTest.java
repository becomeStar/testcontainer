package testconainers.reuse;

import com.baeldung.testconainers.object.StorageDataKey;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CassandraTest extends CasssandraBase {

    private final String KEYSPACE_QUERY = "CREATE KEYSPACE IF NOT EXISTS test WITH replication = \n" +
            "{'class':'SimpleStrategy','replication_factor':'1'};";

    private final String TABLE_CREATE_QUERY = "CREATE TABLE test.item(storage_key text PRIMARY KEY, "
            + "create_time bigint );";

    private final String INSERT_PREPARED_STATEMENT = "INSERT into test.item (storage_key, create_time) VALUES "
            + "(?, ?);";

    private final String SELECT_QUERY = "SELECT storage_key, create_time FROM test.item;";

    @BeforeAll
    public void setup() {
        if(!CASSANDRA_CONTAINER.isCreated()) {
            Cluster cluster = CASSANDRA_CONTAINER.getCluster();
            try (Session session = cluster.connect()) {
                session.execute(KEYSPACE_QUERY);
                session.execute(TABLE_CREATE_QUERY);
            }
        }

    }


    @Test
    public void test() {
        Cluster cluster = CASSANDRA_CONTAINER.getCluster();
        try (Session session = cluster.connect()) {

            PreparedStatement prepared = session.prepare(INSERT_PREPARED_STATEMENT);

            IntStream.range(1, 101)
                    .forEach(i ->
                            session.execute(prepared.bind("storage_data_key_" + i,
                                    LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                    );

            MappingManager mappingManager = new MappingManager(session);
            Mapper<StorageDataKey> m = mappingManager.mapper(StorageDataKey.class);

            List<StorageDataKey> result = m.map(session.execute(SELECT_QUERY)).all();

            assertEquals(result.size(), 100);


        }
    }

}
