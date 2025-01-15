plugins {
    kotlin("jvm") version "2.1.0"
}

group = "com.github.papahigh"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.knowm.xchange:xchange-core:5.2.1")
    implementation("org.knowm.xchange:xchange-bitfinex:5.2.1")
    implementation("org.knowm.xchange:xchange-stream-bitfinex:5.2.1")
    implementation("ch.qos.logback:logback-classic:1.5.16")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}