plugins {
    java
}

group = "io.github.ygqygq2.byteguard.test"
version = "1.0.0"

subprojects {
    apply(plugin = "java")
    
    repositories {
        mavenCentral()
    }
    
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
