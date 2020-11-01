package testconainers.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;
import testconainers.config.ConnectionCfg;
import testconainers.constant.TestContainerConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public enum CassandraManager {

    DB;

    private Session session;
    private Cluster cluster;
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraManager.class);

    /**
     * Connect to the testconainers.cassandra database based on the connection configuration provided.
     * Multiple call to this method will have no effects if a connection is already established
     * @param conf the configuration for the connection
     */
    public void connect(ConnectionCfg conf) {
        if (cluster == null && session == null) {
            cluster = Cluster.builder().withPort(conf.getPort()).withCredentials(conf.getUsername(), conf.getPassword()).addContactPoints(conf.getSeeds()).build();
            session = cluster.connect();
            session.execute(TestContainerConstant.KEYSPACE_QUERY);
            session.execute(TestContainerConstant.TABLE_CREATE_QUERY);
        }
        Metadata metadata = cluster.getMetadata();
        LOGGER.info("Connected to cluster: " + metadata.getClusterName() + " with partitioner: " + metadata.getPartitioner());
        metadata.getAllHosts().stream().forEach((host) -> {
            LOGGER.info("Cassandra datacenter: " + host.getDatacenter() + " | address: " + host.getAddress() + " | rack: " + host.getRack());
        });
    }

    /**
     * Invalidate and close the session and connection to the testconainers.cassandra database
     */
    public void shutdown() {
        LOGGER.info("Shutting down the whole testconainers.cassandra cluster");
        if (null != session) {
            session.close();
        }
        if (null != cluster) {
            cluster.close();
        }
    }

    public Session getSession() {
        if (session == null) {
            throw new IllegalStateException("No connection initialized");
        }
        return session;
    }
}
