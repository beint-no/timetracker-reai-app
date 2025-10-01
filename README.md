# ReAI Time Tracker

Modern time tracking application built with HTMX, Thymeleaf, and Spring Boot. Features a Clockify-inspired interface with seamless real-time updates.

## Features

- ⏱️ **Real-time Timer**: Live countdown display with start/stop functionality
- 🔄 **HTMX Integration**: Dynamic content without page reloads
- 📊 **Multiple Views**: Today, This Week, and All Entries
- 🔗 **ReAI Core Integration**: Syncs with external API
- 🌍 **Internationalization**: English and Vietnamese support
- 🎨 **Modern UI**: Clockify-inspired professional design
- 🔌 **REST API**: Backward-compatible JSON endpoints

## Tech Stack

### Backend
- **Kotlin** - Primary language
- **Spring Boot 3.5.6** - Application framework
- **Spring Data JPA** - Database access
- **PostgreSQL** - Database
- **Flyway** - Database migrations
- **Thymeleaf** - Server-side templates

### Frontend
- **HTMX 1.9.12** - Dynamic HTML updates
- **Web Awesome 3.0** - UI components
- **Vanilla JavaScript** - Minimal client-side logic

## Getting Started

### Prerequisites

- Java 24
- PostgreSQL 12+
- Gradle 9.1+

### Database Setup

```sql
CREATE DATABASE timetracker;
CREATE USER timetracker_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE timetracker TO timetracker_user;
```

### Configuration

Create or update `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/timetracker
spring.datasource.username=timetracker_user
spring.datasource.password=your_password

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# ReAI API
reai.api.base-url=https://your-reai-api.com
api.secret=your-api-secret

# Server
server.port=8080

# Internationalization
spring.messages.basename=messages
spring.messages.encoding=UTF-8
```

### Build & Run

```bash
# Build
./gradlew clean build

# Run
./gradlew bootRun

# Or run JAR
java -jar build/libs/reai-timetracker-0.0.1-SNAPSHOT.jar
```

### Access Application

```
http://localhost:8080/?access_token=YOUR_JWT_TOKEN
```

The token is stored in `localStorage` and automatically included in all requests.

## Usage

### 1. Select Employee
Choose an employee from the dropdown in the header.

### 2. Start Timer
- Select a project
- Click **Start** button
- Timer begins counting

### 3. Stop Timer
- Click **Stop** button
- Entry auto-syncs to ReAI Core

### 4. View Entries
- **Today**: Current day entries with summary
- **All Entries**: Complete history (limited to 50 recent)

### 5. Manual Sync
Click **Sync** button to sync all today's entries to ReAI Core.

## API Endpoints

### HTMX Endpoints (HTML fragments)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/htmx/employees` | Employee dropdown options |
| GET | `/htmx/projects?search=...` | Project list with search |
| GET | `/htmx/timer/current?employeeId=...` | Current timer state |
| POST | `/htmx/timer/start` | Start new timer |
| POST | `/htmx/timer/stop` | Stop active timer |
| GET | `/htmx/entries/today?employeeId=...` | Today's entries |
| GET | `/htmx/entries/all?employeeId=...` | All entries |
| POST | `/htmx/sync?employeeId=...` | Sync to ReAI |

### REST API Endpoints (JSON)

All existing REST endpoints at `/api/time/*` and `/api/employees/*` are maintained for backward compatibility.

## Internationalization

Application uses `messages.properties` for all text content.

### Adding New Language

If needed in the future:
1. Create `src/main/resources/messages_XX.properties`
2. Copy keys from `messages.properties`
3. Translate values
4. Restart application
5. Access with `?lang=XX` parameter

## Project Structure

```
src/main/
├── kotlin/reai/timetracker/
│   ├── controller/
│   │   ├── HomeController.kt              # Main page route
│   │   ├── TimeTrackerViewController.kt   # HTMX endpoints
│   │   ├── TimeTrackerController.kt       # REST API
│   │   └── EmployeeController.kt          # Employee API
│   ├── service/
│   │   ├── TimeTrackerService.kt          # Business logic
│   │   └── ReaiApiService.kt              # External API
│   ├── entity/
│   │   ├── Employee.kt
│   │   └── TimeEntry.kt
│   ├── repository/
│   │   ├── EmployeeRepository.kt
│   │   └── TimeEntryRepository.kt
│   ├── config/
│   │   ├── WebConfig.kt                   # CORS config
│   │   └── RestClientConfiguration.kt
│   └── exception/
│       └── GlobalExceptionHandler.kt
└── resources/
    ├── templates/
    │   ├── index.html                     # Main page
    │   ├── error.html                     # Error page
    │   └── fragments/                     # HTMX fragments
    ├── db/migration/                      # Flyway migrations
    ├── messages.properties                # i18n messages
    └── application.properties             # Configuration
```

## Development

### Hot Reload

Spring Boot DevTools is included for automatic restart during development:

```bash
./gradlew bootRun
```

### Database Migrations

Create new migration in `src/main/resources/db/migration/`:

```sql
-- V3__description.sql
ALTER TABLE time_entries ADD COLUMN notes TEXT;
```

Flyway runs migrations automatically on startup.

### Adding HTMX Endpoint

1. Add method in `TimeTrackerViewController`:
```kotlin
@GetMapping("/custom-view")
fun customView(model: Model): String {
    model.addAttribute("data", fetchData())
    return "fragments/custom-view"
}
```

2. Create Thymeleaf template in `templates/fragments/`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <div th:text="${data}">Content</div>
</body>
</html>
```

3. Add trigger in main template:
```html
<button hx-get="/htmx/custom-view" hx-target="#target">Load</button>
```

## Documentation

- [HTMX Implementation Guide](HTMX_IMPLEMENTATION.md) - Technical details
- [Changes Summary](CHANGES.md) - What changed in refactoring

## License

Proprietary - ReAI Technology
