package testconainers;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Testcontainers
public class LocalStackS3Test {

    @Container
    private final LocalStackContainer container = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.11.3"))
            .withServices(LocalStackContainer.Service.S3);

    private final String GARBAGE_DATA = "GARBAGE_DATA";

    private final String NORMAL_DATA = "NORMAL_DATA";

    @Test
    void test() throws Exception {
        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(container.getEndpointConfiguration(LocalStackContainer.Service.S3))
                .withCredentials(container.getDefaultCredentialsProvider())
                .build();

        String bucketName = "test";
        s3.createBucket(bucketName);
        log.info("bucket created.. bucketName={}", bucketName);

        IntStream.range(1,51)
                .forEach(i ->
                        s3.putObject(bucketName, ));

        String content = "";
        String key = "s3-key";
        s3.putObject(bucketName, key, content);
        log.info("파일을 업로드하였습니다. bucketName={}, key={}, content={}", bucketName, key, content);

        List<String> results = IOUtils.readLines(s3.getObject(bucketName, key).getObjectContent(), "utf-8");
        log.info("파일을 가져왔습니다. bucketName={}, key={}, results={}", bucketName, key, results);

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(content);
    }


}
