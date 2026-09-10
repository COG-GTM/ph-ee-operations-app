# payment-hub-ee
Payment Hub Enterprise Edition middleware for integration to real-time payment systems. 
#Auto-trigger

## Requirements

- JDK 21 (build, runtime and Docker image all target Java 21)
- Gradle 8.5 (via the wrapper, `./gradlew`)
- Spring Boot 2.7.18
- MySQL 5.7+ reachable at `fineract.datasource.core.host` (see `docker-compose.yml`)

## Build and run

```bash
./gradlew clean build          # compiles and runs the tests on JDK 21
./gradlew bootJar
java -jar build/libs/*.jar --tenants=<tenant>   # e.g. --tenants=gorilla
```

Swagger UI is served at `http://localhost:5000/swagger-ui/index.html`, health at `/actuator/health`.

## Java 11 → 21 migration notes

Version bumps:

| Component | Before | After |
| --- | --- | --- |
| Java | 11 | 21 |
| Gradle wrapper | 7.4 | 8.5 |
| Spring Boot | 2.1.9.RELEASE | 2.7.18 |
| spring-security-oauth2 | 2.4.1.RELEASE | 2.5.2.RELEASE |
| spring-security-jwt | 1.1.0.RELEASE | 1.1.1.RELEASE |
| springdoc-openapi-ui | 1.6.11 | 1.7.0 |
| EclipseLink | 2.7.6 | 2.7.15 |
| hibernate-jpamodelgen | 5.4.17.Final | 5.6.15.Final |
| Lombok | 1.18.24 | 1.18.34 |
| mysql-connector-java | 8.0.20 | 8.0.33 |
| JUnit | 4.11 | 4.13.2 |
| commons-lang3 / HikariCP | pinned | managed by Spring Boot |
| CI / Docker images | eclipse-temurin:17 | eclipse-temurin:21 |

Breaking changes handled:

- Spring Data: `Specifications` → `Specification`, `new PageRequest(..)` → `PageRequest.of(..)`, `new Sort(..)` → `Sort.by(..)`.
- Spring Boot `JpaBaseConfiguration` constructor no longer takes `TransactionManagerCustomizers`.
- Properties: `spring.resources.add-mappings` → `spring.web.resources.add-mappings`; `spring.mvc.favicon.enabled` removed;
  `spring.mvc.pathmatch.matching-strategy=ant_path_matcher` set for springdoc 1.x / OAuth endpoints;
  `logging.pattern.console` moved to its correct location.
- Gradle 8: `mainClassName` → `mainClass`, `annotationProcessorGeneratedSourcesDirectory` → `generatedSourceOutputDirectory`.

Spring Boot 3.x / Jakarta EE was deliberately not adopted: `org.mifos:ph-ee-connector-common:1.4.1-gazelle`
(`@EnableJsonWebSignature`) is compiled against `javax.servlet`, and the `/oauth/token` password-grant authorization
server depends on `spring-security-oauth2`, which has no Jakarta / Spring Security 6 equivalent. Moving to Boot 3
requires a Jakarta build of connector-common and a redesign of the OAuth2 authorization server.
