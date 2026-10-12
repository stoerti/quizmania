plugins {
  id("org.springframework.boot") version "3.5.13"
  id("io.spring.dependency-management") version "1.1.7"
  id("jacoco")
  id("com.google.cloud.tools.jib") version "3.5.3"
  kotlin("jvm") version "2.3.20"
  kotlin("plugin.spring") version "2.3.20"
  kotlin("plugin.jpa") version "2.3.20"
  id("io.holixon.emn") version "0.0.1-SNAPSHOT"
}

group = "org.quizmania"
version = "0.0.1-SNAPSHOT"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

kotlin {
  jvmToolchain(21)
  compilerOptions {
    freeCompilerArgs.add("-Xjsr305=strict")
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
  }
}

repositories {
  mavenLocal()
  mavenCentral()
}

emn {
  emnFiles.from(file("../doc/quizmania-blueprint.emn"))
  rootPackageName.set("org.quizmania.api")

  kotlin {
    enabled.set(true)
    annotationEnhancers.set(listOf("axon5", "jackson"))
  }

  specifications {
    enabled.set(true)
    fixtureProvider.set("org.quizmania.test.QuizmaniaFixtureProvider")
    errorVerifier.set("org.quizmania.test.QuizmaniaErrorVerifier")
  }

  verification {
    enabled.set(true)
    failOnWarnings.set(false)
    ignoredIssues {
      ignoreMissingReadModel("AskFirstQuestionAutomationQuery")
      ignoreMissingReadModel("CloseQuestionAutomationQuery")
      ignoreMissingReadModel("GameState")
      ignoreMissingReadModel("OpenQuestion")
      ignoreMissingReadModel("QuestionScoreboardQuery")
      ignoreMissingReadModel("StartRoundAutomationQuery")
      ignoreMissingReadModel("UnscoredQuestionsQuery")
    }
  }
}

dependencies {
  implementation(platform("org.testcontainers:testcontainers-bom:2.0.4")) //import bom

  implementation("io.holixon.emn:emn-verify-annotations:0.0.1-SNAPSHOT")

  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-websocket")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.session:spring-session-core")

  implementation(platform("io.axoniq.framework:axoniq-framework-bom:5.4.0-RC1"))
  implementation("io.axoniq.framework:axoniq-spring-boot-starter")
  implementation("io.axoniq.framework:axon-server-connector:5.4.0-RC1")
  implementation("io.axoniq.framework:axoniq-workflow-engine:5.4.0-RC1")
  implementation("io.axoniq.framework:axoniq-workflow-dsl-kotlin:5.4.0-RC1")
  // Retain the historical Liquibase changelog resource without starting a Quartz scheduler.
  runtimeOnly("org.quartz-scheduler:quartz")

  implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.16")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  // The 5.4 workflow preview uses annotations introduced in Jackson 2.22.
  implementation("com.fasterxml.jackson.core:jackson-annotations:2.22")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.liquibase:liquibase-core")
  developmentOnly("org.springframework.boot:spring-boot-devtools")

  runtimeOnly("org.postgresql:postgresql")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
  testImplementation("org.assertj:assertj-core:3.27.7")
  testImplementation("org.awaitility:awaitility:4.3.0")

  testImplementation("com.tngtech.jgiven:jgiven-spring-junit5:2.0.3")
  testImplementation("io.toolisticon.testing:jgiven-kotlin:1.3.1.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.testcontainers:testcontainers")
  testImplementation("org.testcontainers:junit-jupiter")
  testImplementation("org.testcontainers:postgresql")
  testImplementation("org.axonframework:axon-test")
  testImplementation("io.axoniq.framework:axoniq-testcontainer")
  testImplementation("io.axoniq.framework:axoniq-workflow-test:5.4.0-RC1")
  testRuntimeOnly("org.postgresql:postgresql")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val packageFrontend by tasks.registering(Sync::class) {
  group = "distribution"
  description = "Builds the frontend and packages it into the backend's static resources."
  dependsOn(":frontend:appNpmBuild")
  from(rootProject.layout.projectDirectory.dir("frontend/dist"))
  into(layout.projectDirectory.dir("src/main/resources/public"))
}

tasks.named("processResources") {
  mustRunAfter(packageFrontend)
}

tasks.register("assembleWithFrontend") {
  group = "distribution"
  description = "Builds the executable backend JAR with the packaged frontend."
  dependsOn(packageFrontend, "bootJar")
}

tasks.matching { it.name in setOf("jib", "jibDockerBuild", "jibBuildTar") }.configureEach {
  dependsOn(packageFrontend, "processResources")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.withType<JacocoReport> {
  reports {
    xml.required.set(true)
    csv.required.set(false)
    html.required.set(true)
  }
}

jacoco {
  toolVersion = "0.8.15"
}

jib {
  from {
    image = "eclipse-temurin:21-jdk-ubi9-minimal"
    platforms {
      platform {
        architecture = "arm64"
        os = "linux"
      }
      platform {
        architecture = "amd64"
        os = "linux"
      }
    }
  }
  to {
    image = "ghcr.io/stoerti/quizmania:${project.version}"
  }
}
