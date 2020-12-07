package testconainers.dockerfile;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import testconainers.cassandra.CassandraManager;
import testconainers.config.ConnectionCfg;
import testconainers.object.StorageDataKey;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static testconainers.constant.TestContainerConstant.*;

@Testcontainers
public class DockerFileCassandraTest {

    @Container
    public static final GenericContainer<?> cassandra =
            new GenericContainer<>(new ImageFromDockerfile()
                    .withFileFromClasspath("Dockerfile", "dockerfile/cassandra/Dockerfile")
                    .withFileFromClasspath("entrypoint-wrap.sh","dockerfile/cassandra/entrypoint-wrap.sh"))
                    .withExposedPorts(9042);


    @BeforeEach
    public void cassandraSetup() {
        ConnectionCfg conf = ConnectionCfg.builder()
                .username("matthew")
                .password("1234")
                .seeds(cassandra.getHost())
                .port(cassandra.getFirstMappedPort())
                .keyspace("test")
                .build();

        CassandraManager.DB.connect(conf);

        PreparedStatement preparedOfInsert = CassandraManager.DB.getSession().prepare(INSERT_PREPARED_STATEMENT);

        IntStream.range(1, 101)
                .forEach(i ->
                        CassandraManager.DB.getSession().execute(preparedOfInsert.bind("storage_data_key_" + i,
                                LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                );
    }

    @Test
    public void test() {

            PreparedStatement prepared = CassandraManager.DB.getSession().prepare(INSERT_PREPARED_STATEMENT);

            IntStream.range(1, 101)
                    .forEach(i ->
                            CassandraManager.DB.getSession().execute(prepared.bind("storage_data_key_" + i,
                                    LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                    );

            IntStream.range(1, 101)
                    .forEach(i ->
                            CassandraManager.DB.getSession().execute(prepared.bind("storage_garbage_key_" + i,
                                    LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                    );

            MappingManager mappingManager = new MappingManager(CassandraManager.DB.getSession());
            Mapper<StorageDataKey> m = mappingManager.mapper(StorageDataKey.class);

            List<StorageDataKey> result = m.map(CassandraManager.DB.getSession().execute(SELECT_QUERY)).all();

            assertEquals(200, result.size());

    }

}
