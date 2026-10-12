import com.github.gradle.node.npm.task.NpmTask

plugins {
  base
  id("com.github.node-gradle.node") version "7.1.0"
  id("io.holixon.emn") version "0.0.1-SNAPSHOT"
}

group = "org.quizmania"
version = "0.0.1-SNAPSHOT"

tasks.register<NpmTask>("appNpmInstall") {
  description = "Installs all dependencies from package.json"
  workingDir = file("${project.projectDir}")
  args = listOf("install")
  inputs.files(file("package.json"), file("package-lock.json"))
  outputs.file(file("node_modules/.package-lock.json"))
}

tasks.register<NpmTask>("appNpmBuild") {
  dependsOn("appNpmInstall", "generateEmnTypes")
  description = "Builds project"
  workingDir = file("${project.projectDir}")
  args = listOf("run", "build")
  inputs.dir(file("src"))
  inputs.files(
    file("index.html"),
    file("package.json"),
    file("package-lock.json"),
    file("tsconfig.json"),
    file("tsconfig.app.json"),
    file("tsconfig.node.json"),
    file("vite.config.ts"),
  )
  inputs.file(layout.buildDirectory.file("generated/sources/emn/quizmania-api.ts"))
  outputs.dir(file("dist"))
}

tasks.register<NpmTask>("appNpmStart") {
  dependsOn("appNpmInstall", "generateEmnTypes")
  description = "Generates EMN types and starts the frontend development server"
  workingDir = projectDir
  args = listOf("run", "start")
}

tasks.register<NpmTask>("generateEmnTypes") {
  dependsOn("appNpmInstall", "exportEmnSchemas")
  description = "Generates TypeScript types from the exported EMN JSON Schemas"
  workingDir = projectDir
  args = listOf("run", "generate:emn-types")
  inputs.file(layout.buildDirectory.file("generated/resources/emn-schemas/manifest.json"))
  inputs.dir(layout.buildDirectory.dir("generated/resources/emn-schemas/schemas"))
  inputs.file(file("scripts/generate-emn-types.mjs"))
  inputs.files(file("package.json"), file("package-lock.json"))
  outputs.file(layout.buildDirectory.file("generated/sources/emn/quizmania-api.ts"))
}

emn {
  emnFiles.from(file("../doc/quizmania-blueprint.emn"))

  schemaExport {
    enabled.set(true)
    sourceRoot.set(file("../").canonicalPath)
  }
}

node {
  download = true
  version = "22.12.0"
  npmVersion = "10.9.0"
  // Set the work directory for unpacking node
  workDir = file("${layout.buildDirectory.get()}/nodejs")
  // Set the work directory for NPM
  npmWorkDir = file("${layout.buildDirectory.get()}/npm")
}
