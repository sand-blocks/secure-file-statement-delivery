# Secure File Statement Delivery

[![Java 21](https://img.shields.io/badge/java-21-orange)]() [![Spring Boot 4.0.2](https://img.shields.io/badge/Spring%20Boot-4.0.2-green)]() [![PostgreSQL 16+](https://img.shields.io/badge/PostgreSQL-16+-blue)]()

## Overview

**Secure File Statement Delivery** is a production-ready Spring Boot microservice that generates encrypted PDF account statements, stores them securely in S3-compatible object storage, and delivers them via short-lived, app-mediated retrieval tokens. The system never exposes storage credentials or endpoints to end users.

### System Context

The application orchestrates a secure statement lifecycle:

```
┌─────────────────────────────────────────────────────────────────┐
│ SECURE FILE STATEMENT DELIVERY PIPELINE                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [1. DATA]       [2. GENERATION]      [3. ENCRYPTION]           │
│  Postgres DB  →  Thymeleaf + XHTML  →  AES-256-GCM              │
│  (Customer &      Render HTML         (per-file key             │
│   Transaction      PDFBox / OpenHTML  wrapped by KMS)           │
│   data)            PDF output                                   │
│                                              ↓                  │
│          ↓         ←          ←              ←                  │
│                                                                 │
│  [4. STORAGE]       [5. DELIVERY]        [6. RETRIEVAL]         │
│  S3-compatible  →  Metadata + Token   →  Retrieval Link         │
│  Object Store        (Postgres)            (app-issued)         │
│  (MinIO/AWS S3)      Presigned URL          ↓                   │
│                      (30 min default TTL)  Customer receives    │
│                                           token via email/SMS   │
│                                             ↓                   │
│                   [7. VALIDATION]  ←  [8. DOWNLOAD]             │
│                   Verify token      WebClient fetches           │
│                   Check expiry      from S3 (internal)          │
│                   Rate-limit (10/min) Stream to user            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Why This Architecture

- **Zero Storage Exposure**: Direct S3 endpoints and credentials are never exposed to clients. The app acts as a secure intermediary.
- **Short-Lived Access**: Presigned links expire after 30 minutes (configurable). Combined with token hashing, they prevent unauthorized access windows.
- **Comprehensive Auditing**: All statement generation, token issuance, and downloads are logged via Spring AOP aspects with user and IP metadata (MDC).
- **Rate Limiting**: Per-IP rate limiting (10 requests/minute via Bucket4j) mitigates DoS and brute-force attacks on the retrieval endpoint.
- **Encryption by Design**: Per-file AES-256-GCM encryption with key wrapping (KMS-ready for production).
- **Layered Security**: Spring Security enforces authentication (HTTP Basic, in-memory users) on internal endpoints; public retrieval endpoint permits anonymous access but validates tokens.

---

## Core Domain Logic

### The Security Model

**Threat Model & Mitigations**:

1. **Unauthorized File Access** → Short-lived presigned URLs + unique token + IP-based rate limiting
2. **Storage Credential Exposure** → Internal-only S3 client; all requests funneled through app
3. **Replay Attacks** → Token stored in DB; download links include expiry timestamp
4. **Audit Trail Gaps** → AOP-based auditing logs all statement generation/downloads with user+IP+timestamp+trace-id

### Statement Generation Flow

1. **POST /api/v1/statements/create** (authenticated, HTTP Basic)
   - Validate account exists and has transactions
   - Render HTML statement using Thymeleaf template (`src/main/resources/templates/statement.html`)
   - Convert HTML → PDF via OpenHTMLtoPDF (with pdfbox backend)

2. **Encryption & Storage**
   - Generate random AES-256-GCM key per statement
   - Encrypt PDF blob
   - Wrap AES key with master KMS key (`config.pdf.encryption.master_secret_key`)
   - Upload encrypted blob to S3 bucket (`files`) with unique object key
   - Store statement metadata + token hash in Postgres `statement` table

3. **Token Generation**
   - Generate cryptographically secure random token (48+ bits entropy)
   - Return presigned S3 URL + token to caller
   - Client forwards retrieval link to customer (e.g., via email: `/api/v1/public/{token}`)

### Download & Retrieval Flow

**GET /api/v1/public/{retrievalToken}** (unauthenticated, public endpoint)

1. Rate-limit check: Bucket4j per-IP rate limiter (10 req/min)
2. Lookup token hash in database
3. Verify token not expired (30 min TTL)
4. Fetch encrypted object from S3 using internal WebClient
5. Decrypt blob using wrapped key
6. Stream decrypted PDF to client
7. Audit log: record download with timestamp, IP, user agent

**Why Not Presigned URLs Directly?**  
- Exposes S3 endpoint to client (compliance risk)
- Loses fine-grained audit context (S3 logs are separate)
- Presigned URL can be cached/shared

---


## Getting Started (< 2 minutes)

### Prerequisites

- **JDK 21** (set `JAVA_HOME` if running outside container)
- **Git**
- **Docker & Docker Compose** (optional but recommended for local integration testing)
- Maven wrapper included (no separate Maven install needed)

### Option 1: Run Locally (Dev Mode)

```bash
# Clone and navigate
git clone https://github.com/sand-blocks/secure-file-statement-delivery.git
cd secure-file-statement-delivery

# Fetch dependencies (offline cache for faster builds)
./mvnw -B dependency:go-offline

# Build and package
./mvnw -DskipTests package

# Run the app (uses in-memory H2 DB by default; limited testing)
./mvnw spring-boot:run
```

The app will start on **https://localhost:8443** (self-signed certificate; accept the risk).

### Option 2: Full Integration Stack (Recommended)

```bash
# Start PostgreSQL + MinIO + App (all three services)
docker compose -f compose.yaml up --build

# In another terminal, verify health:
curl -k https://localhost:8443/actuator/health
```

This loads:
- **PostgreSQL** (`cbank` database) on `localhost:5432`
- **MinIO** (S3-compatible) on `localhost:9000` (console at `:9001`)
- **App** on `https://localhost:8443`

All default credentials (postgres/passwd, minioadmin/minioadmin) are **development only**—replace in production via environment variables or secrets manager.

---

## API Reference

### Authentication

Internal endpoints (`/api/v1/**`) use **HTTP Basic Auth**.

Users are defined in [src/main/resources/security.properties](src/main/resources/security.properties) (development) or environment (`SECURITY_USERS_0_NAME=admin`).
```bash
# Example: create statement (requires valid username:password)
curl -X POST https://localhost:8443/api/v1/statements/create \
  -H "Authorization: Basic cG9zdG1hbnVzZXI6cG9zdG1hbnVzZXJwYXNz" \
  -H "Content-Type: application/json" \
  -d '{"accountId": 1000000003}' -k
```
**Use the Customer's ID Number to view the PDF Document**
```bash
9001010000085
```


### Core Endpoints

The comprehensive API guide is available on a publically accessible [Postman Collection](https://www.postman.com/descent-module-astronomer-28342281/cbank-secure-file-statement-delivery). Be sure to choose the **Local Dev** Environment to pre-populate all variables.

| Endpoint | Method | Auth | Purpose |
|---|---|---|---|
| `/api/v1/statements/create` | POST | HTTP Basic | Generate statement for account (returns retrieval token & link) |
| `/api/v1/public/{token}` | GET | None | Redeem retrieval token, download statement PDF |
| `/api/v1/customer-accounts/create` | POST | HTTP Basic | Create customer account |
| `/api/v1/customer-accounts/{accountId}` | GET | HTTP Basic | Fetch account details |
| `/api/v1/transactions/account/{accountId}` | GET | HTTP Basic | List transactions for account |
| `/actuator/health` | GET | None | Liveness/readiness probe (k8s) |

### Example: Generate Statement



```bash
curl -k -X POST https://localhost:8443/api/v1/statements/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic cG9zdG1hbnVzZXI6cG9zdG1hbnVzZXJwYXNz" \
  -d '{
    "accountId": 1000000003
  }'
```
**Use the Customer's ID Number to view the PDF Document**
```bash
9001010000085
```
```bash
# Response (HTTP 201):
{
  "statementId": 42,
  "accountId": 1000000003,
  "retrievalToken": "1111-2222...",
  "link": "https://localhost:8443/api/v1/public/1111-2222...",
  "expiresAt": "2025-02-05T14:30:00Z"
}
```

### Example: Download Statement

```bash
# Customer receives this link, clicks it (no auth needed):
curl -k https://localhost:8443/api/v1/public/1111-2222...

# Returns: PDF file (application/pdf)
#**Use the Customers ID Number to view the PDF file** 
```
### Diagram: Flow
![alt text](docs/diagrams/flow.png)

---

## Development & Operations

### Build Commands

| Command | Purpose |
|---|---|
| `./mvnw clean package` | Full build with tests (required after mapper/DTO changes) |
| `./mvnw -DskipTests package` | Build without tests (fast iteration) |
| `./mvnw -B dependency:go-offline` | Cache dependencies offline (CI/CD friendly) |
| `./mvnw spring-boot:run` | Run locally in dev mode |
| `./mvnw test` | Run all unit & integration tests |
| `./mvnw -Dtest=StatementServiceTest test` | Run single test class |

### Testing Strategy

**Unit Tests** (in-memory H2 DB, mocked S3):
```bash
./mvnw test
```

**Integration Tests** (require compose stack):
```bash
docker compose -f compose.yaml up --build
# In another terminal:
./mvnw verify  # includes integration profile
```

Key test files:
- [src/test/java/za/co/cbank/securefilestatementdelivery/service/StatementServiceTest.java](src/test/java/za/co/cbank/securefilestatementdelivery/service/StatementServiceTest.java)
- [src/test/java/za/co/cbank/securefilestatementdelivery/controller/StatementControllerTest.java](src/test/java/za/co/cbank/securefilestatementdelivery/controller/StatementControllerTest.java)
- [src/test/java/za/co/cbank/securefilestatementdelivery/BlackBoxTest/StatementGenerationBlackBoxTest.java](src/test/java/za/co/cbank/securefilestatementdelivery/BlackBoxTest/StatementGenerationBlackBoxTest.java)

### Testing Conventions

- **Unit Tests**: Test services in isolation (mocked repositories, mocked S3 client).
- **Integration Tests**: Use `@SpringBootTest` with `TestContainers` or compose for real DB/S3.
- **Test Data**: Use builders or `@DataBuilder` for readable test fixtures.
- **Black Box Tests**: End-to-end tests (e.g., `StatementGenerationBlackBoxTest.java`) that validate entire pipeline.

Example unit test :

```java
@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

	@Mock
	private TemplateEngine templateEngine;

	@InjectMocks
	private StatementService statementService;

	private CustomerAccount testAccount;

	@BeforeEach
	void setUp() {
		testAccount = CustomerAccount.builder()
				.accountId(1000000001)
				.firstName("John")
				.idNumber("9001010000083")
				.build();
	}

	@Test
	void formatStatementTransactions_ShouldCalculateCorrectBalance() {
		List<Transaction> transactions = Arrays.asList(
				Transaction.builder().amount(new BigDecimal("1000.00")).drOrCr("CR").customerAccount(testAccount).build(),
				Transaction.builder().amount(new BigDecimal("500.00")).drOrCr("DR").customerAccount(testAccount).build(),
				Transaction.builder().amount(new BigDecimal("50.00")).drOrCr("CR").customerAccount(testAccount).build()
		);
		ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

		statementService.formatStatementTransactions(transactions);

		verify(templateEngine).process(eq("statement"), contextCaptor.capture());

		Context capturedContext = contextCaptor.getValue();

		BigDecimal capturedBalance = (BigDecimal) capturedContext.getVariable("totalBalance");
		CustomerAccount capturedAccount = (CustomerAccount) capturedContext.getVariable("customerAccount");

		assertEquals(0, new BigDecimal("550.00").compareTo(capturedBalance),
				"The balance should be 550.00");
		assertEquals("John", capturedAccount.getFirstName());
	}

	@Test
	void formatStatementTransactions_ShouldReturnEmpty_WhenListIsEmpty() {
		// Act
		String result = statementService.formatStatementTransactions(List.of());

		// Assert
		assertEquals("No transactions, cannot generate statement", result);
	}
}
```

### Configuration & Environment Variables

Spring uses **relaxed property binding**: `config.aws.s3client.secret_key` = `CONFIG_AWS_S3CLIENT_SECRET_KEY`.

**Key Properties** (see [src/main/resources/application.properties.bak](src/main/resources/application.properties.bak)):

| Property | Env Var | Dev Default | Production |
|---|---|---|---|
| `server.port` | `SERVER_PORT` | `8443` | Use load-balancer port (typically 443) |
| `server.ssl.bundle` | N/A | `ssl-bundle` (PKCS12 keystore) | Use Kubernetes secret or Vault cert |
| `config.aws.s3client.endpoint` | `CONFIG_AWS_S3CLIENT_ENDPOINT` | `http://cbank-minio:9000` | `https://s3.amazonaws.com` (or S3-compatible endpoint) |
| `config.aws.s3client.access_key` | `CONFIG_AWS_S3CLIENT_ACCESS_KEY` | `minioadmin` | **Secrets Manager / Vault** |
| `config.aws.s3client.secret_key` | `CONFIG_AWS_S3CLIENT_SECRET_KEY` | `minioadmin` | **Secrets Manager / Vault** |
| `config.aws.s3client.bucket_name` | `CONFIG_AWS_S3CLIENT_BUCKET_NAME` | `files` | Environment-specific (e.g., `cbank-statements-prod`) |
| `config.aws.s3client.link_expiry_mins` | `CONFIG_AWS_S3CLIENT_LINK_EXPIRY_MINS` | `30` | `15` (recommended for higher security) |
| `config.pdf.encryption.master_secret_key` | `CONFIG_PDF_ENCRYPTION_MASTER_SECRET_KEY` | `passwd` | **KMS-wrapped key** (never hardcoded) |
| `config.pdf.retrieval.public_url_base` | `CONFIG_PDF_RETRIEVAL_PUBLIC_URL_BASE` | `https://localhost:8443/api/v1/public/` | Customer-facing domain (e.g., `https://statements.cbank.co.za/api/v1/public/`) |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://cbank-postgres:5432/cbank` | Production RDS/managed PostgreSQL endpoint |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `postgres` | **Secrets Manager / Vault** |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `passwd` | **Secrets Manager / Vault** |
| `spring.datasource.hikari.maximum-pool-size` | N/A | `10` | Scale based on concurrency (e.g., `20` for high load) |

**Security Override Pattern**:

```bash
# Docker Compose example (development)
export CONFIG_AWS_S3CLIENT_SECRET_KEY=$(aws secretsmanager get-secret-value --secret-id cbank/s3-secret --query SecretString --output text)
docker compose -f compose.yaml up
```

Kubernetes example (production):
```yaml
env:
  - name: CONFIG_PDF_ENCRYPTION_MASTER_SECRET_KEY
    valueFrom:
      secretKeyRef:
        name: cbank-secrets
        key: pdf-master-key
```

### Project Structure & Architecture

**Architecture Pattern**: Layered (4-tier) with cross-cutting audit concerns.

```
src/main/java/za/co/cbank/securefilestatementdelivery
├─ controller/api/v1/               # REST API (Controllers)
│  ├─ StatementController.java       # POST /api/v1/statements/create
│  ├─ PublicController.java          # GET /api/v1/public/{token}
│  ├─ CustomerAccountController.java
│  └─ TransactionController.java
│
├─ service/                          # Business Logic (Services)
│  ├─ StatementService.java          # Statement generation & retrieval
│  ├─ FileCreationService.java       # HTML → PDF rendering pipeline
│  ├─ FileStorageService.java        # Encryption & S3 upload/download
│  ├─ CustomerAccountService.java
│  └─ TransactionService.java
│
├─ repository/                       # Data Access (Spring Data JPA)
│  ├─ StatementRepository.java
│  ├─ CustomerAccountRepository.java
│  ├─ TransactionRepository.java
│  └─ AuditLogRepository.java
│
├─ entity/                           # Domain Models (JPA)
│  ├─ Statement.java
│  ├─ CustomerAccount.java
│  ├─ Transaction.java
│  └─ AuditLog.java
│
├─ dto/                              # API Request/Response DTOs
│  ├─ StatementDTO.java
│  ├─ CustomerAccountDTO.java
│  └─ TransactionDTO.java
│
├─ mapper/                           # MapStruct Entity↔DTO Mappers
│  ├─ StatementMapper.java
│  ├─ CustomerAccountMapper.java
│  └─ TransactionMapper.java
│
├─ config/                           # Spring Configuration Beans
│  ├─ SecurityConfig.java            # HTTP Basic + SecurityFilterChain
│  ├─ S3ClientConfig.java            # AWS SDK S3 client bean
│  ├─ WebClientConfig.java           # Spring WebClient (async HTTP)
│  ├─ RateLimitConfig.java           # Bucket4j rate-limit bean
│  └─ SecurityProperties.java        # @ConfigurationProperties for users
│
├─ web/
│  └─ filter/                        # Servlet Filters
│     ├─ AuditContextFilter.java     # Sets MDC (user, IP) before request
│     └─ RateLimitingFilter.java     # Per-IP rate limiting (Bucket4j)
│
├─ Audit/                            # Audit Cross-Cutting Concern
│  ├─ annotation/
│  │  └─ @Auditable                  # Marks methods for audit logging
│  ├─ aspect/
│  │  └─ AuditAspect.java            # Spring AOP intercepts @Auditable
│  ├─ entity/
│  │  └─ AuditLog.java               # Persistent audit record
│  ├─ repository/
│  │  └─ AuditLogRepository.java
│  └─ service/
│     └─ AuditService.java           # Writes to audit log
│
├─ exception/                        # Custom Exceptions
│  ├─ FileCreationException.java
│  ├─ DatabaseEntityException.java
│  └─ GlobalExceptionHandler.java    # @ControllerAdvice
│
└─ SecureFileStatementDeliveryApplication.java  # Entry point
```

### Diagram: Entity Relationship Diagram

![alt text](docs/diagrams/erd.png)

**Key Design Decisions**:

1. **Lombok @RequiredArgsConstructor**: Constructor injection with minimal boilerplate. Enables final field declaration for thread-safe immutability.
2. **MapStruct 1.6.3 Mappers**: Compile-time code generation for entity↔DTO conversions. Faster than reflection-based mappers (Jackson) and type-safe.
3. **Spring Data JPA + Hibernate**: Auto-generated repository methods, type-safe queries. Configuration: lazy loading disabled; explicit join-fetch where needed.
4. **WebClient (Reactive)**: Non-blocking HTTP client for S3 downloads. Handles backpressure gracefully under high concurrency.
5. **Spring AOP @Auditable**: Cross-cutting audit logging without polluting business logic. Captures method name, arguments (serialized), result, exceptions, and execution time.
6. **Bucket4j Per-IP Rate Limiting**: In-memory token bucket per unique client IP. Prevents brute-force on retrieval tokens. Alternative: distributed store (Redis) for multi-instance deployments.

---

## Performance & Scalability

### Throughput & Bottlenecks

**Current Configuration** (single instance):
- **Concurrent Statements**: ~10 req/min (rate-limited); PostgreSQL connection pool 10 (max), 5 (min).
- **PDF Rendering**: 2–5 sec per statement (depends on transaction count). Blocking operation; CPU-bound.
- **S3 Upload/Download**: Network latency + 512 MB max file size (default). Async via WebClient.

**Scaling Recommendations**:

| Metric | Threshold | Action |
|---|---|---|
| CPU (PDF rendering) | > 70% | Add more instances behind load-balancer; consider async statement job queue (Spring Batch / Kafka) |
| DB connections exhausted | > 8/10 | Increase `spring.datasource.hikari.maximum-pool-size` or scale read replicas |
| Disk (S3 bucket) | > 80% | Enable S3 lifecycle policies (archive to Glacier after 90 days) |
| Per-IP rate limit hits | > 20% of traffic | Consider dynamic limits based on user tier or switch to fixed pool size |

**Async Statement Generation** (Future Enhancement):

Current: synchronous POST response includes retrieval link immediately.

Recommended: offload PDF rendering to background job queue:
```
POST /api/v1/statements/create → Returns {jobId, statusUrl}
GET /api/v1/jobs/{jobId} → Returns {status: "generating" | "ready", link: "..."}
```

Enables: horizontal scaling of PDF rendering tier, graceful degradation, retry logic.

### Database Optimization

**Indexes** (essential for production):

```sql
-- Statement retrieval by token (most frequent query)
CREATE INDEX idx_statement_retrieval_token ON statement(retrieval_token);

-- Audit log queries
CREATE INDEX idx_audit_log_timestamp ON audit_log(created_at DESC);
CREATE INDEX idx_audit_log_user ON audit_log(username, created_at DESC);

-- Transaction lookups by account
CREATE INDEX idx_transaction_account_date ON transaction(account_id, post_date DESC);
```

**Connection Pool Tuning**:

```properties
# Hikari defaults are conservative; adjust for your workload
spring.datasource.hikari.maximum-pool-size=20        # Concurrent requests
spring.datasource.hikari.minimum-idle=5              # Warmup pool
spring.datasource.hikari.max-lifetime=1800000        # 30 min (Postgres idle connection timeout)
spring.datasource.hikari.idle-timeout=600000         # 10 min
```

---

## Deployment & Operations

### Docker Build

Multi-stage build (see [Dockerfile](Dockerfile)):

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd -ms /bin/bash springuser
USER springuser
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build & Push**:

```bash
docker build -t cbank/secure-file-statement-delivery:1.0.0 .
docker push cbank/secure-file-statement-delivery:1.0.0
```

### Kubernetes Deployment (Example)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: secure-file-statement-delivery
spec:
  replicas: 3
  selector:
    matchLabels:
      app: sfsd
  template:
    metadata:
      labels:
        app: sfsd
    spec:
      containers:
      - name: sfsd
        image: cbank/secure-file-statement-delivery:1.0.0
        ports:
        - containerPort: 8443
        env:
        - name: CONFIG_PDF_ENCRYPTION_MASTER_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: cbank-secrets
              key: pdf-master-key
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: cbank-db-secret
              key: password
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8443
            scheme: HTTPS
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8443
            scheme: HTTPS
          initialDelaySeconds: 10
          periodSeconds: 5
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
```

### Observability

**Metrics & Tracing**:

- **OpenTelemetry + Logstash**: Logs forwarded to `localhost:5044` (configurable). Includes MDC context (user, IP) and trace IDs.
- **Spring Boot Actuator**: `/actuator/health`, `/actuator/prometheus` (if Micrometer Prometheus is added).
- **Audit Logs**: Persisted in `audit_log` table; queryable via `/api/v1/audit?startDate=...&endDate=...&username=...` (future endpoint).

**Key Metrics to Monitor**:

```properties
# Add to Prometheus scrape config
- job_name: 'secure-file-statement-delivery'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ['localhost:8443']
```

Watch:
- `http_server_requests_seconds{endpoint="/api/v1/statements/create"}` – statement generation latency
- `http_server_requests_seconds{endpoint="/api/v1/public/*"}` – retrieval latency (should be < 1s, limited by S3)
- `jvm_memory_usage_bytes` – heap pressure during bulk PDF renders
- `hikaricp_connections_active` – active DB connections

### Troubleshooting

**Statement Generation Fails** (`HTTP 500`):

1. Check PostgreSQL connectivity: `curl localhost:5432` or `psql -h localhost -U postgres -d cbank -c "SELECT 1"`
2. Verify account exists: `SELECT * FROM customer_account WHERE account_id = ?`
3. Check S3/MinIO: `curl http://localhost:9000` (MinIO console) or `aws s3 ls --endpoint-url http://localhost:9000`
4. Review logs: `docker logs cbank-secure-file-statement-delivery` or app logs in `/var/log/app.log`

**Retrieval Token Invalid** (`HTTP 404` or `HTTP 400`):

1. Verify token not expired: `SELECT retrieved_at, expires_at FROM statement WHERE retrieval_token_hash = HASH(?)`
2. Check rate limit headers: `curl -v https://localhost:8443/api/v1/public/token | grep X-Rate-Limit`
3. Ensure S3 object exists: `aws s3 ls s3://files --endpoint-url http://localhost:9000`

**PDF Rendering Slow**:

1. Profile Thymeleaf template: Add Spring Boot DevTools in IDE for live template reload.
2. Reduce transaction count in test (filter by date range).
3. Check thread pool: `jconsole` or `jps` to inspect GC pauses during rendering.

---

## Code Quality & Conventions

### Development Practices

- **Constructor Injection**: Use `@RequiredArgsConstructor` (Lombok) for DI. Enables final fields and immutability.
- **Thin Controllers**: Controllers are orchestrators only. Business logic lives in services.
- **DTO Mapping**: Use MapStruct. Run `./mvnw clean package` after adding/modifying mappers (compile-time generation).
- **Exception Handling**: Custom exceptions extend `RuntimeException`. Global handler via `@ControllerAdvice` in [src/main/java/za/co/cbank/securefilestatementdelivery/exception/GlobalExceptionHandler.java](src/main/java/za/co/cbank/securefilestatementdelivery/exception/GlobalExceptionHandler.java).
- **Logging**: SLF4J with Logback. Use `@Slf4j` (Lombok). MDC context available in filters (user, IP).

---
## Prepping for production deployment
- Replace dev secrets and use environment variables or a secrets manager (see `application.properties` for keys to replace).
- Align the Key management strategy to the origization's policy with regards to managed KMS for master keys and record key versions used to encrypt files.
- Select a monitoring and alerting strategy and configure tools (OpenTelemetry + Logstash already present); send metrics to a persistent backend.
- Harden the runtime: set JVM args for memory limits, enable remote logging, enable health checks and readiness/liveness probes.
- Use infrastructure IaC to provision Postgres backups, S3 lifecycle, bucket policies, and LB/Ingress TLS.


## Production Checklist

Before deploying to production:

- [ ] **Secrets**: Replace all hardcoded defaults (`passwd`, `minioadmin`) with environment variables from Vault/Secrets Manager.
- [ ] **TLS Certificates**: Import signed certificate into keystore (`spring.ssl.bundle.jks.ssl-bundle.keystore.location`); use Kubernetes secrets in production.
- [ ] **Database**: Migrate to managed PostgreSQL (RDS, CloudSQL); enable automated backups, multi-AZ replication.
- [ ] **S3**: Use AWS S3 (or managed S3-compatible service) with bucket encryption, versioning and lifecycle policies.
- [ ] **Rate Limiting**: Deploy to multi-instance setup; switch to distributed rate limiter (Redis + Spring Session) if needed.
- [ ] **Audit Log Retention**: Set up log archival to S3/Glacier for compliance (typically 7+ years for financial statements).
- [ ] **Monitoring**: Wire Prometheus/Grafana, set up alerts for high latency, errors, and connection pool exhaustion.
- [ ] **Load Testing**: Run load tests (Apache JMeter, k6) to validate throughput under expected peak load (e.g., month-end statement generation).
- [ ] **Security Audit**: Conduct SAST (SonarQube), DAST, and dependency scanning (Snyk, Dependabot).
- [ ] **Documentation**: Update runbooks with production URLs, on-call escalation procedures, and incident response.

---

## Related Files & Resources

- [pom.xml](pom.xml) – Maven configuration, dependencies
- [application.properties.bak](src/main/resources/application.properties.bak) – Configuration template
- [Dockerfile](Dockerfile) – Multi-stage build
- [compose.yaml](compose.yaml) – Local integration stack (Postgres + MinIO + App)
- [init.sql](init.sql) – Database initialization script
- [Audit Aspect Implementation](src/main/java/za/co/cbank/securefilestatementdelivery/Audit/aspect/AuditAspect.java) – Spring AOP for audit logging
- [Security Configuration](src/main/java/za/co/cbank/securefilestatementdelivery/config/SecurityConfig.java) – HTTP Basic auth, filter chain
- [ERD](docs/diagrams/erd.png) – Entity Relationship Diagram
- [Flow Diagram](docs/diagrams/flow.png) – High-level process flow


### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.2/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.2/maven-plugin/build-image.html)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.0.2/reference/actuator/index.html)
* [Docker Compose Support](https://docs.spring.io/spring-boot/4.0.2/reference/features/dev-services.html#features.dev-services.docker-compose)
* [OpenTelemetry](https://docs.spring.io/spring-boot/4.0.2/reference/actuator/observability.html#actuator.observability.opentelemetry)
* [Spring Security](https://docs.spring.io/spring-boot/4.0.2/reference/web/spring-security.html)
* [Reactive HTTP Client](https://docs.spring.io/spring-boot/4.0.2/reference/io/rest-client.html#io.rest-client.webclient)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.2/reference/web/servlet.html)



