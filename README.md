# Quizmania

## Adding new questions and questionsets

### Questions

Questions are defined in JSON files in the resource folder ./backend/src/main/resources/questions and are deployed directly with the application.

Thee are currently three supported types of questions:

**Choice-Questions:**

```
[
  {
    "id": "question01",
    "type": "CHOICE",
    "phrase": "Which US president was NOT assassinated in office?",
    "correctAnswer": "Theodore Roosevelt",
    "answerOptions": ["Abraham Lincoln", "John F. Kennedy", "Theodore Roosevelt", "William McKinley"]
  }
]
```

**Estimation-Questions:**

Although the correct answer is of type String, it must be an Int/Long
```
[
  {
    "id": "question01",
    "type": "ESTIMATE",
    "phrase": "How many years did the Hundred Years' War last?",
    "correctAnswer": "116"
  }
]
```

**Free-Questions:**

Although the correct answer is of type String, it must be an Int/Long
```
[
  {
    "id": "question01",
    "type": "FREE_INPUT",
    "phrase": "Which was the launch title of the Nintendo Game Boy?",
    "correctAnswer": "Tetris"
  }
]
```

Each question type also supports the property "imagePath" to include an image as question.

### Question sets

Questions are grouped into question sets and each game uses one question set. In a question set the questions are listed by their ID.

Question sets are defined in JSON files in the resource folder ./backend/src/main/resources/questionsets and are deployed directly with the application.

```
{
  "id": "my_questionset",
  "name": "My questions",
  "minPlayers": 4,
  "questions": [
    "question01",
    "question02",
    "question03"
  ]
}
```


## Developer Hints

### Axon 5 baseline

The backend uses Java 21, Spring Boot 3.5, and the Axoniq Framework 5.3.3 BOM/starter (including Axon Framework 5.3.3 and the Axon Server connector). Docker configurations pin Axon Server to `2026.1.4-jdk-21`. The connector is an Axoniq-licensed component, available for evaluation without credentials; consult the [Axoniq licensing guidance](https://docs.axoniq.io/axon-framework-reference/5.3/advanced-migration/paths/5.0-to-5.1/) before use beyond evaluation.

This is an architecture-preserving migration: `GameAggregate` remains the game-wide consistency boundary, using Axon 5's event-sourced entity and explicit `EventAppender` APIs. The aggregate-compatible Axon Server storage engine preserves per-game event sequence numbers for REST history and websocket delivery. Dynamic consistency boundaries are deliberately deferred. Switching to DCB storage later will also require revisiting the client event cursor contract.

Event processors explicitly preserve ordering within each game. Axon 5 subscriptions use concrete event names, so the websocket listener lists every game event; a regression test checks this list against the sealed event hierarchy. Jackson 2 remains the event/message converter alongside Spring Boot 3.

The migration is verified with fresh test databases and event stores, not as an in-place upgrade of Axon 4 data. Existing development event stores and processor tokens are not automatically converted or deleted. Use a separate fresh development stack, or explicitly reset disposable development data before switching versions. The backend integration tests now start PostgreSQL and Axon Server through Testcontainers; Docker is required for `./gradlew :backend:test`.

### Question timeout timers

The game module's subscribing `QuestionTimerEventListener` reacts to live `QuestionAskedEvent` events for collective questions with a positive answer timeout. It schedules an in-memory command after the originating transaction commits; the aggregate and round do not depend on the timer. The due time is the event's question timestamp plus its answer duration. Replay is disabled for this listener. Expiry commands for closed questions, older questions, or games that have ended are ignored, so timers do not need cancellation when everyone answers early.

Buzzer questions emit `BuzzerCollectionStartedEvent` on the first eligible buzz in a collection window. The same listener schedules evaluation at the server-defined deadline, 500 ms later. Further buzzes do not restart the window. Evaluation commands carry a window ID, so duplicate or obsolete callbacks are ignored. An incorrect answer immediately promotes a queued player; if nobody is queued, a later buzz starts a new window.

Pending question and buzzer timers are lost when the backend restarts. They are not recovered from the event store; a moderator can close a remaining open question manually. Axon deadlines and the Quartz scheduler are no longer used. The Quartz library remains only to supply the historical Liquibase schema resource.

### Running the end-to-end tests

The E2E suite builds the frontend and backend, starts an isolated PostgreSQL and Axon Server stack, runs the browser tests, and cleans everything up afterwards:

```shell
./scripts/e2e-test
```

Docker, Java 21, Node.js 22 or newer, and `curl` must be available. Test reports, traces, screenshots, video, and the backend log are written below `e2e/output` and `e2e/playwright-report`.

### How to start on local machine

To start the full stack with backend and frontend on the local machine, execute the following steps:

- in directory Frontend:
  - `npm install`
  - `npm run build` (installs the compiled frontend into the ./static folder of the backend)
- in root directory 
  -  `./gradlew clean build -x test backend:jibDockerBuild` (compiles code and builds a local docker image)
- in devsupport directory
  - `docker-compose -f ./docker-compose-app.yml up`
- visit http://localhost:8080 in your browser
