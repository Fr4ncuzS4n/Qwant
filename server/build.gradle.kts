plugins {
    kotlin("jvm") version "2.3.21"
    application
}

group = "com.example.qwant"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:3.6.0")
    implementation("io.ktor:ktor-server-netty:3.6.0")
    implementation("io.ktor:ktor-serialization-gson:3.6.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.6.0")
    implementation("io.ktor:ktor-server-cors:3.6.0")
    implementation("io.ktor:ktor-server-status-pages:3.6.0")
    implementation("io.ktor:ktor-server-call-logging:3.6.0")

    // sqlite3 — база данных сервера (см. AGENTS.md)
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")

    implementation("ch.qos.logback:logback-classic:1.5.18")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:3.6.0")
    testImplementation("io.ktor:ktor-serialization-gson:3.6.0")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.example.qwant.server.MainKt")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.test {
    useJUnitPlatform()
}
