<!-- Copilot / AI agent instructions for this repository -->
# Quick agent guide — secure-file-statement-delivery

Purpose
- Help contributors and automated agents make small, correct changes quickly in this Spring Boot service.

Big picture (what this project is)
- Java Spring Boot (Java 21) web application packaged with Maven (`pom.xml`).
- Main code: `src/main/java/za/co/cbank/securefilestatementdelivery`.
- Key layers: `controller` (REST endpoints under `/api/v1/*`), `service`, `repository`, `dto`, `mapper` (MapStruct), and `templates` (Thymeleaf HTML used for PDF generation).

Build / run / test (exact commands)
- Build (offline dependencies cached by wrapper): `./mvnw -B dependency:go-offline` then `./mvnw package`.
- Run locally (dev): `./mvnw spring-boot:run` (requires JDK 21).
- Run tests: `./mvnw test`.
- Docker build/run: `docker compose -f compose.yaml up --build` (compose file is `compose.yaml`).
- Image build used by `Dockerfile` (multi-stage, Eclipse Temurin 21); produced artifact is `target/*.jar`.

Key integrations and config
- Postgres DB (compose service `db`) — production/dev JDBC is in `application.properties` and overridden by env vars in `compose.yaml` (Spring relaxed binding applies: e.g. `SPRING_DATASOURCE_URL`).
- S3-compatible storage via AWS SDK (MinIO in compose). Properties: `config.aws.s3client.*` and env vars `CONFIG_AWS_S3CLIENT_*`.
- PDF generation: `openhtmltopdf` + `pdfbox`; templates live in `src/main/resources/templates` (render HTML -> PDF).
- Observability: OpenTelemetry + Logstash (see `pom.xml` and `application.properties`).

Project-specific conventions (important for agents)
- REST routes use versioned base `/api/v1/*`. Controllers use Lombok `@RequiredArgsConstructor` and return `ResponseEntity`.
- Create endpoints often use explicit `path = "/create"` (example: `CustomerAccountController#createCustomerAccount`).
- MapStruct annotation processors and Lombok are enabled via the maven-compiler plugin — when adding new mappers or DTOs run a full `./mvnw clean package` so generated sources appear.
- Tests include Spring Boot test starters and H2 dependency; unit/integration tests may rely on in-memory DB unless compose is used for local integration tests.
- Configuration layering: `application.properties` imports `security.properties` (optional: `spring.config.import=optional:classpath:security.properties`).

Where to look for examples
- Controllers: `src/main/java/za/co/cbank/securefilestatementdelivery/controller/api/v1` (e.g. `CustomerAccountController.java`).
- Services: `src/main/java/za/co/cbank/securefilestatementdelivery/service`.
- Mappers: `src/main/java/za/co/cbank/securefilestatementdelivery/mapper` (MapStruct usage patterns).
- PDF/templates: `src/main/resources/templates` and PDF config in `application.properties` (`config.pdf.*`).

Editing guidelines for agents
- Small change checklist:
  - Run `./mvnw -DskipTests package` locally after codegen changes (MapStruct/Lombok) to validate generated classes.
  - Prefer editing controller/service/repository in their existing packages — keep package layout and `api/v1` routes consistent.
  - When changing config keys, update `compose.yaml` env names and `application.properties` usage examples.
  - If adding new external dependencies, update `pom.xml` and ensure the docker build still succeeds (Dockerfile runs `./mvnw package`).

Testing & debugging tips
- To reproduce integration behavior, use `docker compose -f compose.yaml up` (brings Postgres + MinIO). Services expect default passwords shown in `compose.yaml` and `application.properties` (`passwd`, `minioadmin`).
- To run the app in a debugger attach to the JVM started via IDE or adjust `ENTRYPOINT` to include remote debug flags for container testing.

If you need clarification
- Ask for the specific file or feature; point to the controller/service/mapper you want to change — e.g. "Change PDF link expiry handling in `src/main/java/.../service/StatementService`".

Keep changes minimal and build-verified: run `./mvnw -DskipTests package` after edits, run tests with `./mvnw test` when modifying logic.
