plugins {
    kotlin("jvm") version "1.9.22"
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.9.10"
}

group = "com.chainpilots"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(19)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("bech32kt") {
                groupId = "com.chainpilots"
                artifactId = "bech32kt"
                version = "0.1.0"

                from(components["java"])
            }
        }
    }
}