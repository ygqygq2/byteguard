plugins {
    java
    `maven-publish`
    signing
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

    group = rootProject.group
    version = rootProject.version

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        // Common test dependencies
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
