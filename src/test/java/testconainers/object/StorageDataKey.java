package testconainers.object;



@Table(keyspace = "test", name = "item")
public class StorageDataKey {

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "create_time")
    private Long createTime;
}
