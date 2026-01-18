plugins {
    java
    application
}

group = "io.github.ygqygq2.byteguard.test"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("LambdaTest")
}

tasks.register<Jar>("buildTestJar") {
    archiveFileName.set("lambda-app-test.jar")
    from(sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes["Main-Class"] = "LambdaTest"
    }
}
