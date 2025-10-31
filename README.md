# Sailpoint Access Request PoC

A Proof of Concept (PoC) for managing access requests using Sailpoint with Hexagonal Architecture, JobRunr for background processing, and jOOQ for database operations.

## 🏗️ Architecture

This project follows **Hexagonal Architecture** (Ports and Adapters) pattern:

```
sailpoint-access-request-poc/
├── domain/                          # Core business logic
│   ├── model/                       # Domain entities
│   │   ├── AccessRequest.java
│   │   └── AccessRequestStatus.java
│   ├── port/
│   │   ├── in/                      # Use cases (input ports)
│   │   │   ├── CreateAccessRequestUseCase.java
│   │   │   ├── GetAccessRequestUseCase.java
│   │   │   ├── ExecutePoolingUseCase.java
│   │   │   └── RetryAccessRequestsUseCase.java
│   │   └── out/                     # Infrastructure interfaces (output ports)
│   │       ├── AccessRequestRepositoryPort.java
│   │       ├── SailpointClientPort.java
│   │       └── JobSchedulerPort.java
│   ├── service/                     # Business logic implementation
│   │   ├── AccessRequestService.java
│   │   ├── PoolingService.java
│   │   └── RetryAccessRequestsService.java
│   └── exception/                   # Domain exceptions
│       ├── AccessRequestNotFoundException.java
│       ├── AccessRequestLockedException.java
│       └── InvalidStateTransitionException.java
└── infrastructure/                  # Technical implementation
    ├── config/                      # Spring configuration
    │   ├── JobRunrConfig.java
    │   ├── JooqConfig.java
    │   └── FeignConfig.java
    └── adapter/
        ├── in/                      # Input adapters (REST)
        │   └── rest/
        │       ├── AccessRequestController.java
        │       ├── BulkRetryController.java
        │       ├── GlobalExceptionHandler.java
        │       └── dto/
        └── out/                     # Output adapters
            ├── persistence/         # Database adapter (jOOQ)
            ├── client/              # Sailpoint client (Feign)
            └── job/                 # Job scheduler (JobRunr)
```

## 🚀 Features

- ✅ **Create access requests** with automatic background processing
- ✅ **Automatic pooling** to check Sailpoint status with retries
- ✅ **State machine** with validation (PENDING → POOLING → COMPLETED/FAILED)
- ✅ **Pessimistic locking** with FOR UPDATE SKIP LOCKED
- ✅ **Bulk retry endpoints** for failed/non-completed requests
- ✅ **JobRunr dashboard** for monitoring background jobs
- ✅ **Clean architecture** with clear separation of concerns

## 📋 State Machine

```
PENDING ──→ POOLING ──→ COMPLETED
              ↓
            FAILED ──→ PENDING (retry)
```

### State Transitions:
- `PENDING` → `POOLING`: When background job starts processing
- `POOLING` → `COMPLETED`: When Sailpoint confirms success
- `POOLING` → `FAILED`: When Sailpoint returns error or max retries exceeded
- `FAILED` → `PENDING`: When manually retried
- `POOLING` → `PENDING`: When lock timeout expires or manual retry

## 🔧 Technologies

- **Java 17**
- **Spring Boot 3.4.0**
- **JobRunr 8.1.0** - Background job processing with recurring job mechanism
- **jOOQ 3.19.15** - Type-safe SQL queries with pessimistic locking (persistence layer)
- **PostgreSQL** - Database
- **Liquibase** - Database migrations
- **OpenFeign** - HTTP client for Sailpoint integration
- **Lombok** - Reduce boilerplate code

## 📦 Setup

### Prerequisites

- Java 17+
- Docker & Docker Compose

### 1. Start PostgreSQL

```bash
docker-compose up -d
```

### 2. Run Application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

JobRunr Dashboard: `http://localhost:8080/dashboard`

## 📡 API Endpoints

### Access Requests

#### Create Access Request
```http
POST /api/v1/access-requests
Content-Type: application/json

{
  "userId": "john.doe",
  "accessType": "DATABASE_ACCESS",
  "justification": "Need access for project X"
}
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "john.doe",
  "accessType": "DATABASE_ACCESS",
  "justification": "Need access for project X",
  "status": "PENDING",
  "sailpointRequestId": null,
  "createdAt": "2025-01-22T10:00:00",
  "updatedAt": "2025-01-22T10:00:00"
}
```

#### Get Access Request
```http
GET /api/v1/access-requests/{id}
```

#### Get All Access Requests
```http
GET /api/v1/access-requests
```

### Retry Endpoints

#### Retry Single Request
```http
POST /api/v1/access-requests/retry/{id}
```

#### Retry All Failed Requests
```http
POST /api/v1/access-requests/retry/failed
```

#### Retry All Non-Completed Requests
```http
POST /api/v1/access-requests/retry/non-completed
```

#### Retry All Pooling Requests
```http
POST /api/v1/access-requests/retry/pooling
```

#### Retry with Filters
```http
POST /api/v1/access-requests/retry/filtered
Content-Type: application/json

{
  "statuses": ["FAILED", "POOLING"],
  "updatedBefore": "2025-01-22T00:00:00",
  "userId": "john.doe"
}
```

**Bulk Response:**
```json
{
  "totalProcessed": 10,
  "successCount": 8,
  "failedCount": 2,
  "results": [
    {
      "accessRequestId": "550e8400-e29b-41d4-a716-446655440000",
      "success": true,
      "message": "Retry scheduled successfully"
    }
  ]
}
```

## 🔄 Background Processing Flow

1. **User creates access request** → Status: `PENDING`
2. **JobRunr schedules recurring background job** → Transitions to `POOLING`
3. **Job acquires lock** (FOR UPDATE SKIP LOCKED)
4. **First execution**: Create request in Sailpoint
5. **Subsequent executions**: Check status in Sailpoint (polls continuously)
6. **If completed**: Status → `COMPLETED`, recurring job terminates
7. **If failed**: Status → `FAILED`, recurring job terminates
8. **If still processing**: Job continues polling until completion or failure

**Note**: The pooling job uses `scheduleRecurrently` and only terminates when:
- The access request is **COMPLETED** in SailPoint
- The access request is **FAILED** in SailPoint
- The job cannot continue due to unexpected errors

## 🔐 Pessimistic Locking

The application uses PostgreSQL's `FOR UPDATE SKIP LOCKED` to ensure:
- Only one job processes a request at a time
- No blocking waits
- Lock timeout protection (10 minutes by default)

## 🎯 Configuration

Key properties in `application.yml`:

```yaml
org:
  jobrunr:
    jobs:
      default-number-of-retries: 5      # Max retry attempts
      retry-back-off-time-seed: 30      # Backoff seed (seconds)

sailpoint:
  client:
    base-url: http://localhost:9090/sailpoint/api
  pooling:
    lock-timeout-minutes: 10            # Lock expiration time
```

## 🧪 Testing

```bash
./mvnw test
```

## 📊 Monitoring

Access JobRunr Dashboard at `http://localhost:8080/dashboard` to monitor:
- Scheduled jobs
- Processing jobs
- Succeeded/Failed jobs
- Retry attempts

## 🏗️ Development

### Add New Use Case

1. Create interface in `domain/port/in/`
2. Implement in `domain/service/`
3. Create REST adapter in `infrastructure/adapter/in/rest/`

### Add New External Integration

1. Create port interface in `domain/port/out/`
2. Implement adapter in `infrastructure/adapter/out/`

## 📝 License

This project is a Proof of Concept for educational purposes.

---

**Author:** Max Garsaiz  
**Date:** January 2025