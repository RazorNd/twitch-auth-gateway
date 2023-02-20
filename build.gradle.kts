/*
 * Copyright 2023 Daniil Razorenov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
    id("org.graalvm.buildtools.native") version "0.9.18"
    kotlin("jvm") version "1.8.0"
    kotlin("kapt") version "1.8.0"
}

group = "ru.razornd.twitch"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2022.0.1"
extra["testcontainersVersion"] = "1.17.6"
extra["springMockk"] = "4.0.0"
extra["logbackContrib"] = "0.1.5"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("io.micrometer:micrometer-tracing-bridge-brave")
    runtimeOnly("io.zipkin.reporter2:zipkin-reporter-brave")

    runtimeOnly("ch.qos.logback.contrib:logback-json-classic:${property("logbackContrib")}")
    runtimeOnly("ch.qos.logback.contrib:logback-jackson:${property("logbackContrib")}")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.ninja-squad:springmockk:${property("springMockk")}")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.springframework.boot.gradle.tasks.aot.ProcessAot> {
    jvmArgs("-Dspring.main.cloud-platform=kubernetes")
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootBuildImage> {

    val registryUrl: String? by project
    val registryUsername: String? by project
    val registryPassword: String? by project

    val domain = registryUrl?.let { "$it/" } ?: ""
    val userPath = registryUsername?.toLowerCase()?.let { "$it/" } ?: ""

    imageName.set("$domain$userPath${project.name}:${project.version}")

    docker {
        publishRegistry {
            url.set(registryUrl)
            username.set(registryUsername)
            password.set(registryPassword)
        }
    }
}
