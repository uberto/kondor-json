import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `common-kotlin`
    id("java-library")
    id("java-test-fixtures")
    id("maven-publish")
    id("signing")
    alias(libs.plugins.animalSniffer)
}

// kondor-core and kondor-outcome run on Android 13 (API 33) and later: `check` fails if the main code calls a JDK API
// Android 13 does not have. The signature is made by scripts/generate-android-signature.sh, with the same Animal Sniffer
// version. It does not check the bootstrap of the lambdas (LambdaMetafactory, which Android lacks): D8 rewrites them.
animalsniffer {
    toolVersion = "1.28"
    sourceSets = listOf(project.sourceSets.main.get())
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    api(project(":kondor-outcome"))
    implementation(libs.kotlin.jdk8)

    testFixturesImplementation(libs.kotlin.jdk8)
    testFixturesImplementation(libs.justify)

    signature(files(rootProject.file("gradle/android-api-33.signature")))
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        getByName<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.jUnit.get())
            dependencies {
                implementation(libs.striKt)
                implementation(project(":kondor-tools"))
                implementation(libs.justify)
                runtimeOnly(libs.jUnit.launcher)
                runtimeOnly(libs.joy)
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
                name = "kondor-core"
                description = "A Kotlin library to use Json in functional way without reflection"
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
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
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

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
}
