package testconainers;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static constant.TestContainerConstant.BUCKET_NAME;
import static constant.TestContainerConstant.NORMAL_DATA;
import static constant.TestContainerConstant.GARBAGE_DATA;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LocalStackS3Test {

    @Container
    private static final LocalStackContainer container = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.11.3"))
            .withServices(LocalStackContainer.Service.S3);

    private AmazonS3 amazonS3;


    @BeforeAll
    void testDataSetup() {
        amazonS3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(container.getEndpointConfiguration(LocalStackContainer.Service.S3))
                .withCredentials(container.getDefaultCredentialsProvider())
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
