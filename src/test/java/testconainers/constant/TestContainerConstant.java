package testconainers.constant;

public class TestContainerConstant {

    public static final String KEYSPACE_QUERY = "CREATE KEYSPACE IF NOT EXISTS test WITH replication = \n" +
            "{'class':'SimpleStrategy','replication_factor':'1'};";

    public static final String TABLE_CREATE_QUERY = "CREATE TABLE IF NOT EXISTS test.item(storage_key text PRIMARY KEY, "
            + "create_time bigint );";

    public static final String INSERT_PREPARED_STATEMENT = "INSERT into test.item (storage_key, create_time) VALUES "
            + "(?, ?);";

    public static final String SELECT_QUERY = "SELECT storage_key, create_time FROM test.item;";

    public static final String SELECT_PREPARED_STATEMENT = "SELECT storage_key, create_time FROM test.item WHERE storage_key = ? ";

    public static final String BUCKET_NAME = "test";

    public static final String NORMAL_DATA = "NORMAL_DATA_";

    public static final String GARBAGE_DATA = "GARBAGE_DATA_";
}
