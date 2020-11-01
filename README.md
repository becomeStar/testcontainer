# TestContaiers



- 테스트 코드 내에서 컨테이너를 생성하고 제어하는 기능을 제공하는 자바 라이브러리

  - 테스트 코드 실행 시, 테스트 코드와 연동되는 다양한 모듈들(testconainers.cassandra, mysql, kafka, local stack 등)을 컨테이너로 자동으로 실행

- 테스트 코드와 연동되는 외부 의존 모듈을 사용할 때 발생하는 문제점

  - 테스트용 데이터를 insert 할때, DB에 이미 많은 데이터가 존재한다면 insert 속도가 느려지는 문제가 있을 수 있다
  - 기존 운영중인 DB와 연동해서 테스트를 한다면 데이터 충돌 문제가 있을 수 있다

  

> 외부 의존 모듈을 격리시켜  clean state 에서 테스트를 시작할 수 있다
>
> 로컬이나 CI machine 에서 외부 의존 모듈이 설치되었는지 신경쓰지 않아도 된다(터널링 불필요)
>
> port randomisation 을 통해 현재 사용되지 않는 포트를 자동으로 찾아 외부 의존 모듈을 컨테이너로 띄울 수 있다



- JUnit4, JUnit5, Spock 테스트 프레임워크와 통합

- 기본 사용법(JUnit5)

  - testcontainers 의존성 설정(maven)

    - ```
      <dependency>
       <groupId>org.testcontainers</groupId>
       <artifactId>junit-jupiter</artifactId>
       <version>${testcontainers.version}</version>
       <scope>test</scope>
      </dependency>
      ```

  - GenericContainer를 통한 redis 연동

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

      -  Ryuk Container -> 테스트가 끝난 뒤 테스트를 위해 실행되었던 컨테이너를 중지하고 삭제시키는 역할을   하는 컨테이너 (https://github.com/testcontainers/moby-ryuk) 

  - GenericContainer 구현체를 통해 컨테이너를 실행시키는 방법

    - Cassandra(CassandraContainer) , LocalStack(LocalStackContainer) 

