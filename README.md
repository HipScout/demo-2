# GitLab Risk Scanner

A Spring Boot application that scans public GitLab repositories for common security risks and repository misconfigurations.

## Features

The scanner checks for:

* Sensitive files such as `.env`, `.pem`, `id_rsa`, `secrets.yml`
* Exposed secrets such as API keys, tokens, passwords and cloud credentials
* Missing `README.md`
* Missing `LICENSE`
* Risk severity: `HIGH`, `MEDIUM`, `LOW`

The application retrieves **real repository data from GitLab APIs**.

---

## Tech Stack

* Java 17
* Spring Boot
* Spring Web
* Maven
* REST APIs
* GitLab REST API
* JUnit 5 / Mockito

---

## Project Structure

```text
gitlab-risk-scanner
│
├── src/main/java/com/example/gitlabscanner
│   │
│   ├── controller
│   │   └── ScanController.java
│   │
│   ├── service
│   │   └── ScanService.java
│   │
│   ├── client
│   │   └── GitLabClient.java
│   │
│   ├── scanner
│   │   ├── RiskScanner.java
│   │   ├── SensitiveFileScanner.java
│   │   ├── SecretScanner.java
│   │   └── MetadataScanner.java
│   │
│   ├── model
│   │   ├── ScanRequest.java
│   │   ├── Project.java
│   │   ├── Risk.java
│   │   ├── Severity.java
│   │   └── ScanResult.java
│   │
│   ├── config
│   │   └── GitLabConfig.java
│   │
│   └── exception
│       └── GlobalExceptionHandler.java
│
├── src/main/resources
│   └── application.properties
│
├── src/test
│
├── pom.xml
└── README.md
```

---

# Requirements

Install:

* JDK 17+
* Maven 3.8+
* Git
* IntelliJ IDEA / Eclipse / VS Code

Check Java:

```bash
java -version
```

Check Maven:

```bash
mvn -version
```

---

# Configuration

`src/main/resources/application.properties`

```properties
spring.application.name=gitlab-risk-scanner

server.port=8080

gitlab.base-url=https://gitlab.com/api/v4

gitlab.token=${GITLAB_TOKEN:}
```

A GitLab token is **not required for the initial public repository scanner**.

For private repository support:

```bash
export GITLAB_TOKEN=your_gitlab_token
```

Never commit the token to Git.

---

# Build

From the project root:

```bash
mvn clean package
```

Successful build:

```text
BUILD SUCCESS
```

---

# Run

## Using Maven

```bash
mvn spring-boot:run
```

## Using JAR

```bash
java -jar target/gitlab-risk-scanner-*.jar
```

Application runs on:

```text
http://localhost:8080
```

---

# API Flow

```text
User / Group
     ↓
GitLab API
     ↓
Repositories
     ↓
Repository Files
     ↓
Risk Scanners
     ↓
Risk Report
```

---

# APIs

## 1. Scan User

```http
POST /api/v1/scan
```

Request:

```json
{
  "type": "USER",
  "name": "username"
}
```

---

## 2. Scan Group

```http
POST /api/v1/scan
```

Request:

```json
{
  "type": "GROUP",
  "name": "group-name"
}
```

---

## 3. Get Projects

```http
GET /api/v1/projects?type=USER&name=username
```

Used to verify GitLab connectivity and retrieve repositories.

---

## 4. Get Repository Files

```http
GET /api/v1/projects/{projectId}/files
```

---

## 5. Get File Content

```http
GET /api/v1/projects/{projectId}/file?path=README.md&branch=main
```

---

# Example Scan Request

```bash
curl -X POST http://localhost:8080/api/v1/scan \
-H "Content-Type: application/json" \
-d '{
  "type": "USER",
  "name": "username"
}'
```

---

# Example Response

```json
{
  "target": "username",
  "targetType": "USER",
  "totalProjects": 3,
  "totalIssues": 4,
  "high": 2,
  "medium": 1,
  "low": 1,
  "projects": [
    {
      "name": "payment-service",
      "issues": [
        {
          "type": "SENSITIVE_FILE",
          "file": ".env",
          "severity": "HIGH"
        },
        {
          "type": "EXPOSED_SECRET",
          "file": "application.properties",
          "severity": "HIGH"
        }
      ]
    }
  ]
}
```

---

# Risk Checks

## Sensitive Files

Examples:

```text
.env
.env.production
*.pem
id_rsa
id_dsa
secrets.yml
secrets.yaml
config.json
```

Severity:

```text
HIGH
```

---

## Exposed Secrets

The scanner uses regex patterns to detect potential:

```text
API keys
Access tokens
Passwords
AWS credentials
Generic secrets
```

Example:

```text
AWS Access Key
API Key
Password
Token
```

Secrets must never be returned completely in the response or written to logs.

---

## Missing Metadata

The scanner checks for:

```text
README.md
LICENSE
```

Missing metadata:

```text
LOW
```

---

# Scanner Design

All scanners implement:

```java
public interface RiskScanner {

    List<Risk> scan(
        Project project,
        List<RepositoryFile> files
    );
}
```

Current scanners:

```text
RiskScanner
    │
    ├── SensitiveFileScanner
    ├── SecretScanner
    └── MetadataScanner
```

New scanners can be added without changing the existing scanners.

---

# GitLab API

Base URL:

```text
https://gitlab.com/api/v4
```

Main operations:

```text
Find User
    ↓
Get User Projects

Find Group
    ↓
Get Group Projects

Get Project
    ↓
Get Repository Tree
    ↓
Get File Content
```

The application should support GitLab API pagination.

---

# Error Handling

The application should handle:

* Invalid username/group
* User/group not found
* Project not found
* GitLab API errors
* Network errors
* Rate limiting
* Invalid requests

Errors should return a clean JSON response instead of a stack trace.

---

# Testing

Run all tests:

```bash
mvn test
```

Tests should cover:

```text
SensitiveFileScanner
SecretScanner
MetadataScanner
ScanService
GitLabClient
```

---

# Debugging

Run the application using **Debug** in IntelliJ.

Useful breakpoints:

```text
ScanController
ScanService
GitLabClient
SensitiveFileScanner
SecretScanner
MetadataScanner
```

Useful actions:

```text
F7  → Step Into
F8  → Step Over
F9  → Resume
```

---

# Development Plan

Build the project in this order:

```text
1. Spring Boot setup
       ↓
2. GitLab connectivity
       ↓
3. User / Group lookup
       ↓
4. Get repositories
       ↓
5. Get repository files
       ↓
6. Get file contents
       ↓
7. Sensitive file scanner
       ↓
8. Secret scanner
       ↓
9. Metadata scanner
       ↓
10. Scan API
       ↓
11. Error handling
       ↓
12. Unit tests
```

---

# Optional Enhancements

Future improvements:

* HTML dashboard
* JSON/PDF export
* Private repository scanning
* GitLab token authentication
* Swagger/OpenAPI
* Docker
* GitLab CI/CD
* Database for scan history
* Parallel repository scanning
* Additional security scanners

---

# Security

Important rules:

* Never commit GitLab tokens.
* Never log actual secrets.
* Never return complete secrets in API responses.
* Limit repository/file size during scanning.
* Handle GitLab API rate limits.
* Validate all user input.

---

# Status

Current development focus:

```text
[ ] Spring Boot setup
[ ] GitLab connectivity
[ ] User project retrieval
[ ] Group project retrieval
[ ] Repository file retrieval
[ ] Sensitive file scanner
[ ] Secret scanner
[ ] Metadata scanner
[ ] Scan API
[ ] Tests
[ ] Docker
[ ] Swagger
```

## Goal

Build a simple, reliable Spring Boot service that takes a **GitLab username or group**, scans its public repositories using **real GitLab API data**, and returns an actionable security risk report.
