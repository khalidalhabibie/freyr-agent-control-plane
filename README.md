# Freyr Agent Control Plane

Freyr Agent Control Plane is a Spring Boot backend for an AI-assisted agricultural
field operations control plane.

The system is designed around safe orchestration rather than direct autonomous
execution. An agent will generate operational proposals for field work, a human
manager will approve, reject, or override those proposals, and only approved
decisions will result in task assignments.

This initial version contains only the project structure, runtime configuration,
PostgreSQL wiring, Flyway setup, and Swagger/OpenAPI support. Business modules
are intentionally not implemented yet.

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
