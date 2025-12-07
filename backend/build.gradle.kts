import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.springframework.boot") version "4.0.0"
  id("io.spring.dependency-management") version "1.1.5"
  id("jacoco")
  id("com.google.cloud.tools.jib") version "3.4.3"
  kotlin("jvm") version "2.2.21"
  kotlin("plugin.spring") version "2.2.21"
  kotlin("plugin.jpa") version "2.2.21"
}

group = "org.quizmania"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
  compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("org.testcontainers:testcontainers-bom:1.20.0")) //import bom

  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-websocket")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.session:spring-session-core")

  implementation("org.axonframework:axon-spring-boot-starter:4.12.2")
  implementation("org.springframework.boot:spring-boot-starter-quartz")

  implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.liquibase:liquibase-core")
  implementation("commons-io:commons-io:2.16.1")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  runtimeOnly("org.postgresql:postgresql")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
  testImplementation("org.assertj:assertj-core:3.26.0")
  testImplementation("org.awaitility:awaitility:4.2.1")

  testImplementation("com.tngtech.jgiven:jgiven-spring-junit5:1.3.1")
  testImplementation("io.toolisticon.testing:jgiven-kotlin:1.3.1.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("org.testcontainers:testcontainers")
  testImplementation("org.testcontainers:junit-jupiter")
  testImplementation("org.testcontainers:postgresql")
  testImplementation("org.axonframework:axon-test:4.12.2")
  testRuntimeOnly("org.postgresql:postgresql")
}

tasks.withType<KotlinCompile> {
  dependsOn(":frontend:appNpmBuild")
  compilerOptions {
    freeCompilerArgs.add("-Xjsr305=strict")
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
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
  toolVersion = "0.8.7"
}

jib {
  from {
    image = "openjdk:17-jdk-slim"
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
