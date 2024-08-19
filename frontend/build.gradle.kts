import com.moowork.gradle.node.npm.NpmTask


plugins {
  id("com.github.node-gradle.node") version "2.2.2"
  //id("com.github.node-gradle.node") version "7.0.2"
}

group = "org.quizmania"
version = "0.0.1-SNAPSHOT"

tasks.register<NpmTask>("appNpmInstall") {
  description = "Installs all dependencies from package.json"
  workingDir = file("${project.projectDir}/src/main/webapp")
  args = listOf("install")
}


tasks.register<NpmTask>("appNpmBuild") {
  dependsOn("appNpmInstall")
  description = "Builds project"
  workingDir = file("${project.projectDir}/src/main/webapp")
  args = listOf("run", "build")
}

node {
  download = true
  version = "20.16.0"
  npmVersion = "10.8.2"
  // Set the work directory for unpacking node
  workDir = file("${project.buildDir}/nodejs")
  // Set the work directory for NPM
  npmWorkDir = file("${project.buildDir}/npm")
}
