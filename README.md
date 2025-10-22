# Sailpoint Access Request PoC

A Proof of Concept (PoC) application for managing Sailpoint access requests using Hexagonal Architecture, Spring Boot 3.5.6, JobRunr for background processing, jOOQ for database operations, and PostgreSQL.

## Architecture

This application follows **Hexagonal Architecture** (Ports and Adapters) principles:

- **Domain Layer**: Contains business logic, models, ports (interfaces), and domain services
- **Infrastructure Layer**: Contains adapters that implement the ports (REST controllers, database repositories, external clients, job schedulers)

### Key Components

- **Domain Models**: `AccessRequest`, `RequestStatus`
- **Domain Ports**: `AccessRequestRepository`, `SailpointClient`
- **Domain Service**: `AccessRequestService` - Contains all business logic
- **Infrastructure Adapters**:
  - `AccessRequestRepositoryAdapter` - PostgreSQL persistence using JPA and jOOQ
  - `SailpointClientAdapter` - Feign HTTP client for Sailpoint API
  - `AccessRequestController` - REST API endpoints
  - `AccessRequestJobScheduler` - JobRunr background processing

## Features

✅ **Complete Domain Layer**
- Domain models with clear business rules
- Port interfaces for external dependencies
- Domain service with business logic
- Custom domain exceptions

✅ **Infrastructure Layer**
- REST API with validation
- PostgreSQL persistence with JPA entities
- jOOQ support for complex queries
- Feign client for Sailpoint integration
- JobRunr for background job processing

✅ **Database Management**
- Liquibase migrations for schema management
- Optimized indexes for query performance
- Pessimistic locking with `FOR UPDATE SKIP LOCKED` for concurrent processing

✅ **Error Handling**
- Global exception handler using Spring's ProblemDetail (RFC 7807)
- Proper HTTP status codes and error messages
- Structured error responses

✅ **Background Processing**
- JobRunr for asynchronous request processing
- Bulk retry endpoints for failed requests
- Batch processing with pessimistic locking

## Technology Stack

- **Java 17**
- **Spring Boot 3.5.6**
- **PostgreSQL** - Database
- **Liquibase 4.29.2** - Database migrations
- **jOOQ 3.19.11** - Type-safe SQL
- **JobRunr 8.0.2** - Background job processing
- **Feign 13.5** - HTTP client
- **Lombok** - Reduce boilerplate code
- **Maven** - Build tool

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ running on `localhost:5432`
- Database: `sailpoint_access_request`

## Database Setup

### Option 1: Using Docker Compose (Recommended)

```bash
docker-compose up -d
```

This will start a PostgreSQL 16 instance with the database `sailpoint_access_request` already created.

### Option 2: Manual PostgreSQL Setup

Create the PostgreSQL database:

```sql
CREATE DATABASE sailpoint_access_request;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE sailpoint_access_request TO postgres;
```

The application will automatically run Liquibase migrations on startup to create tables and indexes.

## Configuration

Update `src/main/resources/application.yml` if needed:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sailpoint_access_request
    username: postgres
    password: postgres

sailpoint:
  api:
    base-url: http://localhost:8080/sailpoint
```

## Building and Running

### Build the application

```bash
mvn clean install
```

### Run the application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

JobRunr Dashboard will be available at `http://localhost:8001`

## API Documentation

### Base URL
```
http://localhost:8080/api/v1
```

### Endpoints

#### 1. Create Access Request

Creates a new access request and returns it in PENDING status.

**POST** `/access-requests`

**Request Body:**
```json
{
  "requesterId": "user123",
  "requesterName": "John Doe",
  "targetUserId": "target456",
  "targetUserName": "Jane Smith",
  "accessItems": ["Role_A", "Role_B", "Application_X"]
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "requesterId": "user123",
  "requesterName": "John Doe",
  "targetUserId": "target456",
  "targetUserName": "Jane Smith",
  "accessItems": ["Role_A", "Role_B", "Application_X"],
  "status": "PENDING",
  "sailpointRequestId": null,
  "errorMessage": null,
  "retryCount": 0,
  "createdAt": "2025-10-22T10:30:00",
  "updatedAt": "2025-10-22T10:30:00",
  "processedAt": null
}
```

#### 2. Get Access Request by ID

Retrieves a specific access request.

**GET** `/access-requests/{id}`

**Response:** `200 OK`
```json
{
  "id": 1,
  "requesterId": "user123",
  "requesterName": "John Doe",
  "targetUserId": "target456",
  "targetUserName": "Jane Smith",
  "accessItems": ["Role_A", "Role_B", "Application_X"],
  "status": "COMPLETED",
  "sailpointRequestId": "SP-REQ-789",
  "errorMessage": null,
  "retryCount": 0,
  "createdAt": "2025-10-22T10:30:00",
  "updatedAt": "2025-10-22T10:35:00",
  "processedAt": "2025-10-22T10:35:00"
}
```

#### 3. Get Access Requests by Status

Retrieves all access requests, optionally filtered by status.

**GET** `/access-requests?status={status}`

**Query Parameters:**
- `status` (optional): PENDING, PROCESSING, COMPLETED, FAILED, RETRY_SCHEDULED

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "status": "PENDING",
    ...
  },
  {
    "id": 2,
    "status": "PENDING",
    ...
  }
]
```

#### 4. Process Access Request

Manually triggers processing of a specific access request. This submits the request to Sailpoint.

**POST** `/access-requests/{id}/process`

**Response:** `202 Accepted`

#### 5. Retry Failed Requests (Bulk)

Schedules all failed and non-completed requests for retry (max 3 retries per request).

**POST** `/access-requests/retry-failed`

**Response:** `200 OK`
```json
{
  "scheduledCount": 5,
  "message": "Scheduled 5 requests for retry"
}
```

#### 6. Process Batch

Processes a batch of pending/retry-scheduled requests using pessimistic locking (`FOR UPDATE SKIP LOCKED`).

**POST** `/access-requests/process-batch?batchSize={size}`

**Query Parameters:**
- `batchSize` (optional, default: 10): Number of requests to process

**Response:** `200 OK`
```json
{
  "processedCount": 10,
  "message": "Processed 10 requests"
}
```

### Error Responses

All errors follow RFC 7807 Problem Details format:

**404 Not Found:**
```json
{
  "type": "https://api.sailpoint.com/problems/not-found",
  "title": "Access Request Not Found",
  "status": 404,
  "detail": "Access request not found with id: 999",
  "timestamp": "2025-10-22T10:30:00Z"
}
```

**400 Bad Request:**
```json
{
  "type": "https://api.sailpoint.com/problems/validation",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed: requesterId: Requester ID is required",
  "timestamp": "2025-10-22T10:30:00Z"
}
```

**502 Bad Gateway:**
```json
{
  "type": "https://api.sailpoint.com/problems/sailpoint-client",
  "title": "Sailpoint Client Error",
  "status": 502,
  "detail": "Failed to submit access request: Connection refused",
  "timestamp": "2025-10-22T10:30:00Z"
}
```

## Database Schema

### access_requests Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| requester_id | VARCHAR(255) | ID of the user requesting access |
| requester_name | VARCHAR(255) | Name of the requester |
| target_user_id | VARCHAR(255) | ID of the user receiving access |
| target_user_name | VARCHAR(255) | Name of the target user |
| access_items | TEXT | JSON array of access items |
| status | VARCHAR(50) | Request status (enum) |
| sailpoint_request_id | VARCHAR(255) | ID returned by Sailpoint |
| error_message | TEXT | Error message if failed |
| retry_count | INTEGER | Number of retry attempts |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |
| processed_at | TIMESTAMP | Processing completion timestamp |

### Indexes

- `idx_access_requests_status` - Status filtering
- `idx_access_requests_created_at` - Sorting by creation date
- `idx_access_requests_status_created_at` - Composite for batch processing
- `idx_access_requests_sailpoint_id` - Sailpoint ID lookups
- `idx_access_requests_requester_id` - Requester queries
- `idx_access_requests_target_user_id` - Target user queries

## Background Processing

### JobRunr Integration

JobRunr is configured to:
- Run background jobs for processing access requests
- Retry failed jobs automatically (up to 3 times)
- Provide a dashboard at `http://localhost:8001`
- Store job data in PostgreSQL

### Job Types

1. **Process Access Request** - Processes a single access request
2. **Process Batch** - Processes multiple requests in a batch
3. **Retry Failed Requests** - Retries all failed requests

### Pessimistic Locking

The application uses PostgreSQL's `FOR UPDATE SKIP LOCKED` to ensure concurrent processing:

```sql
SELECT * FROM access_requests 
WHERE status IN ('PENDING', 'RETRY_SCHEDULED') 
ORDER BY created_at ASC 
LIMIT 10 
FOR UPDATE SKIP LOCKED
```

This prevents multiple workers from processing the same request simultaneously.

## Request Status Flow

```
PENDING → PROCESSING → COMPLETED
                     ↓
                   FAILED → RETRY_SCHEDULED → PROCESSING
```

- **PENDING**: Initial state after creation
- **PROCESSING**: Currently being processed
- **COMPLETED**: Successfully processed
- **FAILED**: Processing failed
- **RETRY_SCHEDULED**: Scheduled for retry after failure

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/sailpoint/accessrequest/
│   │   ├── domain/
│   │   │   ├── model/           # Domain models
│   │   │   ├── port/            # Port interfaces
│   │   │   ├── service/         # Domain services
│   │   │   └── exception/       # Domain exceptions
│   │   ├── infrastructure/
│   │   │   ├── rest/            # REST controllers & DTOs
│   │   │   ├── persistence/     # JPA entities & repositories
│   │   │   ├── client/          # Feign clients
│   │   │   ├── scheduler/       # JobRunr jobs
│   │   │   └── config/          # Spring configurations
│   │   └── AccessRequestApplication.java
│   └── resources/
│       ├── application.yml
│       └── db/changelog/        # Liquibase migrations
└── test/
```

### Running Tests

```bash
mvn test
```

### Running Tests

The project includes comprehensive unit tests for the domain service layer.

```bash
mvn test
```

**Test Coverage:**
- ✅ AccessRequestService validation tests
- ✅ Create access request tests
- ✅ Process access request tests
- ✅ Get access request tests
- ✅ Retry failed requests tests
- ✅ Exception handling tests

## Monitoring

### JobRunr Dashboard

Access the JobRunr dashboard at `http://localhost:8001` to:
- Monitor job execution
- View failed jobs
- Retry failed jobs manually
- See job statistics

### Logging

The application logs important events:
- Request creation
- Processing start/completion
- Errors and retries
- Batch processing

Log levels can be configured in `application.yml`.

## License

This is a Proof of Concept (PoC) application for demonstration purposes.

## Contact

For questions or issues, please contact the development team.
