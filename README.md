# Freyr Agent Control Plane

Freyr Agent Control Plane is a Spring Boot backend for an AI-assisted agricultural
field operations control plane.

The system is designed around safe orchestration rather than direct autonomous
execution. An agent generates operational proposals for field work, a human
manager approves, rejects, or overrides those proposals, and only approved
decisions result in task assignments.

## Tech Stack

- Java 21
- Spring Boot 3
- Maven
- PostgreSQL
- Flyway
- Spring Data JPA
- Spring Validation
- Springdoc OpenAPI / Swagger
- JUnit 5
- Mockito
- Docker Compose

## Package Structure

Base package: `com.khalid.freyr`

- `common.response`
- `common.exception`
- `common.config`
- `common.logging`
- `farmer`
- `farmfield`
- `agronomist`
- `fieldtask`
- `agent`
- `approval`
- `assignment`

## Local Run

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
mvn spring-boot:run
```

By default, the application uses:

```text
DB_URL=jdbc:postgresql://localhost:5432/freyr_agent_control_plane
DB_USERNAME=freyr
DB_PASSWORD=freyr
SERVER_PORT=8080
```

You can override these values with environment variables.

## Swagger

After the application starts, open:

```text
http://localhost:8080/swagger-ui.html
```

The OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

## Sample API Payloads

Create farmer: `POST /api/v1/farmers`

```json
{
  "name": "Pak Budi",
  "phoneNumber": "08123456789",
  "village": "Lamteh",
  "district": "Aceh Besar"
}
```

Create farm field: `POST /api/v1/fields`

```json
{
  "farmerId": "11111111-1111-1111-1111-111111111111",
  "areaName": "North Block",
  "areaSize": 2.5,
  "cropStage": "VEGETATIVE",
  "waterStatus": "WET",
  "pestReported": false
}
```

Create agronomist: `POST /api/v1/agronomists`

```json
{
  "name": "Sari",
  "phoneNumber": "08129876543",
  "assignedDistrict": "Aceh Besar",
  "maxDailyVisit": 4,
  "availabilityStatus": "AVAILABLE"
}
```

Create field task: `POST /api/v1/field-tasks`

```json
{
  "farmFieldId": "22222222-2222-2222-2222-222222222222",
  "taskType": "WATER_LEVEL_CHECK",
  "priority": "HIGH",
  "status": "CREATED",
  "dueDate": "2026-05-20"
}
```

Run scheduling agent: planned `POST /api/v1/agent/scheduling-runs`

```json
{
  "district": "Aceh Besar",
  "scheduleDate": "2026-05-20"
}
```

Approve proposal: `POST /api/v1/agent-proposals/{proposalId}/approve`

```json
{
  "reviewedBy": "manager-001",
  "note": "Approved for tomorrow visits"
}
```

Reject proposal: `POST /api/v1/agent-proposals/{proposalId}/reject`

```json
{
  "reviewedBy": "manager-001",
  "reason": "Schedule is no longer valid"
}
```

Override proposal: `POST /api/v1/agent-proposals/{proposalId}/override`

```json
{
  "reviewedBy": "manager-001",
  "reason": "Original agronomist is unavailable",
  "overrides": [
    {
      "proposalItemId": "33333333-3333-3333-3333-333333333333",
      "newAgronomistId": "44444444-4444-4444-4444-444444444444"
    }
  ]
}
```

## Domain Boundaries

Farmers, farm fields, agronomists, field tasks, proposals, and assignments are
separate aggregate boundaries. Entities store UUID references instead of direct
JPA object graphs so each aggregate can evolve independently, service methods
can control consistency explicitly, and future asynchronous workflows can pass
stable identifiers through queues or external agent systems.

## Production Notes

- Transaction handling: approval operations run in transactions so proposal
  state, proposal items, task assignments, field task updates, and approval
  history are committed together.
- Approval concurrency: production deployments should add optimistic locking or
  row-level locking around proposals so two managers cannot approve or override
  the same pending proposal concurrently.
- Stale proposal validation: before approval, compare proposal items against the
  current task status, agronomist availability, capacity, and schedule window so
  old agent recommendations cannot assign invalid work.
- Future integrations: RabbitMQ can carry agent jobs/events, Redis can cache
  short-lived scheduling context or locks, and OpenAI integration can generate
  proposal recommendations while keeping human approval as the execution gate.
