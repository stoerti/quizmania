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

