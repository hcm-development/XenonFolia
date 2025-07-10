import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.7.6"
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    implementation("ca.spottedleaf:concurrentutil:0.0.2")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.build {
    dependsOn(tasks.reobfJar)
}

tasks.reobfJar {
    outputJar.set(layout.buildDirectory.file("libs/${project.name}-${project.version}.jar"))
}
