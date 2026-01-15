plugins {
    java
    `maven-publish`
}

description = "ByteGuard Maven Plugin - Maven integration for bytecode encryption"

dependencies {
    implementation(project(":byteguard-core"))
    
    // Maven plugin API
    compileOnly("org.apache.maven:maven-plugin-api:3.9.6")
    compileOnly("org.apache.maven:maven-core:3.9.6")
    compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:3.11.0")
    
    // For processing annotations
    annotationProcessor("org.apache.maven.plugin-tools:maven-plugin-annotations:3.11.0")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

// Generate Maven plugin descriptor
tasks.register("generatePluginDescriptor") {
    group = "build"
    description = "Generate Maven plugin descriptor"
    
    doLast {
        val outputDir = file("$buildDir/maven-plugin")
        outputDir.mkdirs()
        
        val descriptor = file("$outputDir/plugin.xml")
        descriptor.writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <plugin>
                <name>ByteGuard Maven Plugin</name>
                <description>Maven plugin for ByteGuard bytecode encryption</description>
                <groupId>${project.group}</groupId>
                <artifactId>${project.name}</artifactId>
                <version>${project.version}</version>
                <goalPrefix>byteguard</goalPrefix>
            </plugin>
        """.trimIndent())
    }
}

tasks.named("build") {
    dependsOn("generatePluginDescriptor")
}
