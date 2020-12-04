package testconainers.reuse.simple_reuse;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CassandraTest extends CasssandraBase {

    @Test
    public void test() {
        assertTrue(CASSANDRA_CONTAINER.isCreated());
    }

}
