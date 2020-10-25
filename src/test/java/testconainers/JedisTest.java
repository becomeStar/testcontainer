package testconainers;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JedisTest {

    @Container
    public static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine"))
            .withExposedPorts(6379);

    private JedisPool jedisPool;

    @BeforeAll
    public void setUp() {
        System.out.println("port : " + redis.getFirstMappedPort());
        jedisPool = new JedisPool(new JedisPoolConfig(), "localhost", redis.getFirstMappedPort());
    }

    @AfterAll
    public void afterTest() {
        System.out.println("start container logs");
        System.out.println(redis.getLogs());
        System.out.println("end container logs");
    }

    private ArrayList<String> messageContainer = new ArrayList<>();

    private CountDownLatch messageReceivedLatch = new CountDownLatch(1);
    private CountDownLatch publishLatch = new CountDownLatch(1);

    @Test
    public void run() throws InterruptedException {
        setupPublisher();
        JedisPubSub jedisPubSub = setupSubscriber();

        // publish away!
        publishLatch.countDown();

        messageReceivedLatch.await();
        log("Got message: %s", messageContainer.iterator().next());

        jedisPubSub.unsubscribe();

    }

    private void setupPublisher() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    log("Connecting");
                    log("Waiting to publish");
                    publishLatch.await();
                    log("Ready to publish, waiting one sec");
                    Thread.sleep(1000);
                    log("publishing");
                    jedisPool.getResource().publish("test", "This is a message");
                } catch (Exception e) {
                    log(">>> OH NOES Pub, " + e.getMessage());
                    // e.printStackTrace();
                }
            }
        }, "publisherThread").start();
    }

    private JedisPubSub setupSubscriber() {
        final JedisPubSub jedisPubSub = new JedisPubSub() {
            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                log("onUnsubscribe");
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                log("onSubscribe");
            }

            @Override
            public void onPUnsubscribe(String pattern, int subscribedChannels) {
            }

            @Override
            public void onPSubscribe(String pattern, int subscribedChannels) {
            }

            @Override
            public void onPMessage(String pattern, String channel, String message) {
            }

            @Override
            public void onMessage(String channel, String message) {
                messageContainer.add(message);
                log("Message received");
                messageReceivedLatch.countDown();
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    log("Connecting");
                    log("subscribing");
                    jedisPool.getResource().subscribe(jedisPubSub, "test");
                    log("subscribe returned, closing down");
                } catch (Exception e) {
                    log(">>> OH NOES Sub - " + e.getMessage());
                    // e.printStackTrace();
                }
            }
        }, "subscriberThread").start();
        return jedisPubSub;
    }

    static final long startMillis = System.currentTimeMillis();

    private static void log(String string, Object... args) {
        long millisSinceStart = System.currentTimeMillis() - startMillis;
        System.out.printf("%20s %6d %s\n", Thread.currentThread().getName(), millisSinceStart,
                String.format(string, args));
    }

}
