import io.papermc.paperweight.util.*
import java.time.Instant

plugins {
    java
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.7.6"
}

val log4jPlugins = sourceSets.create("log4jPlugins")
configurations.named(log4jPlugins.compileClasspathConfigurationName) {
    extendsFrom(configurations.compileClasspath.get())
}
val alsoShade: Configuration by configurations.creating

// Paper start - configure mockito agent that is needed in newer java versions
val mockitoAgent = configurations.register("mockitoAgent")
abstract class MockitoAgentProvider : CommandLineArgumentProvider {
    @get:CompileClasspath
    abstract val fileCollection: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> {
        return listOf("-javaagent:" + fileCollection.files.single().absolutePath)
    }
}
// Paper end - configure mockito agent that is needed in newer java versions

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    // implementation(project(":folia-api")) // Folia - comment out until subprojects are set up
    implementation("ca.spottedleaf:concurrentutil:0.0.2") // Paper - Add ConcurrentUtil dependency
    // Paper start
    implementation("org.jline:jline-terminal-ffm:3.27.1") // use ffm on java 22+
    implementation("org.jline:jline-terminal-jni:3.27.1") // fall back to jni on java 21
    implementation("net.minecrell:terminalconsoleappender:1.3.0")
    implementation("net.kyori:adventure-text-serializer-ansi:4.17.0") // Keep in sync with adventureVersion from Paper-API build file
    /*
          Required to add the missing Log4j2Plugins.dat file from log4j-core
          which has been removed by Mojang. Without it, log4j has to classload
          all its classes to check if they are plugins.
          Scanning takes about 1-2 seconds so adding this speeds up the server start.
     */
    implementation("org.apache.logging.log4j:log4j-core:2.19.0") // Paper - implementation
    log4jPlugins.annotationProcessorConfigurationName("org.apache.logging.log4j:log4j-core:2.19.0") // Paper - Needed to generate meta for our Log4j plugins
    runtimeOnly(log4jPlugins.output)
    alsoShade(log4jPlugins.output)
    implementation("io.netty:netty-codec-haproxy:4.1.97.Final") // Paper - Add support for proxy protocol
    // Paper end
    implementation("org.apache.logging.log4j:log4j-iostreams:2.24.1") // Paper - remove exclusion
    implementation("org.ow2.asm:asm-commons:9.7.1")
    implementation("org.spongepowered:configurate-yaml:4.2.0-SNAPSHOT") // Paper - config files
    implementation("commons-lang:commons-lang:2.6")
    implementation("dev.dejvokep:boosted-yaml:1.3.7")
    // Paper start - spark
    implementation("me.lucko:spark-api:0.1-20240720.200737-2")
    implementation("me.lucko:spark-paper:1.10.119-SNAPSHOT")
    // Paper end - spark
}

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

tasks.jar {
    archiveClassifier.set("dev")
    from(alsoShade.elements.map {
        it.map { f ->
            if(f.asFile.isFile) {
                zipTree(f.asFile)
            } else {
                f.asFile
            }
        }
    })

    manifest {
        val git = Git(rootProject.layout.projectDirectory.path)
        val mcVersion = rootProject.providers.gradleProperty("mcVersion").get()
        val build = System.getenv("BUILD_NUMBER") ?: null
        val gitHash = git("rev-parse", "--short=7", "HEAD").getText().trim()
        val implementationVersion = "$mcVersion-${build ?: "DEV"}-$gitHash"
        val date = git("show", "-s", "--format=%ci", gitHash).getText().trim() // Paper
        val gitBranch = git("rev-parse", "--abbrev-ref", "HEAD").getText().trim() // Paper
        attributes(
            "Main-Class" to "org.bukkit.craftbukkit.Main",
            "Implementation-Title" to "Folia", // Folia
            "Implementation-Version" to implementationVersion,
            "Implementation-Vendor" to date, // Paper
            "Specification-Title" to "Bukkit",
            "Specification-Version" to project.version,
            "Specification-Vendor" to "Bukkit Team",
            "Multi-Release" to "true", // Paper
            "Git-Branch" to gitBranch, // Paper
            "Git-Commit" to gitHash, // Paper
            "CraftBukkit-Package-Version" to "v1_21_R3",
            "Add-Opens" to "java.base/java.lang java.base/java.io java.base/java.util java.base/java.util.jar java.base/java.util.zip java.base/java.util.concurrent java.base/sun.nio.ch java.base/java.net java.base/java.nio java.base/java.util.concurrent.atomic java.logging/java.util.logging java.base/java.lang.reflect java.base/javax.crypto java.base/java.nio.charset java.base/java.util.concurrent.locks java.base/sun.nio.fs java.base/java.lang.ref java.base/java.net java.base/java.nio.file java.base/java.security java.base/sun.nio.cs java.base/java.text java.sql/java.sql java.base/java.util.stream java.base/java.util.regex java.desktop/java.awt.font"
        )
        for (tld in setOf("net", "com", "org", "io")) {
            attributes("$tld/bukkit", "Name" to "Bukkit", "Specification-Version" to project.version, "Specification-Vendor" to "Bukkit Team", "Implementation-Title" to "CraftBukkit", "Implementation-Version" to implementationVersion, "Implementation-Vendor" to date)
        }
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

val scanJar = tasks.register("scanJarForBadCalls", io.papermc.paperweight.tasks.ScanJarForBadCalls::class) {
    jarToScan.set(tasks.jar.flatMap { it.archiveFile })
    classpath.from(configurations.compileClasspath)
    dependsOn(tasks.jar)
}

// Remove the serverJar tasks and mappings tasks that don't exist in userdev
// They are replaced by standard jar task and reobfJar task

tasks.test {
    exclude("**/**")
}

// Paper start - remove mappings tasks not available in userdev
// Paper end - include reobf mappings in jar for stacktrace deobfuscation

// Paper start - remove TCA tasks not available in userdev
// Paper end - use TCA for console improvements

// Paper start
tasks.register<JavaExec>("runServer") {
    group = "paper"
    description = "Spin up a test server from the reobfJar"
    classpath(tasks.reobfJar.flatMap { it.outputJar })
    args("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    args("--add-opens", "java.base/java.util=ALL-UNNAMED")
    args("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")
    args("--add-opens", "java.base/java.text=ALL-UNNAMED")
    args("--add-opens", "java.desktop/java.awt.font=ALL-UNNAMED")
    args("--add-opens", "java.base/java.util.jar=ALL-UNNAMED")
    args("--add-opens", "java.base/java.util.zip=ALL-UNNAMED")
    args("--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED")
    args("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
    args("--add-opens", "java.base/java.net=ALL-UNNAMED")
    args("--add-opens", "java.base/java.nio=ALL-UNNAMED")
    args("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
    args("--add-opens", "java.logging/java.util.logging=ALL-UNNAMED")
    args("--add-opens", "java.base/javax.crypto=ALL-UNNAMED")
    args("--add-opens", "java.base/java.nio.charset=ALL-UNNAMED")
    args("--add-opens", "java.base/java.util.concurrent.locks=ALL-UNNAMED")
    args("--add-opens", "java.base/sun.nio.fs=ALL-UNNAMED")
    args("--add-opens", "java.base/java.lang.ref=ALL-UNNAMED")
    mainClass.set("org.bukkit.craftbukkit.Main")
    standardInput = System.`in`
    workingDir = rootProject.layout.projectDirectory.dir("run").asFile
    workingDir.mkdirs()
    javaLauncher.set(project.javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.JETBRAINS)
    })

    dependsOn(tasks.reobfJar)
}

tasks.register<JavaExec>("runMojangMappedServer") {
    group = "paper"
    description = "Spin up a test server from the Mojang mapped jar"
    classpath(tasks.jar.flatMap { it.archiveFile })
    mainClass.set("org.bukkit.craftbukkit.Main")
    standardInput = System.`in`
    workingDir = rootProject.layout.projectDirectory.dir("run").asFile
    workingDir.mkdirs()
    javaLauncher.set(project.javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.JETBRAINS)
    })

    dependsOn(tasks.jar)
}

// Paper end - removed bundler and paperclip tasks not available in userdev
