plugins {
  val kotlinVersion = "2.3.10"
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.3.7"
  kotlin("plugin.spring") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.8.2")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.15")

  implementation("org.jetbrains.exposed:exposed-core:0.61.0")
  implementation("org.jetbrains.exposed:exposed-json:0.61.0")
  implementation("org.jetbrains.exposed:exposed-dao:0.61.0")
  implementation("org.jetbrains.exposed:exposed-jdbc:1.1.1")
  implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")

  implementation(platform("software.amazon.awssdk:bom:2.41.29"))
  implementation("software.amazon.awssdk:athena")
  implementation("software.amazon.awssdk:sso")
  implementation("software.amazon.awssdk:ssooidc")
  implementation("software.amazon.awssdk:sts")

  implementation(platform("software.amazon.awssdk:bom:2.41.29"))
  implementation("software.amazon.awssdk:athena")

  implementation("org.postgresql:postgresql:42.7.10")

  implementation("software.amazon.awssdk:s3:2.41.29")
  implementation("software.amazon.awssdk:athena:2.41.29")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.8.2")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("com.ninja-squad:springmockk:5.0.1")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.37") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("com.h2database:h2:2.4.240")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}
