# 🎯 Interview Preparation Guide
## Expense Management System — Senior Backend Engineer

---

## 1. HOW TO INTRODUCE THIS PROJECT (30-second pitch)

> "I built a production-grade, microservices-based Expense Management System using
> Java 17, Spring Boot 3, MySQL, Redis, Kafka, and AWS S3 — deployed on Docker.
> It handles the full expense lifecycle: submission, multi-level approval workflow,
> S3 receipt uploads, Redis-cached analytics reports, and Kafka-driven email notifications.
> The architecture mirrors what companies like Zoho Expense and Expensify use in production."

---

## 2. SYSTEM DESIGN QUESTIONS

### Q: Why microservices over a monolith for this project?

**Answer:**
> Different services have very different scaling profiles:
> - **Expense service**: high write throughput, scales with employee count
> - **Report service**: CPU-intensive aggregations, benefits from independent scaling
> - **Notification service**: I/O-bound, should never block core business flow
>
> Microservices let each scale independently. A monolith would force us to scale everything
> together, wasting resources. The trade-off is operational complexity — which I manage via
> Docker Compose locally and can extend to Kubernetes in production.

---

### Q: How does your approval workflow handle concurrency?

**Answer:**
> The `Expense` entity uses JPA `@Version` for optimistic locking. If two managers
> simultaneously try to approve the same expense, the second update will throw an
> `OptimisticLockException`, which we catch and return a clear error:
> "This expense was already processed." No pessimistic locking needed — approval
> is a low-contention operation.

---

### Q: Why Kafka instead of direct REST calls for notifications?

**Answer:**
> Two reasons:
> 1. **Decoupling**: If the notification service is down, expenses can still be approved.
>    The event is persisted in Kafka and processed when the service recovers.
> 2. **Performance**: The approval API returns in < 50ms instead of waiting for SMTP.
>
> Trade-off: notifications are eventually delivered, not immediate — acceptable since
> a few seconds of email delay is fine.

---

### Q: Why store S3 object keys instead of public URLs?

**Answer:**
> Storing public URLs would expose receipts to anyone with the link permanently.
> Instead, I store the **S3 object key** (e.g., `receipts/uuid/file.pdf`) and generate
> **presigned URLs** at read time with a 1-hour expiry. This means:
> - Receipts are private by default
> - Access is time-limited and auditable
> - We can rotate access without changing stored data

---

### Q: How does your caching strategy work?

**Answer:**
> Report service uses Spring `@Cacheable` backed by Redis:
> - `reports-summary`: TTL 30 minutes — personal expense summary
> - `reports-department`: TTL 60 minutes — changes infrequently
> - `reports-top-spenders`: TTL 15 minutes — finance needs fresher data
>
> Cache is **evicted** via `@CacheEvict` whenever an expense snapshot is updated.
> Cache key includes `submitterId + from + to` for user-scoped isolation.
> In production I'd add cache warming for the most-accessed reports.

---

### Q: How do you handle JWT security across services?

**Answer:**
> **Three-layer approach:**
> 1. **API Gateway** validates the JWT signature before routing — invalid tokens never
>    reach downstream services
> 2. **Each service** independently verifies the JWT using the same shared secret
>    (no network calls) and extracts the user context
> 3. **Redis blacklist** in auth-service: on logout, the access token is blacklisted
>    in Redis until its natural expiry. The gateway checks this on every request.
>
> Refresh token rotation: every refresh issues a new refresh token and revokes the old
> one — preventing refresh token replay attacks.

---

### Q: How would you scale this to 1 million users?

**Answer:**
> **Database layer:**
> - Add MySQL read replicas for report queries (read-heavy)
> - Partition the `expenses` table by `created_at` (range partition) after 100M rows
> - Move to PlanetScale or Aurora for managed scaling
>
> **Application layer:**
> - All services are stateless — horizontal scaling behind a load balancer
> - Kubernetes HPA scales pods based on CPU/memory
>
> **Cache layer:**
> - Redis Cluster for HA (currently single node)
> - Cache hit rate target: > 80% for reports
>
> **Async processing:**
> - Increase Kafka partitions for notification-service (currently 1)
> - Add more consumer instances for parallel processing
>
> **CDN:**
> - CloudFront in front of S3 for receipt delivery at scale

---

## 3. CODE-LEVEL QUESTIONS

### Q: Explain your @Transactional usage

**Answer:**
> I use `@Transactional(readOnly = true)` at the service class level — this is a
> performance optimization that tells Hibernate to skip dirty checking and use
> read-only JDBC connections.
>
> Individual write methods are annotated with `@Transactional` (default read-write),
> which overrides the class-level annotation. This pattern means I never forget
> to mark a read method — writes are explicit opt-ins.

---

### Q: How does your approval policy engine work?

**Answer:**
> The `ApprovalPolicyEngine` is a separate `@Component` with configurable thresholds
> read from `application.yml`:
> - Amount ≤ ₹500: auto-approve (no human needed)
> - Amount ≤ ₹5,000: 1 level (manager only)
> - Amount > ₹5,000: 2 levels (manager + finance admin)
>
> This is completely configurable via environment variables — no code change needed
> when the company changes its policy.

---

### Q: How do you prevent an employee from approving their own expense?

**Answer:**
> In `ApprovalService.validateApproverRole()`:
> ```java
> if (request.getSubmitterId().equals(ctx.getUuid())) {
>     throw new ApprovalException("You cannot approve your own expense");
> }
> ```
> This check runs before any database write — self-approval is blocked at the
> business logic layer regardless of the user's role.

---

### Q: Explain your soft delete strategy

**Answer:**
> Financial data must never be hard-deleted — it's needed for audits and compliance.
> I use `is_deleted = false` flag on the `expenses` table. All queries include
> `AND deleted = false`. Only SUPER_ADMIN can physically delete records (not exposed
> in current API — intentional).
>
> Trade-off: queries are slightly slower (index on `is_deleted` helps). Alternatively,
> I could move deleted records to an `expenses_archive` table.

---

## 4. TRADE-OFFS TO DISCUSS

| Decision | Choice | Trade-off |
|---|---|---|
| Architecture | Microservices | More complexity vs better scalability |
| Inter-service auth | Shared JWT secret | Simpler vs no per-service key rotation |
| Notifications | Kafka async | Eventual delivery vs guaranteed immediacy |
| Reports | Redis cache | Stale data risk vs fast reads |
| Receipts | S3 presigned URLs | Complexity vs security |
| Approval | Optimistic locking | Rare conflict errors vs no lock overhead |
| DB per service | Separate MySQL DBs | Data duplication vs loose coupling |

---

## 5. GITHUB REPOSITORY SETUP CHECKLIST

```
✅ Push code to GitHub
✅ Add README.md with architecture diagram
✅ Add .env.example (never commit real .env)
✅ Add .gitignore (target/, .env, *.jar)
✅ Add GitHub Actions CI/CD pipeline
✅ Tag release: git tag v1.0.0
✅ Add topics: spring-boot, microservices, kafka, redis, aws-s3, mysql
✅ Add screenshots of Swagger UI in README
✅ Pin repo on your GitHub profile
```

---

## 6. METRICS TO MENTION IN INTERVIEWS

- **6 microservices** with independent deployments
- **30+ REST endpoints** across all services
- **JWT with refresh token rotation** + Redis blacklisting
- **Multi-level approval engine** with configurable amount thresholds
- **Redis caching** with per-cache TTL strategy (15–60 minute TTLs)
- **Kafka async messaging** — approval never blocks on notifications
- **S3 presigned URLs** — time-limited, never expose raw storage keys
- **Flyway migrations** — reproducible DB schema across all environments
- **Circuit breakers** at API Gateway — system degrades gracefully
- **Full audit trail** — every state change is logged with actor + timestamp
- **Soft deletes** — financial data retained for compliance
- **Docker Compose** — single command to run entire system locally

---

## 7. FOLLOW-UP FEATURES (mention proactively)

> "Given more time, the next features I would add are:
> 1. **OCR on receipts** using AWS Textract to auto-fill expense details
> 2. **Expense policy violation detection** — flag expenses that exceed category limits
> 3. **Kubernetes manifests** for cloud-native scaling (HPA, resource limits)
> 4. **Distributed tracing** using Micrometer + Zipkin across all services
> 5. **WebSocket notifications** for real-time approval status in the UI"
