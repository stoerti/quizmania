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
