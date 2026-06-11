plugins {
  val kotlinVersion = "2.4.0"
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.4.0"
  kotlin("plugin.spring") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

val springdocOpenapiVersion = "3.0.3"
val hmppsKotlinSpringBootStarterVersion = "2.5.0"
val kotlinLoggingVersion = "3.0.5"
val athenaVersion = "2.46.8"

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:$hmppsKotlinSpringBootStarterVersion")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocOpenapiVersion")
  implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

  implementation("org.jetbrains.exposed:exposed-core:1.3.0")
  implementation("org.jetbrains.exposed:exposed-json:1.3.0")
  implementation("org.jetbrains.exposed:exposed-dao:1.3.0")
  implementation("org.jetbrains.exposed:exposed-jdbc:1.3.0")
  implementation("org.jetbrains.exposed:exposed-java-time:1.3.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0-0.6.x-compat")

  implementation(platform("software.amazon.awssdk:bom:$athenaVersion"))
  implementation("software.amazon.awssdk:athena")
  implementation("software.amazon.awssdk:sso")
  implementation("software.amazon.awssdk:ssooidc")
  implementation("software.amazon.awssdk:sts")

  implementation(platform("software.amazon.awssdk:bom:$athenaVersion"))
  implementation("software.amazon.awssdk:athena")

  implementation("org.postgresql:postgresql:42.7.11")

  implementation("software.amazon.awssdk:s3:$athenaVersion")
  implementation("software.amazon.awssdk:athena:$athenaVersion")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

  testImplementation("org.springframework.boot:spring-boot-webservices-test")
  testImplementation("org.springframework.boot:spring-boot-webflux-test")
  testImplementation("org.springframework.boot:spring-boot-webmvc-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.5.0")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("com.ninja-squad:springmockk:5.0.1")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.43") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("com.h2database:h2:2.4.240")
}

kotlin {
  jvmToolchain(25)
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
