package testconainers;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;
import testconainers.cache.Cache;
import testconainers.cache.RedisBackedCache;

import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RedisBackedCacheIntTest {

    @Rule
    public GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:5.0.3-alpine"))
            .withExposedPorts(6379);

    private Cache cache;

    @Before
    public void setUp() {
        Jedis jedis = new Jedis(redis.getContainerIpAddress(), redis.getMappedPort(6379));
        cache = new RedisBackedCache(jedis, "test");
    }

    @Test
    public void testFindingAnInsertedValue() {
        cache.put("foo", "FOO");
        Optional<String> foundObject = cache.get("foo", String.class);

        assertTrue("When an object in the cache is retrieved, it can be found",
                foundObject.isPresent());
        assertEquals("When we put a String in to the cache and retrieve it, the value is the same",
                "FOO",
                foundObject.get());
    }

    @Test
    public void testNotFindingAValueThatWasNotInserted() {
        Optional<String> foundObject = cache.get("bar", String.class);

        assertFalse("When an object that's not in the cache is retrieved, nothing is found",
                foundObject.isPresent());
    }

}
