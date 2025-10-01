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
### Build & Run

```bash
# Build
./gradlew clean build

# Run
./gradlew bootRun

# Or run JAR
java -jar build/libs/reai-timetracker-0.0.1-SNAPSHOT.jar
```