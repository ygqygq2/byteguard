plugins {
    java
    application
}

description = "ByteGuard CLI - Command-line tool and JavaAgent"

application {
    mainClass.set("io.github.ygqygq2.byteguard.cli.Main")
}

dependencies {
    implementation(project(":byteguard-core"))
    
    // Picocli for CLI argument parsing
    implementation("info.picocli:picocli:4.7.5")
    annotationProcessor("info.picocli:picocli-codegen:4.7.5")
    
    // SLF4J simple implementation (for CLI)
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "io.github.ygqygq2.byteguard.cli.Main",
            "Premain-Class" to "io.github.ygqygq2.byteguard.agent.ByteGuardAgent",
            "Agent-Class" to "io.github.ygqygq2.byteguard.agent.ByteGuardAgent",
            "Can-Redefine-Classes" to "false",
            "Can-Retransform-Classes" to "true",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
    
    // Create fat JAR with all dependencies
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<JavaExec>("runEncrypt") {
    group = "application"
    description = "Run encryption example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.ygqygq2.byteguard.cli.Main")
    args("encrypt", "--help")
}
