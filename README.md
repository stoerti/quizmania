# Quizmania

## Developer Hints

### How to start on local machine

To start the full stack with backend and frontend on the local machine, execute the following steps:

- in directory Frontend:
  - `npm -i`
  - `npm run build` (installs the compiled frontend into the ./static folder of the backend)
- in root directory 
  -  `./gradlew clean build -x test backend:jibDockerBuild` (compiles code and builds a local docker image)
- in devsupport directory
  - `docker-compose -f ./docker-compose-app.yml up`
- visit http://localhost:8080 in your browser
