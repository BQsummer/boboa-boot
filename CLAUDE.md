# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Boboa-boot is a Spring Boot 3.5.5 application using Java 17. It's a full-stack web service with JWT-based authentication, MyBatis Plus for database access, and multiple storage backend integrations (local, Aliyun OSS, AWS S3).

**Technology Stack:**
- Spring Boot 3.5.5 with Spring Security 6.5.3
- MyBatis Plus 3.5.14 for ORM
- MySQL database with Druid connection pool
- JWT (jjwt 0.12.3) for authentication
- RestAssured 5.5.6 for integration testing
- Lombok for boilerplate reduction
- Quartz for scheduled jobs
- OpenFeign for HTTP clients
- Logbook for HTTP request/response logging

## Build & Run Commands

### Build
```bash
mvn clean install
```

### Run application
```bash
mvn spring-boot:run
```

### Run tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserControllerTest

# Run specific test method
mvn test -Dtest=UserControllerTest#shouldReturnCurrentUserProfileWhenTokenValid
```

### Run with specific profile
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Architecture

### Package Structure

- `com.bqsummer.common` - DTOs and value objects
  - `common.dto.*` - Data transfer objects organized by domain (auth, character, config, feedback, im, invite, notify, point, recharge, wallet)
  - `common.vo.req.*` - Request value objects
  - `common.vo.resp.*` - Response value objects
- `com.bqsummer.configuration` - Spring configuration classes
- `com.bqsummer.constant` - Application constants
- `com.bqsummer.controller` - REST API controllers
- `com.bqsummer.framework` - Cross-cutting framework concerns
  - `framework.exception` - Exception handling
  - `framework.http` - HTTP filters (IP rate limiting)
  - `framework.security` - JWT authentication components
  - `framework.log` - Logging utilities
  - `framework.job` - Job scheduling utilities
- `com.bqsummer.mapper` - MyBatis Plus mappers
- `com.bqsummer.repository` - Repository pattern implementations
- `com.bqsummer.service` - Business logic layer
  - `service.auth` - Authentication services
  - `service.configplus` - Dynamic configuration system (see below)
  - `service.im` - Instant messaging features
  - `service.notify` - Notification services
  - `service.recharge` - Payment/recharge services
  - `service.storage` - File storage abstraction with multiple backends
- `com.bqsummer.util` - Utility classes
- `com.bqsummer.job` - Quartz job implementations

### Key Architectural Patterns

#### 1. JWT Authentication Flow
- `JwtAuthenticationFilter` (Order 2) validates Bearer tokens from Authorization header
- Filter checks token blacklist via `TokenBlacklistService`
- Filter validates user exists and is not disabled in database
- User ID and roles are extracted from JWT and stored in SecurityContext
- Anonymous endpoints are configured in `SecurityConfig.filterChain()`
- Protected endpoints use `@hasRole("USER")` or `@hasRole("ADMIN")`

#### 2. IP Rate Limiting
- `IpRateLimitFilter` (Order 1) runs before JWT authentication
- Uses Guava RateLimiter with Caffeine cache per-IP
- Configurable via `ip.rate.limit.*` properties in application.properties
- Supports IP whitelist via `Configs.getIpWhiteList()`
- Supports IP blacklist via `ip.rate.limit.blacklist` property
- Extracts real IP from X-Forwarded-For or X-Real-IP headers

#### 3. Dynamic Configuration System (ConfigPlus)
- Use `@SnorlaxScan` on the main application class to enable config scanning
- `@AppConfig` annotation marks interfaces as dynamic configuration proxies
- `@ConfigEle` annotation on interface methods maps them to database config entries
- `ConfigProxyFactory` creates CGLIB proxies with `ConfigMethodInterceptor`
- Configuration values are resolved at runtime from database via `ConfigService`
- This allows changing application behavior without redeployment

Example:
```java
@AppConfig(name = "my-feature")
public interface MyFeatureConfig {
    @ConfigEle(key = "feature.enabled")
    Boolean isEnabled();
}
```

#### 4. File Storage Abstraction
- `FileStorageService` interface abstracts storage backend (local, S3, OSS)
- Implementations:
  - `LocalFileStorageService` - filesystem storage
  - `S3FileStorageService` - AWS S3
  - `OssFileStorageService` - Aliyun OSS
  - `HybridLocalOssFileStorageService` - hybrid approach
- Supports pre-signed URLs, categorized storage, metadata
- Configuration via `StorageProperties` and `FileStorageConfiguration`

#### 5. MyBatis Plus Integration
- All mappers extend MyBatis Plus `BaseMapper<T>`
- Pagination configured for MySQL via `MybatisPlusConfiguration`
- Mapper scanning via `@MapperScan("com.bqsummer.**.mapper")`
- Mappers use XML or annotation-based queries
- SQL logging enabled at DEBUG level via `logging.level.com.bqsummer.mapper=DEBUG`

### Database Configuration

Database connection details are in `src/main/resources/application.properties`:
- Druid connection pool with slow SQL logging (5s threshold)
- Connection pool: min 5, max 30
- Database initialization script: `src/main/resources/datasourceInit.sql`

### Testing Strategy

Tests use RestAssured for API integration testing:
- `BaseTest` provides common setup with test JWT token via `TestUtil.testToken()`
- Custom assertions in `DbAssertions` for database state verification
- Test pattern: register user → perform action → assert DB state + API response
- Tests should be independent and create unique users per test using timestamp + random suffix

Example test structure:
```java
@DisplayName("UserController API tests")
class UserControllerTest extends BaseTest {
    @Test
    void shouldReturnCurrentUserProfileWhenTokenValid() {
        TestUser user = registerUser("test_prefix");

        given()
            .header("Authorization", "Bearer " + user.token)
        .when()
            .get("/api/v1/user/profile")
        .then()
            .statusCode(200)
            .body("id", equalTo(user.id.intValue()));

        assertValue("users", "username", user.username, "id = ?", user.id);
    }
}
```

## Security Notes

- Passwords are hashed with BCrypt via `BCryptPasswordEncoder`
- JWT tokens contain userId, username, and roles
- Token blacklisting is enforced on logout/delete account
- Deleted or disabled users (`status = 0`) cannot authenticate even with valid tokens
- CORS is configured to allow all origins in development (review for production)
- CSRF is disabled (stateless JWT authentication)
- Session management uses `SessionCreationPolicy.STATELESS`

## Development Workflow

1. Database schema changes should be reflected in corresponding mapper and DTO classes
2. New API endpoints should be added to `SecurityConfig` authorization rules
3. Use Lombok annotations (`@Data`, `@Builder`, etc.) to reduce boilerplate
4. Follow existing package structure when adding new features
5. Add integration tests for new controller endpoints using RestAssured pattern
6. Actuator endpoints are exposed at `/actuator/*` with health details enabled
7. Use Logbook for HTTP logging (configured to TRACE level)

## Common Configuration Properties

Key properties in `application.properties`:
- `spring.datasource.*` - Database connection
- `message.center.*` - Email configuration (SMTP)
- `ip.rate.limit.*` - Rate limiting settings
- `management.endpoints.*` - Actuator endpoints
- `logging.level.*` - Log levels

## Profiles

Active profile set via `spring.profiles.active=local` (default in application.properties)

## Important Files

- `src/main/resources/applicationContext.xml` - XML-based bean configuration
- `src/main/resources/quartz.properties` - Quartz scheduler configuration
- `src/main/resources/logback-spring.xml` - Logging configuration
