plugins {
    java
}

description = "ByteGuard Core - Encryption and decryption engine"

dependencies {
    // Caffeine for LRU cache (optional, can be made optional)
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    // Argon2 for password hashing
    implementation("de.mkammerer:argon2-jvm:2.11")
    
    // SLF4J API (for logging interface)
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // Test dependencies
    testImplementation("ch.qos.logback:logback-classic:1.4.14")
    testImplementation("org.bouncycastle:bcprov-jdk18on:1.77") // For test comparisons
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Automatic-Module-Name" to "io.github.ygqygq2.byteguard.core"
        )
    }
}
