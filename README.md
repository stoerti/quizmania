# Quizmania

Quizmania is a learning application for experimenting with event sourcing, Dynamic Consistency Boundaries (DCB), and Axoniq workflows. It contains a Kotlin/Spring Boot backend, a React frontend, and browser-based end-to-end tests.

## Questions and question sets

Questions and question sets are JSON resources packaged with the backend:

- Questions: `backend/src/main/resources/questions`
- Question sets: `backend/src/main/resources/questionsets`

### Questions

Every question has an `id`, `type`, `phrase`, and `correctAnswer`. The optional `imagePath` and `answerImagePath` properties add an image to the question or answer.

Five question types are supported:

- `CHOICE`: one answer from `answerOptions`
- `MULTIPLE_CHOICE`: multiple answers from `answerOptions`
- `FREE_INPUT`: a textual answer
- `ESTIMATE`: a numeric answer stored as a string
- `SORT`: puts the values from `answerOptions` into the order expressed by `correctAnswer`

Example:

```json
[
  {
    "id": "question01",
    "type": "CHOICE",
    "phrase": "Which US president was NOT assassinated in office?",
    "correctAnswer": "Theodore Roosevelt",
    "answerOptions": [
      "Abraham Lincoln",
      "John F. Kennedy",
      "Theodore Roosevelt",
      "William McKinley"
    ]
  }
]
```

See the existing resource files for examples of every question type.

### Question sets and rounds

Each game uses one question set. A question set contains one or more rounds; each round references questions by ID and defines whether it is collective or uses a buzzer.

```json
{
  "id": "my_questionset",
  "name": "My questions",
  "rounds": [
    {
      "name": "Regular questions",
      "roundConfig": {
        "useBuzzer": false,
        "secondsToAnswer": 30
      },
      "questions": [
        "question01",
        "question02"
      ]
    },
    {
      "name": "Buzzer final",
      "roundConfig": {
        "useBuzzer": true,
        "secondsToAnswer": 10
      },
      "questions": [
        "question03"
      ]
    }
  ]
}
```

A buzzer game requires a moderator. Empty question sets and rounds without questions are rejected when the game is created.

## Technology baseline

The backend currently uses:

- Java 21 and Kotlin 2.3
- Spring Boot 3.5
- Axoniq Framework `5.4.0-RC1`
- Axon Server `2026.1.4-jdk-21`
- PostgreSQL

This project uses the Axoniq workflow engine, which is a commercial Axoniq Framework module. Axon Server also requires a license for production use. Local development and evaluation can run temporarily without arranging a license; see the [official Axoniq licensing documentation](https://docs.axoniq.io/axoniq-platform-reference/licensing/) for the current limits and production options.

## DCB command model

The command model is split into three immutable event-sourced states:

- `GameState`: configuration, lifecycle, moderator, and active players
- `ProgressionState`: current round, question progression, and round scoring
- `GameQuestionState`: one question's lifecycle, answers, buzzer activity, and winner

Each state is reconstructed through `evolve(event)` functions. Command handlers resolve external input, inject the states needed by a command, and delegate the business decision to `decide(...)` functions. A decision returns the events to append.

The states select explicit event types and a `gameId` or `gameQuestionId` tag. Combining states in one processing context gives Axon Server the histories required to enforce the decision's consistency boundary.

Important behavior:

- `QuestionAskedEvent` captures the players eligible for that question. A late joiner waits for the next question.
- A departed player no longer has to answer an open collective question. Answers already submitted remain available for scoring.
- Commands address a specific question and validate that it belongs to the game and is still in the required phase. The model assumes only one open question per game.
- Moderated collective free-input questions are closed before the moderator reviews and scores them. Other questions are scored when they close.
- Events returned by one decision are appended atomically. Collective answers and the workflow-triggered completion are separate decisions; a correct buzzer answer returns its answer, close, and score events as one batch.
- For a buzzer question, an incorrect answer promotes the next eligible queued player. If nobody is queued, the buzzer reopens.
- Commands are still routed by `gameId`; DCB controls consistency through selected event histories, not through parallel routing.

### Event history cursors

REST history and websocket envelopes expose `cursor` as a decimal string representing a global event-store position. Resume with:

```text
GET /api/game/{id}/events?afterCursor={cursor}
```

Positions can have gaps within one game and are not aggregate sequence numbers. The browser compares them losslessly, buffers live events during REST catch-up, and resumes from the last cursor after reconnecting. Event processors explicitly sequence game events by `gameId`.

### Fresh DCB storage

The Docker configurations initialize Axon Server with:

```text
AXONIQ_AXONSERVER_STANDALONE_DCB=true
```

This initializes fresh DCB storage; it does not convert an existing aggregate-format context. Historical payload/tag conversion and processor-token migration are outside the scope of this project. Use a fresh local stack when switching from the earlier aggregate model.

## Workflows and timing

Timing and question completion are coordinated with the Axoniq Framework 5.4 workflow preview.

### Collective questions

`QuestionLifecycleWorkflow` starts for every collective `QuestionAskedEvent`. It waits until one of the following happens:

- all eligible players answer or leave;
- the question closes through another path;
- the game is canceled; or
- the answer deadline expires.

The workflow then sends an idempotent completion or expiry command. `GameQuestionState` rechecks the current state before deciding whether the question should close, so obsolete workflow commands are ignored.

### Buzzer questions

The first eligible buzz emits `BuzzerCollectionStartedEvent` and opens a 500 ms collection window. `BuzzerCollectionWorkflow` waits until the event's `evaluateAt` timestamp and sends `EvaluateBuzzesCommand`. Later buzzes do not restart the window, and the window ID makes duplicate or obsolete evaluations harmless.

When the moderator marks an answer incorrect, the next eligible queued player wins immediately. If nobody is queued, the buzzer reopens and a later buzz starts a new window.

Both workflow event processors use a JPA token store. Workflow support is currently based on `5.4.0-RC1`; recovery and upgrade behavior should therefore be verified again before treating it as production-ready. Axon deadlines and Quartz scheduling are not used. Quartz remains a runtime dependency only for the historical Liquibase schema resource.

## Abandoned-game cleanup

A scheduled projection-backed job cancels active games after their creation age exceeds the configured maximum. By default it runs hourly and abandons games after 24 hours:

```yaml
quizmania:
  abandoned-game-cleanup:
    cron: "0 0 * * * *"
    maximum-game-age: 24h
```

## Running locally

Requirements:

- Docker with Docker Compose
- Java 21

Start PostgreSQL and a fresh DCB-enabled Axon Server:

```shell
docker compose -f devsupport/docker-compose.yml up --detach --wait
```

Run the application:

```shell
./gradlew :backend:bootRun
```

The Gradle build downloads its configured Node.js runtime, installs the frontend dependencies, builds the frontend into `backend/src/main/resources/public`, and starts the backend. Open [http://localhost:8080](http://localhost:8080).

Stop the infrastructure with:

```shell
docker compose -f devsupport/docker-compose.yml down
```

## Tests

### Backend tests

Run the unit, command-handler, workflow, and Spring integration tests with:

```shell
./gradlew :backend:test
```

The Spring integration tests start PostgreSQL and a DCB-enabled Axon Server through Testcontainers, so Docker must be available.

Generate the JaCoCo report with:

```shell
./gradlew :backend:test :backend:jacocoTestReport
```

The HTML report is written to `backend/build/reports/jacoco/test/html`.

### End-to-end tests

The E2E script installs its dependencies, builds the frontend and executable backend, starts isolated PostgreSQL and Axon Server containers, runs the Playwright suite, and cleans up afterward:

```shell
./scripts/e2e-test
```

Docker, Java 21, Node.js 22 or newer, and `curl` must be available. Reports, traces, screenshots, video, and the backend log are written below `e2e/output` and `e2e/playwright-report`.

CI runs the Gradle build and the Playwright E2E suite as separate jobs for pull requests. A push to `main` also publishes the application image.
