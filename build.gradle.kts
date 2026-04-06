plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
}

group = "com.eilaji"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("io.ktor:ktor-server-auth:2.3.7")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.7")
    implementation("io.ktor:ktor-server-websockets:2.3.7")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    
    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:0.45.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.45.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.45.0")
    
    // Database driver
    implementation("org.postgresql:postgresql:42.7.1")
    
    // HikariCP connection pool
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // Redis (Lettuce)
    implementation("io.lettuce:lettuce-core:6.3.1.RELEASE")
    
    // MinIO
    implementation("io.minio:minio:8.5.7")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.ktor:ktor-server-call-logging:2.3.7")
    
    // Config
    implementation("com.typesafe:config:1.4.3")
    
    // Kotlinx serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests:2.3.7")
}

application {
    mainClass.set("com.eilaji.backend.ApplicationKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.eilaji.backend.ApplicationKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
