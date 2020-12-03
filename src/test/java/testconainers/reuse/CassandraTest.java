package testconainers.reuse;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import testconainers.object.StorageDataKey;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static testconainers.constant.TestContainerConstant.SELECT_QUERY;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CassandraTest extends CasssandraBase {

    @Test
    public void test() {
        Cluster cluster = CASSANDRA_CONTAINER.getCluster();
        try (Session session = cluster.connect()) {

            MappingManager mappingManager = new MappingManager(session);
            Mapper<StorageDataKey> m = mappingManager.mapper(StorageDataKey.class);

            List<StorageDataKey> result = m.map(session.execute(SELECT_QUERY)).all();

            assertEquals(result.size(), 100);

        }
    }

}
