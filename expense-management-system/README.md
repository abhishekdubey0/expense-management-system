# 💰 Expense Management System

A production-grade, microservices-based Expense Management System built with **Java 17**, **Spring Boot 3.x**, **MySQL**, **Redis**, **Kafka**, and **AWS S3**.

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT (REST)                           │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (:8080)                          │
│         Rate Limiting | JWT Validation | Routing                │
└────┬──────────┬──────────┬──────────┬──────────┬───────────────┘
     │          │          │          │          │
     ▼          ▼          ▼          ▼          ▼
 ┌───────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
 │ Auth  │ │Expense │ │Approval│ │ Report │ │Notif.  │
 │ :8081 │ │ :8082  │ │ :8083  │ │ :8084  │ │ :8085  │
 └───┬───┘ └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘
     │         │          │          │          │
     ▼         ▼          ▼          ▼          ▼
  MySQL      MySQL      MySQL    MySQL+Redis   Kafka
  (auth_db) (exp_db)  (appr_db) (report_db)  Consumer
                          │                    │
                          └──── Kafka ─────────┘
                                (Topics:
                               expense-created
                               expense-approved
                               expense-rejected)
```

---

## 🚀 Services

| Service | Port | Responsibility |
|---|---|---|
| **API Gateway** | 8080 | Routing, rate limiting, JWT filter |
| **Auth Service** | 8081 | Register, login, JWT, refresh tokens |
| **Expense Service** | 8082 | CRUD expenses, receipt upload to S3 |
| **Approval Service** | 8083 | Multi-level approval workflow |
| **Report Service** | 8084 | Analytics, reports (Redis cached) |
| **Notification Service** | 8085 | Email notifications via Kafka |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.x |
| Database | MySQL 8.0 |
| Cache | Redis 7.x |
| Messaging | Apache Kafka 3.x |
| File Storage | AWS S3 |
| Auth | JWT (Access + Refresh tokens) |
| Gateway | Spring Cloud Gateway |
| Containerization | Docker + Docker Compose |
| Build Tool | Maven |
| API Docs | SpringDoc OpenAPI (Swagger) |
| Migrations | Flyway |
| Monitoring | Spring Actuator + Prometheus |

---

## ⚡ Quick Start

### Prerequisites
- Docker & Docker Compose installed
- Java 17+
- Maven 3.8+
- AWS Account (for S3) or use LocalStack for local dev

### 1. Clone the repository
```bash
git clone https://github.com/yourusername/expense-management-system.git
cd expense-management-system
```

### 2. Configure environment variables
```bash
cp .env.example .env
# Edit .env with your values (DB passwords, JWT secret, AWS keys)
```

### 3. Start with Docker Compose
```bash
docker-compose up --build
```

### 4. Access the services
- API Gateway: http://localhost:8080
- Auth Swagger: http://localhost:8081/swagger-ui.html
- Expense Swagger: http://localhost:8082/swagger-ui.html
- Approval Swagger: http://localhost:8083/swagger-ui.html
- Report Swagger: http://localhost:8084/swagger-ui.html

---

## 📁 Project Structure

```
expense-management-system/
├── api-gateway/                    # Spring Cloud Gateway
├── auth-service/                   # Authentication & Authorization
├── expense-service/                # Expense CRUD + S3 uploads
├── approval-service/               # Approval workflow engine
├── notification-service/           # Kafka-driven email notifications
├── report-service/                 # Reports + Redis caching
├── docker/
│   ├── mysql/init/                 # DB init scripts
│   └── kafka/                      # Kafka config
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## 🔐 Authentication Flow

```
POST /auth/register   → Create account
POST /auth/login      → Get access_token (15min) + refresh_token (7days)
POST /auth/refresh    → Rotate refresh token, get new access_token
POST /auth/logout     → Blacklist refresh token in Redis
```

All other endpoints require: `Authorization: Bearer <access_token>`

---

## 👥 Roles & Permissions

| Role | Permissions |
|---|---|
| `EMPLOYEE` | Create/view own expenses, upload receipts |
| `MANAGER` | All Employee + approve/reject team expenses |
| `FINANCE_ADMIN` | All Manager + generate reports, mark reimbursed |
| `SUPER_ADMIN` | Full system access, user management |

---

## 💸 Expense Lifecycle

```
DRAFT → SUBMITTED → PENDING_APPROVAL → APPROVED → REIMBURSED
                                     ↓
                                  REJECTED
```

---

## 📊 Key API Endpoints

### Auth Service
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
```

### Expense Service
```
POST   /api/v1/expenses                    Create expense
GET    /api/v1/expenses                    List expenses (paginated, filtered)
GET    /api/v1/expenses/{id}               Get expense by ID
PUT    /api/v1/expenses/{id}               Update expense
DELETE /api/v1/expenses/{id}               Soft delete
POST   /api/v1/expenses/{id}/submit        Submit for approval
POST   /api/v1/expenses/{id}/receipt       Upload receipt to S3
```

### Approval Service
```
GET    /api/v1/approvals/pending           List pending approvals
POST   /api/v1/approvals/{expenseId}/approve
POST   /api/v1/approvals/{expenseId}/reject
GET    /api/v1/approvals/history           Approval audit trail
```

### Report Service
```
GET    /api/v1/reports/summary             Monthly summary (cached)
GET    /api/v1/reports/by-category         Spending by category
GET    /api/v1/reports/by-department       Department-level report
GET    /api/v1/reports/export?format=csv   Export report
```

---

## 🔧 Environment Variables

See `.env.example` for all required variables.

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific service tests
cd auth-service && mvn test
```

---

## 📈 Monitoring

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

---

## 🤝 Contributing

1. Fork the repo
2. Create feature branch: `git checkout -b feature/your-feature`
3. Commit: `git commit -m 'feat: add your feature'`
4. Push: `git push origin feature/your-feature`
5. Open a Pull Request
