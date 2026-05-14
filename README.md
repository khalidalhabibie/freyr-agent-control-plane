# Freyr Agent Control Plane

A backend system for AI-assisted agricultural field operations.

## Problem

Field operation managers need to coordinate farmers, fields, agronomists, and
urgent field tasks. Freyr demonstrates how agent-generated recommendations can
be controlled, reviewed, audited, and safely executed.

This is not a chatbot project. The agent does not chat with users or directly
change operations without oversight. The core concept is:

```text
Agent proposes -> Human approves -> System executes safely
```

## Architecture

```text
                 +----------------------+
                 |  REST API / Swagger  |
                 +----------+-----------+
                            |
                            v
+-----------+     +---------+----------+     +------------------+
| Farmers   |     | Field Tasks        |     | Agronomists      |
| Fields    +---->+ Scheduling Agent   +<----+ Availability     |
+-----------+     +---------+----------+     +------------------+
                            |
                            v
                 +----------+-----------+
                 | Agent Execution Log  |
                 | Agent Proposal       |
                 +----------+-----------+
                            |
                            v
                 +----------+-----------+
                 | Human Approval       |
                 | Approve/Reject/      |
                 | Override             |
                 +----------+-----------+
                            |
                            v
                 +----------+-----------+
                 | Task Assignment      |
                 | Audit History        |
                 +----------------------+
```

## Main Features

- Farmer management
- Farm field management
- Agronomist management
- Field task management
- Rule-based scheduling agent
- Agent execution log
- Agent proposal records
- Human approve, reject, and override workflow
- Transactional task assignment

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Validation
- PostgreSQL
- Flyway
- Springdoc OpenAPI / Swagger
- Maven
- Docker Compose
- JUnit 5
- Mockito

## Run Locally

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
mvn spring-boot:run
```

Default configuration:

```text
DB_URL=jdbc:postgresql://localhost:5432/freyr_agent_control_plane
DB_USERNAME=freyr
DB_PASSWORD=freyr
SERVER_PORT=8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Example API Flow

1. Create a farmer: `POST /api/v1/farmers`
2. Create a farm field: `POST /api/v1/fields`
3. Create an agronomist: `POST /api/v1/agronomists`
4. Create a field task: `POST /api/v1/field-tasks`
5. Run the scheduling agent: `POST /api/v1/agents/scheduling/run`
6. Review the generated proposal: `GET /api/v1/agent-proposals/{id}`
7. Approve the proposal: `POST /api/v1/agent-proposals/{id}/approve`

The scheduling agent creates an execution log, generates a proposal, stores
proposal items, and marks related tasks as proposed. Human approval converts
those recommendations into active task assignments inside a transaction.

## Agent Payload Storage

Agent execution `input_payload` and `output_payload` values are stored as
PostgreSQL `TEXT` columns for the initial implementation. These columns can be
upgraded to PostgreSQL `JSONB` later when payload contracts stabilize and
querying inside payloads becomes necessary.

## Production Considerations

- Decision logging for agent runs and human review outcomes
- Audit trail through approval history and execution records
- Idempotency for retry-safe agent execution and approval endpoints
- Async execution for long-running agents
- RabbitMQ or Redis for background job orchestration
- OpenAI adapter for replacing or augmenting rule-based recommendations
