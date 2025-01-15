plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.github.papahigh"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}