package testconainers.compose;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import testconainers.cassandra.CassandraManager;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import testconainers.config.ConnectionCfg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import testconainers.object.StorageDataKey;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static com.amazonaws.auth.profile.internal.ProfileKeyConstants.REGION;
import static testconainers.constant.TestContainerConstant.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DockerComposeTest extends DockerComposeBase {

    private AmazonS3 amazonS3;

    @BeforeAll
    public void setUp() {
        s3Setup();
        cassandraSetup();
    }

    public void cassandraSetup() {
        ConnectionCfg conf = ConnectionCfg.builder()
                .username("matthew")
                .password("1234")
                .seeds(DOCKER_COMPOSE_CONTAINER.getServiceHost("cassandra_1", CASSANDRA_PORT))
                .port(DOCKER_COMPOSE_CONTAINER.getServicePort("cassandra_1", CASSANDRA_PORT))
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


    public void s3Setup() {

        amazonS3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        "http://" +
                                DOCKER_COMPOSE_CONTAINER.getServiceHost("localstack_1", LOCALSTACK_S3_PORT) +
                                ":" +
                                DOCKER_COMPOSE_CONTAINER.getServicePort("localstack_1", LOCALSTACK_S3_PORT),
                        REGION))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("foobar", "foobar")))
                .withPathStyleAccessEnabled(true)
                .build();

        amazonS3.createBucket(BUCKET_NAME);
        log.info("bucket created.. bucketName={}", BUCKET_NAME);

        IntStream.range(1, 101)
                .forEach(i ->
                        amazonS3.putObject(BUCKET_NAME, "storage_data_key_" + i, NORMAL_DATA + i));

        log.info("normal data uploaded..");


        IntStream.range(1, 101)
                .forEach(i ->
                        amazonS3.putObject(BUCKET_NAME, "storage_garbage_key_" + i, GARBAGE_DATA + i));

        log.info("garbage data uploaded..");
    }

    @AfterAll
    public void release() {
        CassandraManager.DB.shutdown();
    }

    @Test
    public void composeTest() {
        ListObjectsV2Request req = new ListObjectsV2Request()
                .withBucketName(BUCKET_NAME)
                .withMaxKeys(10)
                .withEncodingType("url");

        ListObjectsV2Result result;

        List<DeleteObjectsRequest.KeyVersion> garbageList = Lists.newArrayList();
        int successfulDeleteGarbage = 0;


        MappingManager mappingManager = new MappingManager(CassandraManager.DB.getSession());
        Mapper<StorageDataKey> m = mappingManager.mapper(StorageDataKey.class);

        PreparedStatement preparedOfSelect = CassandraManager.DB.getSession().prepare(SELECT_PREPARED_STATEMENT);

        do {
            result = amazonS3.listObjectsV2(req);

            for (S3ObjectSummary s3ObjectSummary : result.getObjectSummaries()) {

                StorageDataKey storageDataKey =
                        m.map(CassandraManager.DB.getSession().execute(preparedOfSelect.bind(s3ObjectSummary.getKey()))).one();

                if (storageDataKey == null) {
                    garbageList.add(new DeleteObjectsRequest.KeyVersion(s3ObjectSummary.getKey()));
                }

            }

            String token = result.getNextContinuationToken();
            req.setContinuationToken(token);

        } while (result.isTruncated());

        if (garbageList.size() > 0) {

            DeleteObjectsRequest multiObjectDeleteRequest = new DeleteObjectsRequest(BUCKET_NAME)
                    .withKeys(garbageList)
                    .withQuiet(false);

            DeleteObjectsResult delObjRes = amazonS3.deleteObjects(multiObjectDeleteRequest);

            successfulDeleteGarbage += delObjRes.getDeletedObjects().size();
        }


        assertEquals(100, successfulDeleteGarbage);


    }
}