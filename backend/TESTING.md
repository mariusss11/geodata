# Backend Testing Guide

This guide covers unit testing, integration testing, and test automation for the Spring Boot microservices.

## Quick Start

### Run All Tests

```bash
cd backend
./mvnw clean test
```

### Run Tests for One Service

```bash
./mvnw -pl identity-service test
./mvnw -pl map-service test
./mvnw -pl borrow-service test
```

### Run Integration Tests

```bash
./mvnw clean verify
```

### Run Tests with Coverage

```bash
./mvnw clean verify -P coverage
# View report: open target/site/jacoco/index.html
```

## Test Structure

```
identity-service/src/test/java/com/geodata/
├── controller/          # Controller tests (REST endpoints)
├── service/             # Service layer tests
├── repository/          # Repository tests (DB layer)
├── integration/         # Integration tests
└── config/              # Test configuration
```

## Types of Tests

### 1. Unit Tests

Test individual classes in isolation.

**Example:** Testing a service method

```java
@SpringBootTest
@ActiveProfiles("test")
public class AuthServiceTest {
  
  @Mock
  private UserRepository userRepository;
  
  private AuthService authService;
  
  @BeforeEach
  public void setup() {
    authService = new AuthService(userRepository);
  }
  
  @Test
  public void testLoginSuccessful() {
    // Given
    User user = new User(...);
    when(userRepository.findByEmail("test@example.com"))
      .thenReturn(Optional.of(user));
    
    // When
    User result = authService.authenticate("test@example.com", "password");
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo("test@example.com");
  }
}
```

### 2. Integration Tests

Test multiple components working together, including the database.

```java
@SpringBootTest
@ActiveProfiles("test")
public class BorrowIntegrationTest {
  
  @Autowired
  private BorrowService borrowService;
  
  @Autowired
  private BorrowRepository borrowRepository;
  
  @Test
  @Transactional
  public void testBorrowWorkflow() {
    // Setup: Create test data
    Map map = createTestMap();
    User user = createTestUser();
    
    // Borrow a map
    Borrows borrow = borrowService.borrowMap(map.getId(), user.getId());
    
    // Verify persisted
    Optional<Borrows> saved = borrowRepository.findById(borrow.getId());
    assertThat(saved).isPresent();
    assertThat(saved.get().getStatus()).isEqualTo(BorrowsStatus.ACTIVE);
  }
}
```

### 3. Controller Tests

Test REST endpoints and HTTP responses.

```java
@SpringBootTest
@AutoConfigureMockMvc
public class MapControllerTest {
  
  @Autowired
  private MockMvc mockMvc;
  
  @Test
  public void testGetMapsEndpoint() throws Exception {
    mockMvc.perform(get("/api/maps")
      .header("Authorization", "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].title").exists());
  }
}
```

## Running Tests in GitHub Actions

Tests automatically run on:

- ✅ Every push to `main` or `develop`
- ✅ Every pull request to `main` or `develop`
- ✅ Before building Docker images

**View test results:** Check GitHub Actions tab → Backend Tests → Test Results

## Test Profiles

### Development Profile (`application-test.yml`)

- Uses in-memory H2 database (no external DB needed)
- Disables migrations
- Reduced logging
- Fast execution

```yaml
# In application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false
```

### Running Tests with Database

```bash
# Requires PostgreSQL running
./mvnw test -P postgres
```

## Test Coverage

### Generate Coverage Report

```bash
./mvnw clean verify jacoco:report

# View report
open identity-service/target/site/jacoco/index.html
```

### Coverage Goals

| Module  | Target |
|---------|--------|
| Services | 80%+  |
| Controllers | 75%+ |
| Repositories | 70%+ |

### Increase Coverage

```bash
# Show uncovered lines
./mvnw clean jacoco:report

# Check coverage in CI
./mvnw clean verify sonar:sonar \
  -Dsonar.projectKey=geodata \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin
```

## Testing Best Practices

### 1. Test Naming Convention

```java
// Good: describes what is being tested and expected outcome
public void testBorrowMapWhenMapNotAvailableThrowsException()
public void testLoginSuccessfulReturnsUser()
public void testFindMapsByRadiusReturnsNearbyMaps()

// Bad
public void test1()
public void testBorrow()
```

### 2. Use AAA Pattern

```java
@Test
public void testTransferMap() {
  // Arrange
  Map map = new Map("test", true);
  User from = new User("user1");
  User to = new User("user2");
  
  // Act
  Borrows transfer = borrowService.transferMap(map, from, to);
  
  // Assert
  assertThat(transfer.getStatus()).isEqualTo(BorrowsStatus.ACTIVE);
}
```

### 3. Mock External Dependencies

```java
@Mock
private RestTemplate restTemplate;

@Test
public void testCallsIdentityService() {
  when(restTemplate.postForObject(...)).thenReturn(...);
  
  // Test code
  
  verify(restTemplate).postForObject(...);
}
```

### 4. Use Fixtures & Factory Methods

```java
private User createTestUser(String email) {
  return User.builder()
    .email(email)
    .role("USER")
    .build();
}

@Test
public void testUserCreation() {
  User user = createTestUser("test@example.com");
  // Use user...
}
```

### 5. Test Exception Handling

```java
@Test
public void testMapNotFoundThrowsException() {
  // Should throw MapNotFoundException
  assertThrows(MapNotFoundException.class, () -> {
    mapService.getMapById("nonexistent");
  });
}
```

## Running Tests Locally

### Before Committing

```bash
# Run tests locally
cd backend
./mvnw clean test

# Run full verification (includes integration tests)
./mvnw clean verify
```

### Watch Mode (Auto-run on file change)

```bash
./mvnw clean test -Dwatch=true
```

### Skip Tests (Only for emergencies)

```bash
./mvnw clean install -DskipTests  # ⚠️ Not recommended
```

## Debugging Tests

### Run Single Test

```bash
./mvnw -Dtest=AuthServiceTest test
./mvnw -Dtest=AuthServiceTest#testLoginSuccessful test
```

### Debug with IDE

1. Set breakpoint in test code
2. Run as "Debug" (not "Run")
3. Step through execution

### Print Debugging

```java
System.out.println("Debug value: " + variable);
logger.debug("Debug value: {}", variable);
```

### Check Test Output

```bash
./mvnw test -X  # Extra verbose
```

## Common Issues

### Test hangs or times out

```java
@Test(timeout = 5000)  // 5 second timeout
public void testSomething() {
  // Test code
}
```

### Database state issues

```java
@Test
@Transactional  // Automatic rollback after test
public void testWithDatabase() {
  // Test code
}
```

### Mock not working

```java
@ExtendWith(MockitoExtension.class)  // Required for @Mock
public class MyTest {
  @Mock
  private SomeService service;
}
```

## Continuous Integration

### GitHub Actions Workflow

File: `.github/workflows/backend-test.yml`

Automatically runs:
- `mvn clean compile` (static analysis)
- `mvn test` (unit tests)
- `mvn verify` (integration tests)
- Coverage reports uploaded to Codecov

### Merge Requirements

- ✅ All tests must pass
- ✅ No test flakiness
- ✅ Code coverage maintained

### Failed Tests in CI

1. View failure in GitHub Actions
2. Reproduce locally: `./mvnw test`
3. Debug and fix
4. Commit fix with test name in message: `fix: resolve flaky BorrowServiceTest#testTransfer`

## Performance

### Optimize Test Execution

```bash
# Run tests in parallel
./mvnw test -T 1C  # 1 thread per core

# Skip expensive tests during development
./mvnw test -Dgroups=!integration
```

### Profile Tests

```bash
./mvnw test -Dlogging.level.com.geodata=DEBUG
```

## Test Statistics

Monitor these metrics:

```bash
# Count tests
find . -name "*Test.java" | wc -l

# Average test duration
./mvnw surefire-report:report
```

---

For more information:
- [Spring Boot Testing Docs](https://spring.io/guides/gs/testing-web/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
