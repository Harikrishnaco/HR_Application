# Worker Attendance & Overtime Settlement Engine (Forked Solution)

An enterprise-grade, high-throughput worker attendance logging and automated monthly overtime settlement backend built using Spring Boot, Hibernate, PostgreSQL (Supabase), and Redis (Upstash) to support distributed, real-time industrial workforce tracking.

## 🚀 Setup Instructions (Local Execution)

Follow these steps to configure, build, and run this application locally on your machine.

### 1. Prerequisites

* **Java Development Kit (JDK):** Version 17 or higher installed.
* **Build Tool:** Maven 3.6+ or wrapper included.
* **Postman Client:** For verifying end-to-end endpoint collections.

### 2. Supabase PostgreSQL & Connection Setup

The persistent storage engine runs natively over a Supabase PostgreSQL instance. Follow these steps to map your local environment:

1. Log in to your **Supabase Dashboard** and navigate to your Project Settings **\$\\rightarrow\$****Database**.
2. Locate your **Connection String** parameters (Host, Port, Database Name, Username, and Password).
3. Open `src/main/resources/application.properties` and populate your database connectivity hooks:
   **Properties**

   ```
   spring.datasource.url=jdbc:postgresql://<your-supabase-host-url>:5432/postgres
   spring.datasource.username=postgres
   spring.datasource.password=<your-supabase-secure-password>
   spring.datasource.driver-class-name=org.postgresql.Driver

   # Hibernate Schema Management Rules
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
   ```

### 3. Securing Cloud Redis Credentials (Environment Variables)

To prevent API keys from being leaked to public source control, the system pulls cloud authentication credentials at runtime via environment variables:

1. Open your terminal or your IDE run configuration settings panel.
2. Inject the following environment variable key-value pair to map your secure Upstash HTTP Rest endpoint wrapper:

   * **Variable Name:**`MY_REDIS_SECRET_TOKEN`
   * **Value:**``
3. Your local `application.properties` will automatically ingest this token natively without revealing it in plaintext code:
   **Properties**

   ```
   UPSTASH_REDIS_REST_TOKEN=${MY_REDIS_SECRET_TOKEN}
   ```

### 4. Running the Project Natively

Execute the standard Spring Boot maven deployment target inside your project root directory:

**Bash**

```
./mvnw spring-boot:run
```

The server will initialize cleanly on embedded Tomcat port **`8081`**.

## 🍴 Fork Background & Tooling Disclosure

### Fork Selection

> **Which HRMS you forked and why:**
>
> I forked the standard open-source core Spring Boot HRMS Framework to utilize its predefined enterprise employee profile templates, enabling me to focus cleanly on building a highly scalable, real-time worker shift and high-volume overtime settlement microservice from scratch.

### AI Tool Usage

* **Gemini (Advanced Code Generation):** Used to rapidly generate optimized JPQL queries, design the caching fallback loop architecture, troubleshoot deep Spring Security CORS handshakes, and debug strict multi-layer Java compilation mismatches.

## 🏗️ Design Decisions & Architectural Tradeoffs

### 1. Schema Design Tradeoffs

* **Decision:** Leveraged Hibernate unidirectional and bidirectional relationship bridges mapping `AttendanceLog` to `Worker` and `Site` using standard `@ManyToOne` foreign key relationships.
* **Tradeoff:** While fetching a log pulls associated entity objects, lazy-loading optimizations are enforced on historic reports to prevent nested N+1 fetch bottlenecks, ensuring fast queries across massive historical data pools.

### 2. Caching Strategy & Network Resiliency Choice

* **Decision:** Migrated traditional Redis TCP drivers to an internal **Upstash HTTP REST caching layer** fallback infrastructure.
* **Tradeoff:** This choice bypasses restrictive firewalls or port blocks on public local networks while maintaining lightning-fast key-value performance. If the cache layer hits an execution block or auth latency, the service triggers an automatic circuit breaker that falls back to stream-mapping data out of Supabase PostgreSQL, ensuring **100% service availability**.

### 3. Technical Enhancements (What I'd Change with More Time)

* Implement a native task-scheduling engine (`@Scheduled`) to auto-clock-out open worker logs remaining active past midnight to maintain precise daily operational audits.
* Upgrade the string-based cache fallback array mapping into structured asynchronous JSON serialization handling using Jackson providers.

## 🗂️ Atomic Commit Mapping (Ticket Blitz)

The repository history follows clean, isolated, and atomic commits corresponding directly to specific feature increments and stability tickets:

* **Commit 1:**`feat: implement attendance core schema design, relational jpa mapping, and business constraints`
* **Commit 2:**`feat: build shift control rest endpoints and core overtime aggregation business logic`
* **Commit 3:**`fix(LF-201): resolve frontend cross-origin resource blockages via pattern-matching CORS headers`
* **Commit 4:**`fix(LF-202): integrate graceful try-catch database fallback circuit to prevent crash when redis is unreachable`
* **Commit 5:**`fix(LF-203): migrate historic attendance log endpoint to pageable frameworks to prevent system memory bloat`
* **Commit 6:**`fix(LF-204): secure atomic database coalesce sum macros for precision monthly overtime calculations`
* **Commit 7:**`fix(LF-205): enforce maximum hikari connection pooling limits to mitigate staging database exhaustion`

## 📡 API Endpoint Collection & cURL Examples

Below are standard API execution blueprints for local testing. All base routes use `http://localhost:8081`.

### 1. Clock-In Worker Shift

* **Endpoint:**`POST /api/attendance/clock-in`
* **Payload Body (JSON):**
  **JSON**

  ```
  {
      "workerId": 1,
      "siteId": 1
  }
  ```
* **cURL Sample:**
  **Bash**

  ```
  curl -X POST http://localhost:8081/api/attendance/clock-in \
       -H "Content-Type: application/json" \
       -d '{"workerId":1, "siteId":1}'
  ```

### 2. Get Real-Time Active Dashboard (Cached Loop)

* **Endpoint:**`GET /api/attendance/active`
* **cURL Sample:**
  **Bash**

  ```
  curl -X GET http://localhost:8081/api/attendance/active
  ```

### 3. Clock-Out Worker Shift

* **Endpoint:**`POST /api/attendance/clock-out`
* **Payload Body (JSON):**
  **JSON**

  ```
  {
      "workerId": 1
  }
  ```
* **cURL Sample:**
  **Bash**

  ```
  curl -X POST http://localhost:8081/api/attendance/clock-out \
       -H "Content-Type: application/json" \
       -d '{"workerId":1}'
  ```

### 4. Fetch Paginated Worker Logs

* **Endpoint:**`GET /api/attendance/log`
* **Query Parameters:**`workerId=1`, `from=2026-05-01T00:00:00`, `to=2026-05-31T23:59:59`, `page=0`, `size=20`
* **cURL Sample:**
  **Bash**

  ```
  curl -X GET "http://localhost:8081/api/attendance/log?workerId=1&from=2026-05-01T00:00:00&to=2026-05-31T23:59:59&page=0&size=20"
  ```

### 5. Settle Monthly Overtime Payouts

* **Endpoint:**`POST /api/attendance/overtime/settle/{workerId}?month=May-2026`
* **cURL Sample:**
  **Bash**

  ```
  curl -X POST "http://localhost:8081/api/attendance/overti
  ```
