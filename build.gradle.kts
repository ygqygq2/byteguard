plugins {
    java
    `maven-publish`
    signing
    id("jacoco")
    id("org.sonarqube") version "4.4.1.3373" apply false
    id("com.github.spotbugs") version "6.0.4" apply false
}

group = "io.github.ygqygq2"
version = "1.0.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "jacoco")

    group = rootProject.group
    version = rootProject.version

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.test {
        useJUnitPlatform()
        finalizedBy(tasks.jacocoTestReport)
    }

    // Register integration test task
    val integrationTest by tasks.registering(Test::class) {
        description = "Runs integration tests"
        group = "verification"
        
        useJUnitPlatform {
            includeTags("integration")
        }
        
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        
        shouldRunAfter(tasks.test)
    }
    
    tasks.check {
        dependsOn(integrationTest)
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.jacocoTestCoverageVerification {
        violationRules {
            rule {
                limit {
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
    }

    tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
        reports.create("html") {
            required.set(true)
        }
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testImplementation("org.assertj:assertj-core:3.24.2")
        testImplementation("org.mockito:mockito-core:5.8.0")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                pom {
                    name.set(project.name)
                    description.set("Modern Java bytecode encryption with 100% Lambda support")
                    url.set("https://github.com/ygqygq2/byteguard")
                    
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set("ygqygq2")
                            name.set("ygqygq2")
                            email.set("ygqygq2@qq.com")
                        }
                    }
                    
                    scm {
                        connection.set("scm:git:git://github.com/ygqygq2/byteguard.git")
                        developerConnection.set("scm:git:ssh://github.com/ygqygq2/byteguard.git")
                        url.set("https://github.com/ygqygq2/byteguard")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "OSSRH"
                val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                
                credentials {
                    username = project.findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                    password = project.findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }

    signing {
        sign(publishing.publications["maven"])
    }
}
