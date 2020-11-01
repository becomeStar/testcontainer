package testconainers.singleton;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static testconainers.constant.TestContainerConstant.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LocalStackS3Test extends AbstractContainerBaseTest{

    private AmazonS3 amazonS3;

    @BeforeAll
    void testDataSetup() {
        amazonS3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(LOCAL_STACK_CONTAINER.getEndpointConfiguration(LocalStackContainer.Service.S3))
                .withCredentials(LOCAL_STACK_CONTAINER.getDefaultCredentialsProvider())
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
    void confirmAllContainerIsRunning() {
        assertTrue(LOCAL_STACK_CONTAINER.isRunning());
        assertTrue(CASSANDRA_CONTAINER.isRunning());
    }


    @Test
    void s3GarbageDeleteTest()  {
        ListObjectsV2Request req = new ListObjectsV2Request()
                .withBucketName(BUCKET_NAME)
                .withMaxKeys(10)
                .withEncodingType("url");

        ListObjectsV2Result result;

        int successfulDeleteGarbage = 0;

        do {
            result = amazonS3.listObjectsV2(req);

            List<DeleteObjectsRequest.KeyVersion> garbageKeyList = result.getObjectSummaries()
                    .stream()
                    .filter(s3ObjectSummary ->
                            s3ObjectSummary.getKey().startsWith("storage_garbage_key")
                    )
                    .map(s3ObjectSummary ->
                            new DeleteObjectsRequest.KeyVersion(s3ObjectSummary.getKey()))
                    .collect(Collectors.toList());

            if(garbageKeyList.size() > 0) {

                DeleteObjectsRequest multiObjectDeleteRequest = new DeleteObjectsRequest(BUCKET_NAME)
                        .withKeys(garbageKeyList)
                        .withQuiet(false);

                DeleteObjectsResult delObjRes = amazonS3.deleteObjects(multiObjectDeleteRequest);

                successfulDeleteGarbage += delObjRes.getDeletedObjects().size();
            }
            String token = result.getNextContinuationToken();
            req.setContinuationToken(token);

        } while (result.isTruncated());

        assertEquals(100, successfulDeleteGarbage);
    }


}
