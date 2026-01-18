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
    
    // 零外部依赖 - 纯手工参数解析
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
    
    // 排除签名文件以避免签名冲突（Bouncy Castle 等库会有签名）
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/LICENSE*")
    exclude("META-INF/NOTICE*")
}
