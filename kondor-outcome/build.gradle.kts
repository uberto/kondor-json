import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `common-kotlin`
    id("maven-publish")
    id("signing")
    id("java-library")
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(libs.kotlin.jdk8)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        getByName<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.jUnit.get())
            dependencies {
                implementation(libs.striKt)
            }
            targets {
                all {
                    testTask.configure {
                        maxHeapSize = "2g"
                        testLogging {
//                            showStandardStreams = true
                            events = setOf(
                                TestLogEvent.SKIPPED,
                                TestLogEvent.FAILED,
                                TestLogEvent.PASSED
                            )
                        }
                    }
                }
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components.getByName("java"))
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                name = "kondor-outcome"
                description = "A Kotlin specialized Either type for wrapping results in a functional way"
                url = "https://github.com/uberto/kondor-json"
                inceptionYear = "2021"
                scm {
                    url = "https://github.com/uberto/kondor-json"
                    connection = "https://github.com/uberto/kondor-json.git"
                    developerConnection = "git@github.com:uberto/kondor-json.git"
                }
                licenses {
                    license {
                        name = "The Apache Software License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "UbertoBarbini"
                        name = "Uberto Barbini"
                        email = "uberto.gama@gmail.com"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("nexusUsername").toString()
                password = findProperty("nexusPassword").toString()
            }
        }
    }
}

signing {
    sign(publishing.publications.getByName("mavenJava"))
}