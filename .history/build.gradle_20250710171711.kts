plugins {
    java
    `maven-publish`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("ca.spottedleaf:concurrentutil:0.0.2")
    
    // Add basic server dependencies
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("org.apache.logging.log4j:log4j-iostreams:2.24.1")
    implementation("net.minecrell:terminalconsoleappender:1.3.0")
    implementation("org.jline:jline-terminal-ffm:3.27.1")
    implementation("org.jline:jline-terminal-jni:3.27.1")
    implementation("net.kyori:adventure-text-serializer-ansi:4.17.0")
    implementation("org.ow2.asm:asm-commons:9.7.1")
    implementation("org.spongepowered:configurate-yaml:4.2.0-SNAPSHOT")
    implementation("commons-lang:commons-lang:2.6")
    implementation("io.netty:netty-codec-haproxy:4.1.97.Final")
    implementation("me.lucko:spark-api:0.1-20240720.200737-2")
    implementation("me.lucko:spark-paper:1.10.119-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 21
}

tasks.jar {
    archiveClassifier.set("dev")
    
    manifest {
        attributes(
            "Main-Class" to "org.bukkit.craftbukkit.Main",
            "Implementation-Title" to "XenonFolia",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "XenonFolia Team",
            "Specification-Title" to "Bukkit",
            "Specification-Version" to project.version,
            "Specification-Vendor" to "Bukkit Team"
        )
    }
}

tasks.register<Jar>("createPaperclipJar") {
    group = "build"
    description = "Create a Paperclip-style jar"
    archiveClassifier.set("paperclip")
    
    from(tasks.jar.get().outputs.files.map { zipTree(it) })
    
    // Include all runtime dependencies
    from(configurations.runtimeClasspath.get().map { 
        if (it.isDirectory) it else zipTree(it) 
    })
    
    // Exclude duplicate files
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes(
            "Main-Class" to "org.bukkit.craftbukkit.Main",
            "Implementation-Title" to "XenonFolia",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "XenonFolia Team"
        )
    }
}

tasks.register<Jar>("createMojmapPaperclipJar") {
    group = "build"
    description = "Create a Mojmap Paperclip jar (same as regular for this setup)"
    archiveClassifier.set("mojmap-paperclip")
    
    from(tasks.jar.get().outputs.files.map { zipTree(it) })
    
    // Include all runtime dependencies
    from(configurations.runtimeClasspath.get().map { 
        if (it.isDirectory) it else zipTree(it) 
    })
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes(
            "Main-Class" to "org.bukkit.craftbukkit.Main",
            "Implementation-Title" to "XenonFolia",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "XenonFolia Team"
        )
    }
}

tasks.build {
    dependsOn(tasks.jar)
}
