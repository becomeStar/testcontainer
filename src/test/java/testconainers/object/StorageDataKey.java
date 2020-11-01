package testconainers.object;


import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "test", name = "item")
public class StorageDataKey {

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "create_time")
    private Long createTime;
}
