# TestContainers



- 테스트 코드 내에서 컨테이너를 생성하고 제어하는 기능을 제공하는 자바 라이브러리

  - 테스트 코드 실행 시, 테스트 코드와 연동되는 다양한 모듈들(cassandra, mysql, kafka, local stack 등)을 컨테이너로 자동으로 실행
  - 별도의 환경 구축이 필요없다

- 테스트 코드와 연동되는 외부 의존 모듈을 사용할 때 발생하는 문제점

  - 터널링이 필요한 경우가 있다
  - 테스트용 데이터를 insert 할때, DB에 이미 많은 데이터가 존재한다면 insert 속도가 느려지는 문제가 있을 수 있다
  - 기존 운영중인 DB와 연동해서 테스트를 한다면 데이터 충돌 문제가 있을 수 있다

  

> 외부 의존 모듈을 격리시켜  clean state 에서 테스트를 시작할 수 있다
>
> 로컬이나 CI machine 에서 외부 의존 모듈이 설치되었는지 신경쓰지 않아도 된다(터널링 불필요)
>
> port randomisation 을 통해 현재 사용되지 않는 포트를 자동으로 찾아 외부 의존 모듈을 컨테이너로 띄울 수 있다



- junit4, junit5, spock 테스트 프레임워크에서 사용가능

  

## 기본 사용법 (JUnit5)

- ### pom.xml

  - ```
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>${testcontainers.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>${testcontainers.version}</version>
        <scope>test</scope>
    </dependency>
    ```

- ### GenericContainer

  

  - ```
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
        
        ...
    ```

  - @Container : instance field에 사용하면 모든 테스트 메소드마다 컨테이너를 재시작하고

    (start -> stop -> remove) , static field에 사용하면 클래스 내부 모든 테스트에서 동일한 컨테이너를 재사용

  - @Testcontainers : 테스트 클래스에 @Container를 사용한 필드를 찾아서 컨테이너 라이프사이클 관련 메소드를 실행해주는 역할

  - 테스트 클래스에 있는 테스트가 실행될때 (지정된 이미지가 로컬에 없다면 docker hub로부터 이미지를 pull 받은 다음) 컨테이너가 실행되고 테스트가 끝나면 컨테이너가 **중지(stop)되고 삭제(remove)**된다 

    - Ryuk Container -> 테스트가 끝난 뒤 테스트를 위해 실행되었던 컨테이너를 중지하고 삭제시키는 역할을  하는 컨테이너 (https://github.com/testcontainers/moby-ryuk) 

  - dockerfile로 직접 이미지 생성 후 컨테이너 실행도 가능

    - ```
      @Testcontainers
      public class DockerFileCassandraTest {
      
          @Container
          public static final GenericContainer<?> cassandra =
                  new GenericContainer<>(new ImageFromDockerfile()
                          .withFileFromClasspath("Dockerfile", "dockerfile/cassandra/Dockerfile")
                          .withFileFromClasspath("entrypoint-wrap.sh","dockerfile/cassandra/entrypoint-wrap.sh"))
                          .withExposedPorts(9042);
      ```

      

- ### Specialized Containers

  - GenericContainer 를 상속하고 특정한 모듈에 커스터마이징 된 컨테이너

  - CassandraContainer

    - ```
      @Testcontainers
      public class CassandraTest {
      
          @Container
          public static final CassandraContainer<?> cassandra =
                  new CassandraContainer<>("cassandra:3.11.2");
                  
          ...        
      ```

    - ```
      <dependency>
          <groupId>org.testcontainers</groupId>
          <artifactId>cassandra</artifactId>
          <version>${testcontainers.version}</version>
          <scope>test</scope>
      </dependency>
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

    - ```
      <dependency>
          <groupId>org.testcontainers</groupId>
          <artifactId>localstack</artifactId>
          <version>${testcontainers.version}</version>
          <scope>test</scope>
      </dependency>
      ```

  

## 여러 클래스에서 컨테이너를 공유



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

    

### docker compose로 여러개의 컨테이너를 묶음으로 실행시키기



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

- 실행시킨 컨테이너의 host와 port 를 얻어내기

  - ```
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
    ```

    

## 테스트를 실행할 때마다 컨테이너 재사용



- 컨테이너를 재사용하는 방법 

  1. 설정 파일에서 reuse 프로퍼티를 추가 (testcontainers.reuse.enable=true)

     - Linux : /home/myuser/.testcontainers.properties
     - Windows : C:/Users/myuser/.testcontainers.properties
     - macOS :  /Users/myuser/.testcontainers.properties  

     ```
     - Linux : /home/myuser/.testcontainers.properties
     - Windows : C:/Users/myuser/.testcontainers.properties
     - macOS : /Users/myuser/.testcontainers.properties   
     ```

  2.  reusability flag = true로 설정 (withReuse(true))

     

- 컨테이너 처음 생성시에만 테스트 데이터 초기화 동작을 수행

  -  GenericContainer lifecycle method 재정의

    - containerIsCreated(String)
    - **containerIsStarting(InspectContainerResponse, boolean)**
    - **containerIsStarted(InspectContainerResponse, boolean)**
    - containerIsStopping(InspectContainerResponse)
    - containerIsStopped(InspectContainerResponse)

  - ```
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

  - 컨테이너에 label을 붙여 구분

    - 여러 프로젝트에서 동일한 모듈을 사용할 경우 충돌을 방지

    - ```
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

  

## circle ci 에서의 실행



- testcontainer가 Docker-in-Docker 환경에서 수행되는 것을 지원하지 않음

- executor type 을 docker가 아닌 machine으로 설정해야 함 (Linux virtual machine에서 테스트 수행)

- .circleci/config.yml

  - ```
    # Check https://circleci.com/docs/2.0/language-java/ for more details
    #
    version: 2
    machine: true
    jobs:
      build:
        steps:
          - checkout
    
          - run: mvn -B clean install
    ```

### 참고 문서

- https://www.testcontainers.org/ (공식 홈페이지)
- https://github.com/testcontainers (testcontainer github)
- https://pawelpluta.com/optimise-testcontainers-for-better-tests-performance/ (testcontainer 재사용)
- https://callistaenterprise.se/blogg/teknik/2020/10/09/speed-up-your-testcontainers-tests/ (testcontainer 재사용)
- https://www.slideshare.net/rich.north/testcontainers-geekout-ee-2017-presentation 
- https://dzone.com/articles/easy-integration-testing-with-testcontainers
- https://woowabros.github.io/tools/2019/07/18/localstack-integration.html
- https://blog.geunho.dev/posts/container-based-test-env/#fn:1
- https://kin3303.tistory.com/16 (docker container lifecycle)