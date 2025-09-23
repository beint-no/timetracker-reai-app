# ReAI Time Tracker

A simple time tracking application that demonstrates integration with ReAI platform APIs.

## Features

- Employee selection from ReAI API
- Start/stop time tracking for projects
- View time entries and durations
- Real-time duration updates
- Integration with ReAI employee and project APIs

## Quick Start

1. Clone the repository
2. Configure ReAI API credentials in `application.yml`
3. Run: `./gradlew bootRun`
4. Open: http://localhost:8080

## API Integration

This application demonstrates how to:
- Authenticate with ReAI platform
- Fetch employee data from ReAI
- Sync time entries back to ReAI timesheet API

## Technology Stack

- Spring Boot 3.x
- Spring Data JPA
- H2 Database (development)
- Thymeleaf (optional)
- Vanilla JavaScript

=== AGENTS.md ===
# Time Tracker Agents

This document describes the AI agents and automated tools used in this project.

## Development Agents

### Code Quality Agent
- Automated code review and formatting
- Dependency vulnerability scanning
- Test coverage reporting

### Integration Testing Agent
- API endpoint testing
- ReAI platform integration testing
- Performance monitoring

## Deployment Agents

### CI/CD Pipeline
- Automated testing on pull requests
- Docker image building
- Deployment to staging/production