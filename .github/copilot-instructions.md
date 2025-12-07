# Copilot Instructions for Quizmania

## Project Overview

Quizmania is a quiz game application with a backend built on Kotlin/Spring Boot and a frontend built with React/TypeScript. The application uses WebSockets for real-time game interactions and supports multiple types of quiz questions (Choice, Estimation, and Free Input).

## Technology Stack

### Backend
- **Language**: Kotlin 2.0.0
- **Framework**: Spring Boot 3.3.0
- **Build Tool**: Gradle with Kotlin DSL
- **Java Version**: 17
- **Key Technologies**:
  - Spring Data JPA
  - Spring WebSocket
  - Axon Framework 4.10.3 (Event Sourcing/CQRS)
  - PostgreSQL
  - Liquibase (Database migrations)
  - JUnit 5 (Testing)
  - Testcontainers (Integration testing)
  - JaCoCo (Code coverage)

### Frontend
- **Language**: TypeScript 5.5.3
- **Framework**: React 18.3.1
- **Build Tool**: Vite 5.3.4
- **Key Technologies**:
  - Material-UI (MUI) 5.16.4
  - React Router 6.26.2
  - STOMP/WebSocket client
  - Vitest 2.0.3 (Testing)
  - ESLint (Linting)

### E2E Testing
- **Framework**: CodeceptJS 3.5.6
- **Browser Automation**: Playwright 1.39.0

## Project Structure

```
quizmania/
├── backend/                        # Spring Boot backend
│   ├── src/main/kotlin/           # Main Kotlin source code
│   │   └── org/quizmania/         # Main package structure
│   ├── src/main/resources/        # Resources including questions and questionsets
│   │   ├── questions/             # Question JSON files
│   │   └── questionsets/          # Question set JSON files
│   └── src/test/kotlin/           # Kotlin tests
├── frontend/                      # React frontend
│   ├── src/                       # TypeScript/React source
│   │   ├── pages/                 # React pages/components
│   │   ├── services/              # API services
│   │   └── domain/                # Domain models
│   └── package.json               # NPM dependencies
├── e2e/                          # End-to-end tests
├── devsupport/                   # Development support files (Docker Compose)
├── doc/                          # Architecture and design documentation
└── .github/workflows/            # CI/CD workflows
```

## Coding Standards

### General
- **Indentation**: 2 spaces for all files (as per `.editorconfig`)
- **Line Endings**: LF (Unix-style)
- **Encoding**: UTF-8
- **Max Line Length**: 160 characters
- **Trailing Whitespace**: Remove (except in Markdown files)
- **Final Newline**: Always add

### Kotlin
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Prefer immutability (use `val` over `var`)
- Use data classes for DTOs
- Use sealed classes for state modeling
- Write unit tests for domain logic
- Use JGiven for integration tests

### TypeScript/React
- Use functional components with hooks
- Follow React best practices
- Use TypeScript strict mode
- Prefer arrow functions for components
- Use Material-UI components consistently
- Follow the ESLint configuration in `frontend/.eslintrc.json`

## Build and Test Commands

### Full Stack Build
```bash
# Build frontend and backend together
./gradlew clean build -x test

# Build frontend only
./gradlew frontend:appNpmBuild

# Build backend only
./gradlew backend:build -x test
```

### Testing
```bash
# Run all tests
./gradlew test

# Run backend tests with coverage
./gradlew test jacocoTestReport

# Run frontend tests
cd frontend && npm test

# Run E2E tests
cd e2e && npm run codeceptjs:headless
```

### Development
```bash
# Frontend development (from frontend directory)
npm install
npm run build

# Backend development (from root)
./gradlew clean build -x test backend:jibDockerBuild

# Start with Docker Compose (from devsupport directory)
docker-compose -f ./docker-compose-app.yml up

# Access the application at http://localhost:8080
```

### Linting
```bash
# Frontend linting
cd frontend && npm run lint
```

## CI/CD

The project uses GitHub Actions for CI/CD:
- **Workflow File**: `.github/workflows/build-gradle.yml`
- **Triggers**: On every push
- **Steps**:
  1. Build frontend
  2. Build backend
  3. Run tests
  4. Generate test reports
  5. Generate JaCoCo coverage report (minimum 80%)
  6. Build and push Docker image to GitHub Container Registry

## Questions and Question Sets

### Adding Questions
Questions are defined in JSON files in `backend/src/main/resources/questions/`:

**Choice Questions**:
```json
{
  "id": "question01",
  "type": "CHOICE",
  "phrase": "Question text here?",
  "correctAnswer": "Correct answer",
  "answerOptions": ["Option 1", "Option 2", "Option 3", "Option 4"]
}
```

**Estimation Questions**:
```json
{
  "id": "question02",
  "type": "ESTIMATE",
  "phrase": "Question text here?",
  "correctAnswer": "123"
}
```

**Free Input Questions**:
```json
{
  "id": "question03",
  "type": "FREE_INPUT",
  "phrase": "Question text here?",
  "correctAnswer": "Answer text"
}
```

All question types support an optional `imagePath` property.

### Adding Question Sets
Question sets are defined in JSON files in `backend/src/main/resources/questionsets/`:

```json
{
  "id": "my_questionset",
  "name": "My Questions",
  "minPlayers": 4,
  "questions": ["question01", "question02", "question03"]
}
```

## Architecture Notes

- The backend uses **Event Sourcing and CQRS** patterns via Axon Framework
- **WebSocket** communication for real-time game updates
- **PostgreSQL** for persistence
- Frontend state management uses React hooks and context
- The frontend is built and deployed into the backend's static resources

## Security Guidelines

- Never commit secrets or credentials to source code
- Use environment variables for sensitive configuration
- Keep dependencies up to date
- Follow Spring Security best practices
- Validate all user inputs
- Use parameterized queries for database access

## Testing Guidelines

- Write unit tests for all business logic
- Use Testcontainers for integration tests requiring database
- Aim for at least 80% code coverage
- Write E2E tests for critical user journeys
- Mock external dependencies in tests
- Follow the existing test structure and conventions

## Development Workflow

1. Create feature branches from `main`
2. Make small, focused commits with clear messages
3. Write tests for new functionality
4. Run linting and tests before committing
5. Create pull requests for review
6. Ensure CI passes before merging
7. Review and address code review feedback

## Validation Checklist

Before finalizing any changes:
- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] Linting passes (frontend)
- [ ] Test coverage meets requirements (80%+)
- [ ] Documentation is updated (if applicable)
- [ ] No secrets or sensitive data in code
- [ ] Changes follow the project's coding standards

## Additional Resources

- **README**: See `/README.md` for project overview and setup instructions
- **Architecture Docs**: See `/doc` directory for architecture diagrams
- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **React Docs**: https://react.dev/
- **Axon Framework Docs**: https://docs.axoniq.io/
