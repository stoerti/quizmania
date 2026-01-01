import com.github.gradle.node.npm.task.NpmTask


plugins {
  id("com.github.node-gradle.node") version "7.1.0"
}

group = "org.quizmania"
version = "0.0.1-SNAPSHOT"

tasks.register<NpmTask>("appNpmInstall") {
  description = "Installs all dependencies from package.json"
  workingDir = file("${project.projectDir}")
  args = listOf("install")
  // Skip if already installed by CI pipeline
  onlyIf {
    !file("${project.projectDir}/node_modules").exists() ||
    System.getenv("CI") != "true"
  }
}


tasks.register<NpmTask>("appNpmBuild") {
  dependsOn("appNpmInstall")
  description = "Builds project"
  workingDir = file("${project.projectDir}")
  args = listOf("run", "build")
}

node {
  download = true
  version = "20.16.0"
  npmVersion = "10.8.2"
  // Set the work directory for unpacking node
  workDir = file("${layout.buildDirectory.get()}/nodejs")
  // Set the work directory for NPM
  npmWorkDir = file("${layout.buildDirectory.get()}/npm")
}
