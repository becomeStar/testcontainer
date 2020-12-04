# TestContaiers



- 테스트 코드 내에서 컨테이너를 생성하고 제어하는 기능을 제공하는 자바 라이브러리

  - 테스트 코드 실행 시, 테스트 코드와 연동되는 다양한 모듈들(cassandra, mysql, kafka, local stack 등)을 컨테이너로 자동으로 실행

- 테스트 코드와 연동되는 외부 의존 모듈을 사용할 때 발생하는 문제점

  - 테스트용 데이터를 insert 할때, DB에 이미 많은 데이터가 존재한다면 insert 속도가 느려지는 문제가 있을 수 있다
  - 기존 운영중인 DB와 연동해서 테스트를 한다면 데이터 충돌 문제가 있을 수 있다
  - 터널링

  

> 외부 의존 모듈을 격리시켜  clean state 에서 테스트를 시작할 수 있다
>
> 로컬이나 CI machine 에서 외부 의존 모듈이 설치되었는지 신경쓰지 않아도 된다(터널링 불필요)
>
> port randomisation 을 통해 현재 사용되지 않는 포트를 자동으로 찾아 외부 의존 모듈을 컨테이너로 띄울 수 있다



- ## 기본 사용법(JUnit5)

  - ### pom.xml

    - ```
      <dependency>
       <groupId>org.testcontainers</groupId>
       <artifactId>junit-jupiter</artifactId>
       <version>${testcontainers.version}</version>
       <scope>test</scope>
      </dependency>
      ```

  - ### GenericContainer를 통해 컨테이너를 실행시키기

    
    - ```
      @Testcontainers
      public class JedisTest {
      
          @Container
          public GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:5.0.3-alpine"))
                  .withExposedPorts(6379);
      
          private JedisPool jedisPool;
      
          @BeforeEach
          public void setUp() {
              jedisPool = new JedisPool(new JedisPoolConfig(), "localhost", redis.getFirstMappedPort());
          }
      
          @AfterEach
          public void afterTest() {
              System.out.println("after test");
              System.out.println(redis.getLogs());
          }
          
          ...
      ```

    - @Container : instance field에 사용하면 모든 테스트 메소드마다 컨테이너를 재시작하고

      (start -> stop -> remove) , static field에 사용하면 클래스 내부 모든 테스트에서 동일한 컨테이너를 재사용

    - @Testcontainers : 테스트 클래스에 @Container를 사용한 필드를 찾아서 컨테이너 라이프사이클 관련 메소드를 실행해주는 역할

    - 테스트 클래스에 있는 테스트가 실행될때 (지정된 이미지가 로컬에 없다면 docker hub로부터 이미지를 pull 받은은 다음) 컨테이너가 실행되고 테스트가 끝나면 컨테이너가 **중지(stop)되고 삭제(remove)**된다 

      - Ryuk Container -> 테스트가 끝난 뒤 테스트를 위해 실행되었던 컨테이너를 중지하고 삭제시키는 역할을   하는 컨테이너 (https://github.com/testcontainers/moby-ryuk) 

        

  - ### Specialized Containers 를 통해 컨테이너를 실행시키기

    

    - CassandraContainer

      - ```
        @Testcontainers
        public class CassandraTest {
        
            @Container
            public static final CassandraContainer<?> cassandra =
                    new CassandraContainer<>("cassandra:3.11.2");
                    
            ...        
        ```

    - LocalStackContainer

      - ```
        @Testcontainers
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        public class LocalStackS3Test {
        
            @Container
            private static final LocalStackContainer container = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.11.3"))
                    .withServices(LocalStackContainer.Service.S3);
        ```

    

  - ### 여러 클래스에서 컨테이너를 공유하기

    

    - abstract 클래스에서 컨테이너를 실행시키고 컨테이너 공유가 필요한 클래스에서 extends 해서 사용

      - ```
        public abstract class AbstractContainerBaseTest {
        
            static final CassandraContainer<?> CASSANDRA_CONTAINER;
        
            static final LocalStackContainer LOCAL_STACK_CONTAINER;
        
            static {
                CASSANDRA_CONTAINER = new CassandraContainer<>("cassandra:3.11.2");
                CASSANDRA_CONTAINER.start();
                LOCAL_STACK_CONTAINER = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.11.3"))
                        .withServices(LocalStackContainer.Service.S3);
                LOCAL_STACK_CONTAINER.start();
            }
        
        }
        ```

      - ```
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        public class CassandraTest extends AbstractContainerBaseTest{
        	...
        	
        	@AfterAll
            void confirmContainerIsRunning() {
                assertTrue(LOCAL_STACK_CONTAINER.isRunning());
                assertTrue(CASSANDRA_CONTAINER.isRunning());
            }
        
        }
        ```

  - ### docker compose로 여러개의 컨테이너 실행시키기

    

    - docker-compose.yml

      - ```
        localstack:
          image: localstack/localstack:0.11.3
        cassandra:
          image: cassandra:3.11.2
        ```

    - DockerComposeContainer

      - ```
        public abstract class DockerComposeBase {
        
            static final DockerComposeContainer<?> DOCKER_COMPOSE_CONTAINER;
        
            static final int LOCALSTACK_S3_PORT = 4572;
        
            static final int CASSANDRA_PORT = 9042;
        
            static {
                DOCKER_COMPOSE_CONTAINER =
                        new DockerComposeContainer<>(
                                new File("src/test/resources/docker-compose.yml"))
                                .withExposedService("localstack_1", LOCALSTACK_S3_PORT)
                                .withExposedService("cassandra_1", CASSANDRA_PORT,
                                        Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));
        
                DOCKER_COMPOSE_CONTAINER.start();
            }
        }
        ```

    

  - ### 테스트를 수행시킬 때마다 컨테이너 재사용하기
  
    - 컨테이너를 재사용하는 방법
      1. 설정 파일에서 reuse 프로퍼티 추가 (testcontainers.reuse.enable=true)
          - Linux : /home/myuser/.testcontainers.properties
          - Windows : C:/Users/myuser/.testcontainers.properties
          - macOS : /Users/myuser/.testcontainers.properties   
      2. reusability flag = true로 설정 (withReuse(true))
    
    - 컨테이너가 
    
    - GenericContainer lifecycle method 재정의
        - containerIsCreated(String)
        - **containerIsStarting(InspectContainerResponse, boolean)**
        - **containerIsStarted(InspectContainerResponse, boolean)**
        - containerIsStopping(InspectContainerResponse)
        - containerIsStopped(InspectContainerResponse)
          
            ```
          public class CassandraContainerWrapper extends CassandraContainer {
             
                 public CassandraContainerWrapper(String confluentPlatformVersion) {
                     super(confluentPlatformVersion);
                 }
             
                 @Override
                 protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
                     if (!reused) {
                         Cluster cluster = this.getCluster();
                         try (Session session = cluster.connect()) {
                             session.execute(KEYSPACE_QUERY);
                             session.execute(TABLE_CREATE_QUERY);
                             PreparedStatement prepared = session.prepare(INSERT_PREPARED_STATEMENT);
             
                             IntStream.range(1, 101)
                                     .forEach(i ->
                                             session.execute(prepared.bind("storage_data_key_" + i,
                                                     LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                                     );
                         }
                     } 
                 }
             }
          ```
          
    - 테스트 컨테이너에 label을 붙여서 버전 관리
       ```
      public abstract class CasssandraBase {
      
          static final CassandraContainerWrapper CASSANDRA_CONTAINER;
      
          static {
              CASSANDRA_CONTAINER = (CassandraContainerWrapper) new CassandraContainerWrapper("cassandra:3.11.2")
                      .withReuse(true)
                      .withLabel("reuse.image.name", "reuse-test-version-1");
      
              CASSANDRA_CONTAINER.start();
          }
      
      }
      ```
        
        
