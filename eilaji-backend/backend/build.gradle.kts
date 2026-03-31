plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("io.ktor.plugin") version "3.0.1"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.0.21"
    application
}

group = "com.eilaji"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Core
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    
    // Ktor Features
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-websockets-jvm")
    implementation("io.ktor:ktor-server-cors-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-rate-limit-jvm")
    
    // Ktor Client (for FCM, external APIs)
    implementation("io.ktor:ktor-client-core-jvm")
    implementation("io.ktor:ktor-client-cio-jvm")
    implementation("io.ktor:ktor-client-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    
    // Database - Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.55.0")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:6.0.0")
    
    // Redis
    implementation("redis.clients:jedis:5.1.5")
    
    // MinIO/S3
    implementation("io.minio:minio:8.5.14")
    
    // Security
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    // Configuration
    implementation("com.typesafe:config:1.4.3")
    implementation("io.ktor:ktor-server-config-yaml-jvm")
    
    // Validation
    implementation("io.ktor:ktor-server-request-validation-jvm")
    
    // OpenAPI Documentation
    implementation("io.ktor:ktor-swagger-ui-jvm:3.0.1")
    
    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("io.ktor:ktor-client-mock-jvm")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.eilaji.backend.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}

// Fat JAR for deployment
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.eilaji.backend.ApplicationKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Docker support
tasks.register<Copy>("copyDockerfile") {
    from("Dockerfile")
    into(layout.buildDirectory.dir("docker"))
}

tasks.register<Copy>("copyJarToDocker") {
    dependsOn(tasks.jar)
    from(tasks.jar)
    into(layout.buildDirectory.dir("docker"))
}
