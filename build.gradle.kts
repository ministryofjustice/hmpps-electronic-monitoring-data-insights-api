plugins {
  val kotlinVersion = "2.4.21"
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.15"
  id("io.gatling.gradle") version "3.16.0"
  kotlin("plugin.spring") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion
  kotlin("plugin.jpa") version kotlinVersion
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

val springdocOpenapiVersion = "3.2.0"
val hmppsKotlinSpringBootStarterVersion = "3.1.0"
val kotlinLoggingVersion = "3.0.5"
val commonsTextVersion = "1.15.0"
val athenaVersion = "2.57.13"
val shedlockVersion = "7.11.0"

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:$hmppsKotlinSpringBootStarterVersion")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocOpenapiVersion")
  constraints {
    implementation("org.webjars:swagger-ui:5.34.1") {
      because("5.34.0 bundles DOMPurify 3.4.16 (CVE-2026-116802/116803/116804). Remove once springdoc ships swagger-ui >= 5.34.1.")
    }
  }
  implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
  implementation("org.apache.commons:commons-text:$commonsTextVersion")
  implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlockVersion")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$shedlockVersion")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0-0.6.x-compat")

  implementation(platform("software.amazon.awssdk:bom:$athenaVersion"))
  implementation("software.amazon.awssdk:athena")
  implementation("software.amazon.awssdk:sso")
  implementation("software.amazon.awssdk:ssooidc")
  implementation("software.amazon.awssdk:sts")

  implementation(platform("software.amazon.awssdk:bom:$athenaVersion"))
  implementation("software.amazon.awssdk:athena")

  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  implementation("software.amazon.awssdk:s3:$athenaVersion")
  implementation("software.amazon.awssdk:athena:$athenaVersion")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

  testImplementation("org.springframework.boot:spring-boot-webservices-test")
  testImplementation("org.springframework.boot:spring-boot-webflux-test")
  testImplementation("org.springframework.boot:spring-boot-webmvc-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:3.1.0")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("com.ninja-squad:springmockk:5.0.1")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.48") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("com.h2database:h2:2.5.252")
  testImplementation("org.testcontainers:testcontainers-postgresql")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
}

kotlin {
  jvmToolchain(25)
}

dependencyCheck {
  suppressionFiles.add("$rootDir/dependencyCheck/suppression.xml")
  // Gatling is load-test tooling run from developer machines/CI against the dev environment.
  // It is not on productionRuntimeClasspath and is not in the deployed image, so it is excluded from the scan.
  skipConfigurations.addAll("gatling", "gatlingCompileClasspath", "gatlingRuntimeClasspath")
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }

  test {
    environment("AWS_ACCESS_KEY_ID", "test")
    environment("AWS_SECRET_ACCESS_KEY", "test")
    environment("AWS_SESSION_TOKEN", "test")
    environment("AWS_REGION", "eu-west-2")
  }
}
